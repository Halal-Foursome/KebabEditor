package org.halalfoursome.kebabeditor.editor;

import java.util.List;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A copy of everything an edit can change in a scene. Restoring writes it back
 * into the same node and archetype objects, so the selection, open dialogs and
 * references by id keep pointing at the right things.
 */
final class SceneSnapshot {

    // values is null for unresolved archetypes, which are immutable
    private record ArchetypeState(NodeArchetype archetype, Map<String, Object> values) {}

    private record NodeState(
        SceneNode node,
        Maybe<SceneNode> parent,
        String name,
        Transform transform,
        JsonNode rawJson,
        List<ArchetypeState> archetypes
    ) {}

    // In traversal order, so every parent comes before its children
    private final List<NodeState> nodes;
    private final List<SceneNode> selected;

    private SceneSnapshot(List<NodeState> nodes, List<SceneNode> selected) {
        this.nodes = nodes;
        this.selected = selected;
    }

    static SceneSnapshot capture(Scene scene, Selection selection) {
        List<NodeState> nodes = scene.traverse()
            .map(node -> new NodeState(
                node,
                node.getParent(),
                node.getName(),
                node.getTransform().copy(),
                node.getRawJson().deepCopy(),
                node.getArchetypes().stream()
                    .map(archetype -> new ArchetypeState(
                        archetype,
                        archetype instanceof ArchetypeInstance instance ? instance.copyValues() : null
                    ))
                    .toList()
            ))
            .toList();

        return new SceneSnapshot(nodes, selection.nodes());
    }

    /** Whether both snapshots hold the same scene; the selection alone is not an edit. */
    boolean sameScene(SceneSnapshot other) {
        return nodes.equals(other.nodes);
    }

    void restore(Scene scene, Selection selection) {
        scene.clear();
        // Nodes deleted since the capture are outside the scene but still linked to their subtree.
        nodes.forEach(state -> scene.unlink(state.node()));

        for (NodeState state : nodes) {
            SceneNode node = state.node();
            node.setName(state.name());
            node.setTransform(state.transform().copy());
            node.setRawJson(state.rawJson().deepCopy());

            List.copyOf(node.getArchetypes()).forEach(node::removeArchetype);
            for (ArchetypeState archetype : state.archetypes()) {
                if (archetype.archetype() instanceof ArchetypeInstance instance) {
                    instance.restoreValues(archetype.values());
                }
                node.addArchetype(archetype.archetype());
            }

            try {
                if (state.parent().isSome()) {
                    scene.add(node, state.parent().unwrap());
                } else {
                    scene.add(node);
                }
            } catch (SceneException e) {
                // The scene was emptied and parents are added before their children.
                throw new IllegalStateException("Cannot restore '" + node.getName() + "'", e);
            }
        }

        selection.set(selected.stream()
            .filter(node -> scene.find(node.getId()).isSomeAnd(found -> found == node))
            .toList());
    }
}
