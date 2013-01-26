package com.gardner.soundengine.alignment;

import java.util.List;

import com.gardner.soundengine.common.*;

/**
 * Represents an alignment between a list of TranscribedNotes and a list of MusicNotes.
 */
public class NoteAlignment {
    private final List<NotePair> pairs;

    public NoteAlignment(List<NotePair> pairs) {
        this.pairs = pairs;
    }

    public List<NotePair> getPairs() {
        return pairs;
    }

    /**
     * For now, this returns a two line string, with the music notes on top and the transcribed
     * notes on the bottom.
     */
    public String getStringRepresentation() {
        StringBuilder builder = new StringBuilder();
        for (NotePair pair : pairs) {
            if (pair.getMusicNote() == null) {
                builder.append("    ");
            } else {
                builder.append(String.format("%4s", pair.getMusicNote().getName()));
            }
            String m = "    ";
            if (pair.getMusicNote() != null) {
                m = pair.getMusicNote().getName();
            }
            String t = "    ";
            if (pair.getTranscribedNote() != null) {
                t = NoteUtil.findClosestNote(pair.getTranscribedNote().getPitch());
            }
            System.out.println(m + " ---- " + t);
        }
        builder.append("\n");
        for (NotePair pair : pairs) {
            if (pair.getTranscribedNote() == null) {
                builder.append("    ");
            } else {
                builder.append(String.format("%4s",
                            NoteUtil.findClosestNote(pair.getTranscribedNote().getPitch())));
            }
        }
        return builder.toString();
    }
}
