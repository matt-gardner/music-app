package com.gardner.soundengine;

/**
 * A pair of notes, one of which is a TranscribedNote and one of which is a MusicNote.  Used for
 * NoteAlignments.
 */
public class NotePair {
    private final TranscribedNote transcribedNote;
    private final MusicNote musicNote;

    public NotePair(TranscribedNote transcribedNote, MusicNote musicNote) {
        this.transcribedNote = transcribedNote;
        this.musicNote = musicNote;
    }

    public TranscribedNote getTranscribedNote() {
        return transcribedNote;
    }

    public MusicNote getMusicNote() {
        return musicNote;
    }
}
