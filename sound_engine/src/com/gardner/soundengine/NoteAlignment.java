package com.gardner.soundengine;

import java.util.List;

/**
 * Represents an alignment between a list of TranscribedNotes and a list of MusicNotes.
 */
public class NoteAlignment {
    private final List<NotePair> pairs;

    public NoteAlignment(List<NotePair> pairs) {
        this.pairs = pairs;
    }

    public String getStringRepresentation() {
        // TODO
        return null;
    }
}
