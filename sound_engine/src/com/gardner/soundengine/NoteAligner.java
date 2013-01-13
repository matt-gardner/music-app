package com.gardner.soundengine;

import java.util.ArrayList;
import java.util.List;

public class NoteAligner {
    private List<MusicNote> musicNotes;
    private List<TranscribedNote> transcribedNotes;
    private int lastTranscribedNote;
    private byte[][] alignment;

    private byte MUSIC_NOTE_SKIPPED = 1;
    private byte TRANSCRIBED_NOTE_ADDED = 2;
    private byte NOTES_ALIGNED = 2;

    public NoteAligner(SheetMusic music) {
        musicNotes = music.getNotes();
        transcribedNotes = new ArrayList<TranscribedNote>();
        lastTranscribedNote = 0;
    }

    public void updateAlignment(List<TranscribedNote> notes) {
        // Right now we just redo the whole alignment every time update is called - not a big deal
        // for short files.  But this is written so that it can be easily made incremental.
        for (; lastTranscribedNote<notes.size(); lastTranscribedNote++) {
            transcribedNotes.add(notes.get(lastTranscribedNote));
        }
        computeAlignment();
    }

    /**
     * Gets the currently computed alignment.  This won't return anything meaningful unless
     * updateAlignment is called first.
     */
    public NoteAlignment getAlignment() {
        if (alignment == null) {
            computeAlignment();
        }
        // TODO
        return null;
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
                    alignCost += alignCost(t, m);
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
                    alignCost += alignCost(t, m);
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
                    double alignCost = alignCost(t, m) + costMatrix[t-1][m-1];
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
