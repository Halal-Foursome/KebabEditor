package org.halalfoursome.kebabeditor.scene.model;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class SceneNode {
    
    private NodeId id;
    private String name;
    private Transform transform;
    private Maybe<SceneNode> parent;
    private List<SceneNode> children;
    private List<NodeArchetype> archetypes = new ArrayList<>();
    private JsonNode rawJson;
}
