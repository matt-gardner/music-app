package com.gardner.soundengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoteAligner {
    private List<MusicNote> musicNotes;
    private List<TranscribedNote> transcribedNotes;
    private int lastTranscribedNote;
    private byte[][] alignment;

    private double secondsPerBeat;

    private byte MUSIC_NOTE_SKIPPED = 1;
    private byte TRANSCRIBED_NOTE_ADDED = 2;
    private byte NOTES_ALIGNED = 2;

    public NoteAligner(SheetMusic music) {
        musicNotes = music.getNotes();
        transcribedNotes = new ArrayList<TranscribedNote>();
        lastTranscribedNote = 0;
        secondsPerBeat = -1;
    }

    public void updateAlignment(List<TranscribedNote> notes) {
        // Right now we just redo the whole alignment every time update is called - not a big deal
        // for short files.  But this is written so that it can be easily made incremental.
        for (; lastTranscribedNote<notes.size(); lastTranscribedNote++) {
            transcribedNotes.add(notes.get(lastTranscribedNote));
        }
        computeAlignment();
        estimateTempo();
    }

    /**
     * Gets the currently computed alignment.  This won't return anything meaningful unless
     * updateAlignment is called first.
     */
    public NoteAlignment getAlignment() {
        if (alignment == null) {
            computeAlignment();
            estimateTempo();
        }
        computeAlignment();
        int m = musicNotes.size() - 1;
        int t = transcribedNotes.size() - 1;
        List<NotePair> pairs = new ArrayList<NotePair>();
        while (m != 0 && t != 0) {
            // Note that we're always adding at 0 here, because we're building the array backwards.
            if (alignment[t][m] == NOTES_ALIGNED) {
                pairs.add(0, new NotePair(transcribedNotes.get(t), musicNotes.get(m)));
                m--;
                t--;
            } else if (alignment[t][m] == TRANSCRIBED_NOTE_ADDED) {
                pairs.add(0, new NotePair(transcribedNotes.get(t), null));
                t--;
            } else if (alignment[t][m] == MUSIC_NOTE_SKIPPED) {
                pairs.add(0, new NotePair(null, musicNotes.get(m)));
                m--;
            } else {
                throw new RuntimeException("There's a bug somewhere");
            }
        }
        // Once we get to one of the edges, we add skipped or added notes until we get to the top
        // left corner.
        while (m >= 0) {
            pairs.add(0, new NotePair(null, musicNotes.get(m)));
            m--;
        }
        while (t >= 0) {
            pairs.add(0, new NotePair(transcribedNotes.get(t), null));
            t--;
        }
        return new NoteAlignment(pairs);
    }

    public double getBeatsPerMinute() {
        return 60.0 / secondsPerBeat;
    }

    private void estimateTempo() {
        NoteAlignment alignment = getAlignment();
        List<NotePair> pairs = alignment.getPairs();
        List<Double> tempoEstimates = new ArrayList<Double>();
        int pair_index = 0;
        double beats = -1;
        double startTime = -1;
        while (pair_index < pairs.size()) {
            NotePair pair = pairs.get(pair_index);
            pair_index++;
            if (pair.getMusicNote() == null) {
                startTime = -1;
                continue;
            }
            if (pair.getTranscribedNote() == null) {
                startTime = -1;
                continue;
            }
            if (startTime != -1) {
                double endTime = pair.getTranscribedNote().getEndTime();
                double tempoEstimate = (endTime - startTime) / beats;
                tempoEstimates.add(tempoEstimate);
            }
            beats = pair.getMusicNote().getBeats();
            startTime = pair.getTranscribedNote().getStartTime();
        }
        if (tempoEstimates.size() < 3) {
            // If we haven't seen 3 good notes yet, don't try to estimate the tempo
            return;
        }
        Collections.sort(tempoEstimates);
        // Take the median tempo as our estimate
        secondsPerBeat = tempoEstimates.get(tempoEstimates.size()/2);
    }

    private double noteSkipCost(int m_index) {
        // Guiding principle here: the shorter the note's duration, the more likely it is for a
        // person to miss it, so the less it costs to skip.
        return musicNotes.get(m_index).getBeats();
    }

    private double noteAddCost(int t_index) {
        // Guiding principle: as above, the shorter the note's duration, the more likely it is to
        // be accidental.  Also, because of some deficencies in my transcription code, very short
        // notes get spuriously added, and so they should have a low cost (and some special case in
        // the UI to just ignore them).
        double denom = secondsPerBeat;
        if (denom == -1) {
            // Just use 80 beats per minute if we don't have an estimate of the tempo yet
            denom = 60.0/80;
        }
        return transcribedNotes.get(t_index).getDuration() / denom;
    }

    private double noteAlignCost(int t_index, int m_index) {
        // Guiding principle: length of note is more important than pitch, but not by a lot.  If
        // the notes are off by an octave, maybe the transcription just got the octave wrong.
        MusicNote m = musicNotes.get(m_index);
        TranscribedNote t = transcribedNotes.get(t_index);
        double cost = 0.0;
        if (secondsPerBeat != -1) {
            double musicBeats = m.getBeats();
            double startTime = t.getStartTime();
            double endTime;
            if (t_index < transcribedNotes.size() - 1) {
                endTime = transcribedNotes.get(t_index+1).getStartTime();
            } else {
                endTime = t.getEndTime();
            }
            double transcribedBeats = (endTime - startTime) / secondsPerBeat;
            double percentOff = Math.abs(musicBeats - transcribedBeats) / musicBeats;
            cost += 2 * percentOff;
        }
        double musicPitch = NoteUtil.getNoteFrequency(m.getName());
        double transcribedPitch = t.getPitch();
        double percentOff = Math.abs(musicPitch - transcribedPitch) / musicPitch;
        cost += 10 * percentOff;
        return cost;
    }

    private void computeAlignment() {
        alignment = new byte[transcribedNotes.size()][musicNotes.size()];
        double[][] costMatrix = new double[transcribedNotes.size()][musicNotes.size()];
        for (int t=0; t<transcribedNotes.size(); t++) {
            for (int m=0; m<musicNotes.size(); m++) {
                if (t == 0) {
                    // If we're on the first row, there are two possibilites: either we skipped all
                    // prior notes, or we played one prior note and skipped the rest.  We need to
                    // check both possibilities and keep the best one.
                    double alignCost = 0.0;
                    for (int i=0; i<m; i++) {
                        alignCost += noteSkipCost(i);
                    }
                    alignCost += noteAlignCost(t, m);
                    if (m == 0) {
                        // If this is the top left corner, the only choice is to align the notes
                        costMatrix[t][m] = alignCost;
                        alignment[t][m] = NOTES_ALIGNED;
                        continue;
                    }
                    double skipCost = noteSkipCost(m) + costMatrix[t][m-1];
                    if (skipCost < alignCost) {
                        costMatrix[t][m] = skipCost;
                        alignment[t][m] = MUSIC_NOTE_SKIPPED;
                    } else {
                        costMatrix[t][m] = alignCost;
                        alignment[t][m] = NOTES_ALIGNED;
                    }
                } else if (m == 0) {
                    // If we're on the first column, we have a similar situation to the above,
                    // except we're adding transcribed notes instead of skipping music notes.
                    // And we don't need to check for t == 0, because that case is already covered
                    // above.
                    double alignCost = 0.0;
                    for (int i=0; i<t; i++) {
                        alignCost += noteAddCost(i);
                    }
                    alignCost += noteAlignCost(t, m);
                    double addCost = noteAddCost(m) + costMatrix[t-1][m];
                    if (addCost < alignCost) {
                        costMatrix[t][m] = addCost;
                        alignment[t][m] = TRANSCRIBED_NOTE_ADDED;
                    } else {
                        costMatrix[t][m] = alignCost;
                        alignment[t][m] = NOTES_ALIGNED;
                    }
                } else {
                    // We've now handled the edge cases, so we're in the middle of the matrix and
                    // there are three possibilities.  Either the two notes align, the music note
                    // was skipped, or the transcribed note was added.
                    double alignCost = noteAlignCost(t, m) + costMatrix[t-1][m-1];
                    double skipCost = noteSkipCost(m) + costMatrix[t][m-1];
                    double addCost = noteAddCost(t) + costMatrix[t-1][m];
                    if (addCost < skipCost && addCost < alignCost) {
                        costMatrix[t][m] = addCost;
                        alignment[t][m] = TRANSCRIBED_NOTE_ADDED;
                    } else if (skipCost < addCost && skipCost < alignCost) {
                        costMatrix[t][m] = skipCost;
                        alignment[t][m] = MUSIC_NOTE_SKIPPED;
                    } else {
                        costMatrix[t][m] = alignCost;
                        alignment[t][m] = NOTES_ALIGNED;
                    }
                }
            }
        }
    }
}
