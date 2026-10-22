package org.halalfoursome.kebabeditor.playtest;

import java.util.ArrayList;
import java.util.List;

/**
 * The recorded paths of past playtests that the viewport draws. The newest is
 * drawn brightest; only the last {@link #MAX_RECORDINGS} are kept.
 */
public final class PathRays {

    public static final int MAX_RECORDINGS = 8;

    private final List<PathRecording> recordings = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private boolean visible = true;

    /** Oldest first. */
    public List<PathRecording> recordings() {
        return List.copyOf(recordings);
    }

    public boolean isEmpty() {
        return recordings.isEmpty();
    }

    /** Keep a new recording and show the rays, so the run just finished is visible. */
    public void add(PathRecording recording) {
        recordings.add(recording);
        while (recordings.size() > MAX_RECORDINGS) {
            recordings.removeFirst();
        }
        visible = true;
        notifyListeners();
    }

    public void clear() {
        if (recordings.isEmpty()) {
            return;
        }
        recordings.clear();
        notifyListeners();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
