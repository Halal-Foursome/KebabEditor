package org.halalfoursome.kebabeditor.editor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Undo and redo for scene edits. Each step keeps the scene as it was before
 * and after the edit, so undoing and redoing restore whole snapshots.
 */
public final class History {

    private static final int LIMIT = 100;

    private record Step(String label, SceneSnapshot before, SceneSnapshot after) {}

    private final Supplier<Maybe<Scene>> scene;
    private final Selection selection;
    private final Deque<Step> undoSteps = new ArrayDeque<>();
    private final Deque<Step> redoSteps = new ArrayDeque<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private Maybe<SceneSnapshot> pending = Maybe.none();
    private int depth;

    History(Supplier<Maybe<Scene>> scene, Selection selection) {
        this.scene = scene;
        this.selection = selection;
    }

    /** Runs {@code change} as one undoable step. Nested calls fold into the outer step. */
    public void record(String label, Runnable change) {
        record(label, () -> {
            change.run();
            return null;
        });
    }

    public <T> T record(String label, Supplier<T> change) {
        if (depth > 0 || pending.isSome() || scene.get().isNone()) {
            return change.get();
        }

        SceneSnapshot before = capture();
        depth++;
        try {
            return change.get();
        } finally {
            depth--;
            push(label, before);
        }
    }

    /** Starts a step that spans several events, like a gizmo drag; ends with {@link #commit} or {@link #cancel}. */
    public void begin() {
        if (pending.isNone() && scene.get().isSome()) {
            pending = Maybe.some(capture());
            notifyListeners();
        }
    }

    public void commit(String label) {
        pending.ifSome(before -> {
            pending = Maybe.none();
            push(label, before);
            notifyListeners();
        });
    }

    /** Drops the step started by {@link #begin}; the caller has already put the scene back. */
    public void cancel() {
        if (pending.isSome()) {
            pending = Maybe.none();
            notifyListeners();
        }
    }

    public boolean canUndo() {
        return pending.isNone() && !undoSteps.isEmpty() && scene.get().isSome();
    }

    public boolean canRedo() {
        return pending.isNone() && !redoSteps.isEmpty() && scene.get().isSome();
    }

    /** @return the label of the undone step, or none when there was nothing to undo */
    public Maybe<String> undo() {
        if (!canUndo()) {
            return Maybe.none();
        }

        Step step = undoSteps.pop();
        step.before().restore(scene.get().unwrap(), selection);
        redoSteps.push(step);
        notifyListeners();
        return Maybe.some(step.label());
    }

    /** @return the label of the redone step, or none when there was nothing to redo */
    public Maybe<String> redo() {
        if (!canRedo()) {
            return Maybe.none();
        }

        Step step = redoSteps.pop();
        step.after().restore(scene.get().unwrap(), selection);
        undoSteps.push(step);
        notifyListeners();
        return Maybe.some(step.label());
    }

    public void clear() {
        undoSteps.clear();
        redoSteps.clear();
        pending = Maybe.none();
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private SceneSnapshot capture() {
        return SceneSnapshot.capture(scene.get().unwrap(), selection);
    }

    private void push(String label, SceneSnapshot before) {
        if (scene.get().isNone()) {
            return;
        }

        SceneSnapshot after = capture();
        if (before.sameScene(after)) {
            return;
        }

        undoSteps.push(new Step(label, before, after));
        if (undoSteps.size() > LIMIT) {
            undoSteps.removeLast();
        }
        redoSteps.clear();
        notifyListeners();
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
