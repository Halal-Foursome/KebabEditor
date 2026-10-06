package org.halalfoursome.kebabeditor.scene.io;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstanceJson;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.model.NodeId;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GltfSceneReader {

    private static final float UNIFORM_EPSILON = 1e-6f;

    public Scene read(JsonNode document, ArchetypeRegistry registry) throws SceneException {
        Scene scene = new Scene(document);

        JsonNode scenes = document.path(GltfKeys.SCENES);
        if (!scenes.isArray() || scenes.isEmpty()) {
            return scene;
        }

        int sceneIndex = document.path(GltfKeys.SCENE).asInt(0);
        JsonNode sceneJson = scenes.get(sceneIndex);
        if (sceneJson == null) {
            throw new SceneFormatException("Scene " + sceneIndex + " does not exist");
        }

        new Loading(scene, document.path(GltfKeys.NODES), registry)
            .loadAll(sceneJson.path(GltfKeys.NODES));

        return scene;
    }

    private static final class Loading {

        private final Scene scene;
        private final JsonNode nodes;
        private final ArchetypeRegistry registry;
        private final Set<Integer> visited = new HashSet<>();

        Loading(Scene scene, JsonNode nodes, ArchetypeRegistry registry) {
            this.scene = scene;
            this.nodes = nodes;
            this.registry = registry;
        }

        void loadAll(JsonNode rootIndices) throws SceneException {
            for (JsonNode index : rootIndices) {
                load(index, Maybe.none());
            }
        }

        private void load(JsonNode indexJson, Maybe<SceneNode> parent) throws SceneException {
            int index = indexOf(indexJson);

            if (!visited.add(index)) {
                throw new SceneFormatException("Node " + index + " is referenced more than once");
            }

            if (!(nodes.get(index) instanceof ObjectNode json)) {
                throw new SceneFormatException("Node " + index + " is not an object");
            }

            SceneNode node = new SceneNode(
                readId(json, index),
                json.path(GltfKeys.NAME).asText(null),
                readTransform(json, index),
                json
            );
            readArchetypes(json, node, index);

            if (parent.isSome()) {
                scene.add(node, parent.unwrap());
            } else {
                scene.add(node);
            }

            for (JsonNode child : json.path(GltfKeys.CHILDREN)) {
                load(child, Maybe.some(node));
            }
        }

        private int indexOf(JsonNode indexJson) throws SceneFormatException {
            if (!indexJson.canConvertToInt() || !indexJson.isIntegralNumber()) {
                throw new SceneFormatException("Invalid node index: " + indexJson);
            }

            int index = indexJson.intValue();
            if (index < 0 || index >= nodes.size()) {
                throw new SceneFormatException("Node index out of range: " + index);
            }

            return index;
        }

        private NodeId readId(ObjectNode json, int index) throws SceneFormatException {
            JsonNode id = json.path(GltfKeys.EXTRAS).path(GltfKeys.KEBAB).path(GltfKeys.ID);

            if (id.isMissingNode()) {
                return new NodeId(UUID.randomUUID());
            }

            try {
                return new NodeId(UUID.fromString(id.asText()));
            } catch (IllegalArgumentException e) {
                throw new SceneFormatException("Node " + index + " has an invalid id: " + id, e);
            }
        }

        private Transform readTransform(ObjectNode json, int index) throws SceneFormatException {
            if (json.has(GltfKeys.MATRIX)) {
                throw new SceneFormatException(
                    "Node " + index + " uses a matrix; only translation/rotation/scale are supported"
                );
            }

            float[] t = floats(json, GltfKeys.TRANSLATION, 3, new float[] { 0, 0, 0 }, index);
            float[] r = floats(json, GltfKeys.ROTATION, 4, new float[] { 0, 0, 0, 1 }, index);
            float[] s = floats(json, GltfKeys.SCALE, 3, new float[] { 1, 1, 1 }, index);

            if (Math.abs(s[0] - s[1]) > UNIFORM_EPSILON || Math.abs(s[0] - s[2]) > UNIFORM_EPSILON) {
                throw new SceneFormatException("Node " + index + " has a non-uniform scale");
            }

            return new Transform(new Vec3(t[0], t[1], t[2]), new Quat(r[0], r[1], r[2], r[3]), s[0]);
        }

        private float[] floats(
            ObjectNode json, 
            String key, 
            int size, 
            float[] fallback, 
            int index
        ) throws SceneFormatException {
            JsonNode array = json.get(key);

            if (array == null) {
                return fallback;
            }

            if (!array.isArray() || array.size() != size) {
                throw new SceneFormatException("Node " + index + ": `" + key + "` must have " + size + " numbers");
            }

            float[] values = new float[size];
            for (int i = 0; i < size; i++) {
                if (!array.get(i).isNumber()) {
                    throw new SceneFormatException("Node " + index + ": `" + key + "` must contain only numbers");
                }
                values[i] = array.get(i).floatValue();
            }

            return values;
        }

        private void readArchetypes(ObjectNode json, SceneNode node, int index) throws SceneFormatException {
            JsonNode archetypes = json.path(GltfKeys.EXTRAS)
                .path(GltfKeys.KEBAB)
                .path(GltfKeys.ARCHETYPES);

            for (JsonNode archetype : archetypes) {
                if (!archetype.path(GltfKeys.ID).isTextual()) {
                    throw new SceneFormatException("Node " + index + " has an archetype without an id");
                }

                Map<String, JsonNode> params = new LinkedHashMap<>();
                archetype.path(GltfKeys.PARAMS).properties()
                    .forEach(entry -> params.put(entry.getKey(), entry.getValue()));

                node.addArchetype(
                    new ArchetypeInstanceJson(archetype.get(GltfKeys.ID).textValue(), params)
                        .toInstance(registry)
                );
            }
        }
    }
}
