package org.halalfoursome.kebabeditor.scene.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lombok.Getter;

public class Scene {

    private final List<SceneNode> rootNodes = new ArrayList<>();
    private final Map<NodeId, SceneNode> byId = new HashMap<>();

    @Getter
    private final JsonNode document;

    public Scene() {
        this(JsonNodeFactory.instance.objectNode());
    }

    public Scene(JsonNode document) {
        this.document = document;
    }

    public List<SceneNode> getRootNodes() {
        return Collections.unmodifiableList(rootNodes);
    }

    public Maybe<SceneNode> find(NodeId id) {
        return Maybe.ofNullable(byId.get(id));
    }

    public Stream<SceneNode> traverse() {
        return rootNodes.stream().flatMap(Scene::subtree);
    }

    public void add(SceneNode node) throws SceneException {
        register(node);
        rootNodes.add(node);
    }

    public void add(SceneNode node, SceneNode parent) throws SceneException {
        requireInScene(parent);
        register(node);
        parent.attachChild(node);
    }

    public void add(SceneNode node, NodeId parent) throws SceneException {
        add(node, get(parent));
    }

    public void remove(NodeId id) throws SceneException {
        remove(get(id));
    }

    public void remove(SceneNode node) throws SceneException {
        requireInScene(node);

        if (node.getParent().isSome()) {
            node.getParent().unwrap().detachChild(node);
        } else {
            rootNodes.remove(node);
        }

        subtree(node).forEach(n -> byId.remove(n.getId()));
    }

    // Checks the whole subtree first, so a failed add leaves the scene untouched
    private void register(SceneNode node) throws SceneException {
        if (node.getParent().isSome()) {
            throw new SceneException(
                "Node '" + node.getName() + "' already has a parent"
            );
        }

        List<SceneNode> added = subtree(node).toList();
        Set<NodeId> seen = new HashSet<>();

        for (SceneNode n : added) {
            if (byId.containsKey(n.getId()) || !seen.add(n.getId())) {
                throw new SceneException(
                    "Duplicate node id " + n.getId().value() + " ('" + n.getName() + "')"
                );
            }
        }

        added.forEach(n -> byId.put(n.getId(), n));
    }

    private SceneNode get(NodeId id) throws SceneException {
        if (byId.get(id) == null) {
            throw new SceneException("No node with id " + id.value() + " in this scene");
        }

        return byId.get(id);
    }

    private void requireInScene(SceneNode node) throws SceneException {
        if (byId.get(node.getId()) != node) {
            throw new SceneException(
                "Node '" + node.getName() + "' is not in this scene"
            );
        }
    }

    private static Stream<SceneNode> subtree(SceneNode node) {
        return Stream.concat(
            Stream.of(node),
            node.getChildren().stream().flatMap(Scene::subtree)
        );
    }
}
