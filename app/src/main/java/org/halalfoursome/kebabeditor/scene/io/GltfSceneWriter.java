package org.halalfoursome.kebabeditor.scene.io;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstanceJson;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.archetype.instance.UnresolvedArchetype;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GltfSceneWriter {

    public JsonNode write(Scene scene) throws SceneFormatException {
        if (!(scene.getDocument() instanceof ObjectNode document)) {
            throw new SceneFormatException("The glTF document is not an object");
        }

        if (!document.has(GltfKeys.ASSET)) {
            document.putObject(GltfKeys.ASSET).put("version", "2.0");
        }

        ArrayNode nodes = arrayOf(document, GltfKeys.NODES);
        Map<JsonNode, Integer> indices = assignIndices(scene, nodes);

        scene.traverse().forEach(node -> writeNode(node, indices));
        writeRoots(document, scene.getRootNodes(), indices);

        return document;
    }

    private Map<JsonNode, Integer> assignIndices(Scene scene, ArrayNode nodes) {
        Map<JsonNode, Integer> indices = new IdentityHashMap<>();

        for (int i = 0; i < nodes.size(); i++) {
            indices.put(nodes.get(i), i);
        }

        scene.traverse().forEach(node -> {
            if (!(node.getRawJson() instanceof ObjectNode json)) {
                node.setRawJson(nodes.addObject());
                indices.put(node.getRawJson(), nodes.size() - 1);
            } else if (!indices.containsKey(json)) {
                nodes.add(json);
                indices.put(json, nodes.size() - 1);
            }
        });

        return indices;
    }

    private void writeNode(SceneNode node, Map<JsonNode, Integer> indices) {
        ObjectNode json = (ObjectNode) node.getRawJson();

        if (node.getName() != null) {
            json.put(GltfKeys.NAME, node.getName());
        } else {
            json.remove(GltfKeys.NAME);
        }

        writeTransform(json, node.getTransform());
        writeChildren(json, node.getChildren(), indices);
        writeExtras(json, node);
    }

    private void writeTransform(ObjectNode json, Transform transform) {
        Vec3 t = transform.getTranslation();
        Quat r = transform.getRotation();
        float k = transform.getScale();

        float[] translation = { t.getX(), t.getY(), t.getZ() };
        float[] rotation = { r.getX(), r.getY(), r.getZ(), r.getW() };
        float[] scale = { k, k, k };

        json.remove(GltfKeys.MATRIX);
        putIfChanged(json, GltfKeys.TRANSLATION, translation, new float[] { 0, 0, 0 });
        putIfChanged(json, GltfKeys.ROTATION, rotation, new float[] { 0, 0, 0, 1 });
        putIfChanged(json, GltfKeys.SCALE, scale, new float[] { 1, 1, 1 });
    }

    private void putIfChanged(ObjectNode json, String key, float[] values, float[] defaults) {
        JsonNode existing = json.get(key);

        if (existing == null) {
            if (!Arrays.equals(values, defaults)) {
                putArray(json, key, values);
            }
            return;
        }

        if (existing.isArray() && existing.size() == values.length) {
            boolean same = true;
            for (int i = 0; i < values.length; i++) {
                same &= existing.get(i).isNumber() && existing.get(i).floatValue() == values[i];
            }
            if (same) {
                return;
            }
        }

        putArray(json, key, values);
    }

    private void putArray(ObjectNode json, String key, float[] values) {
        ArrayNode array = json.putArray(key);
        for (float value : values) {
            array.add(value);
        }
    }

    private void writeChildren(ObjectNode json, List<SceneNode> children, Map<JsonNode, Integer> indices) {
        if (children.isEmpty()) {
            json.remove(GltfKeys.CHILDREN);
            return;
        }

        ArrayNode array = json.putArray(GltfKeys.CHILDREN);
        children.forEach(child -> array.add(indices.get(child.getRawJson())));
    }

    private void writeExtras(ObjectNode json, SceneNode node) {
        ObjectNode extras = json.get(GltfKeys.EXTRAS) instanceof ObjectNode e
            ? e
            : json.putObject(GltfKeys.EXTRAS);
        ObjectNode kebab = extras.get(GltfKeys.KEBAB) instanceof ObjectNode k
            ? k
            : extras.putObject(GltfKeys.KEBAB);

        kebab.put(GltfKeys.ID, node.getId().value().toString());

        if (node.getArchetypes().isEmpty()) {
            kebab.remove(GltfKeys.ARCHETYPES);
            return;
        }

        ArrayNode archetypes = kebab.putArray(GltfKeys.ARCHETYPES);
        node.getArchetypes().forEach(archetype -> archetypes.add(archetypeToJson(archetype)));
    }

    private JsonNode archetypeToJson(NodeArchetype archetype) {
        return switch (archetype) {
            case ArchetypeInstance instance -> {
                ArchetypeInstanceJson dto = ArchetypeInstanceJson.from(instance);
                ObjectNode json = JsonNodeFactory.instance.objectNode();
                json.put(GltfKeys.ID, dto.id());
                ObjectNode params = json.putObject(GltfKeys.PARAMS);
                dto.params().forEach(params::set);
                yield json;
            }
            case UnresolvedArchetype unresolved -> unresolved.rawJson().deepCopy();
        };
    }

    private void writeRoots(
        ObjectNode document, 
        List<SceneNode> roots, 
        Map<JsonNode, Integer> indices
    ) throws SceneFormatException {
        ArrayNode scenes = arrayOf(document, GltfKeys.SCENES);

        if (scenes.isEmpty()) {
            scenes.addObject();
            document.put(GltfKeys.SCENE, 0);
        }

        int sceneIndex = document.path(GltfKeys.SCENE).asInt(0);
        if (!(scenes.get(sceneIndex) instanceof ObjectNode sceneJson)) {
            throw new SceneFormatException("Scene " + sceneIndex + " does not exist");
        }

        if (roots.isEmpty()) {
            sceneJson.remove(GltfKeys.NODES);
            return;
        }

        ArrayNode array = sceneJson.putArray(GltfKeys.NODES);
        roots.forEach(root -> array.add(indices.get(root.getRawJson())));
    }

    private ArrayNode arrayOf(
        ObjectNode document, 
        String key
    ) throws SceneFormatException {
        JsonNode existing = document.get(key);

        if (existing == null) {
            return document.putArray(key);
        }

        if (existing instanceof ArrayNode array) {
            return array;
        }

        throw new SceneFormatException("`" + key + "` is not an array");
    }
}
