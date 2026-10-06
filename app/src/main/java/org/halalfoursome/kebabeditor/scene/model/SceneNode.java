package org.halalfoursome.kebabeditor.scene.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SceneNode {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final NodeId id;

    @Setter
    @ToString.Include
    private String name;

    @Setter
    private Transform transform;

    @Setter
    private JsonNode rawJson;

    // parent and children are only changed by Scene, which keeps both sides in sync
    private Maybe<SceneNode> parent = Maybe.none();
    private final List<SceneNode> children = new ArrayList<>();
    private final List<NodeArchetype> archetypes = new ArrayList<>();

    public SceneNode(NodeId id, String name, Transform transform, JsonNode rawJson) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.transform = transform;
        this.rawJson = rawJson;
    }

    public SceneNode(String name) {
        this(
            new NodeId(UUID.randomUUID()),
            name,
            Transform.identity(),
            JsonNodeFactory.instance.objectNode()
        );
    }

    public List<SceneNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public List<NodeArchetype> getArchetypes() {
        return Collections.unmodifiableList(archetypes);
    }

    public void addArchetype(NodeArchetype archetype) {
        archetypes.add(archetype);
    }

    public void removeArchetype(NodeArchetype archetype) {
        archetypes.remove(archetype);
    }

    void attachChild(SceneNode child) {
        children.add(child);
        child.parent = Maybe.some(this);
    }

    void detachChild(SceneNode child) {
        children.remove(child);
        child.parent = Maybe.none();
    }
}
