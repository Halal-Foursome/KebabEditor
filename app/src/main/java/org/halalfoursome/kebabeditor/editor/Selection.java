package org.halalfoursome.kebabeditor.editor;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class Selection {

    private final List<SceneNode> nodes = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public List<SceneNode> nodes() {
        return List.copyOf(nodes);
    }

    public Maybe<SceneNode> primary() {
        return nodes.isEmpty() 
            ? Maybe.none() 
            : Maybe.some(nodes.getLast());
    }

    public boolean contains(SceneNode node) {
        return nodes.contains(node);
    }

    public void select(SceneNode node) {
        nodes.clear();
        nodes.add(node);
        notifyListeners();
    }

    public void set(List<SceneNode> selected) {
        if (nodes.equals(selected)) {
            return;
        }
        nodes.clear();
        nodes.addAll(selected);
        notifyListeners();
    }

    public void clear() {
        if (nodes.isEmpty()) {
            return;
        }
        nodes.clear();
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
