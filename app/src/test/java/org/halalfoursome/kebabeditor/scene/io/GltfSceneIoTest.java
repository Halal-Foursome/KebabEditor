package org.halalfoursome.kebabeditor.scene.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.UnresolvedArchetype;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class GltfSceneIoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SAMPLE = """
        {
          "asset": { "version": "2.0", "generator": "test" },
          "scene": 0,
          "scenes": [ { "nodes": [0, 2] } ],
          "nodes": [
            { "name": "root", "children": [1], "translation": [0.123456789012, 0, 0],
              "extras": { "other": 42, "kebab": { "id": "11111111-1111-1111-1111-111111111111",
                "archetypes": [
                  { "id": "tag", "params": { "label": "hello", "pos": "(1.0, 2.0, 3.0)", "target": null } },
                  { "id": "gone", "params": { "x": 1 } }
                ] } } },
            { "name": "child", "mesh": 0, "scale": [2, 2, 2] },
            { "name": "second" }
          ],
          "meshes": [ { "name": "m", "primitives": [] } ]
        }
        """;

    private static ArchetypeRegistry registry() {
        Map<String, ParamType> params = new LinkedHashMap<>();
        params.put("label", ParamType.STRING);
        params.put("pos", ParamType.VEC3);
        params.put("target", ParamType.NODE_REF);

        ArchetypeRegistry registry = new ArchetypeRegistry();
        registry.add(new ArchetypeDefinition("tag", "Tag", Maybe.none(), params));
        return registry;
    }

    private static JsonNode json(String text) throws Exception {
        return MAPPER.readTree(text);
    }

    @Test
    void readBuildsTreeTransformsAndArchetypes() throws Exception {
        Scene scene = new GltfSceneReader().read(json(SAMPLE), registry());

        assertEquals(2, scene.getRootNodes().size());
        SceneNode root = scene.getRootNodes().get(0);
        assertEquals("root", root.getName());
        assertEquals(1, root.getChildren().size());
        assertEquals(2f, root.getChildren().get(0).getTransform().getScale());
        assertEquals(3, scene.traverse().count());

        assertEquals(2, root.getArchetypes().size());
        assertInstanceOf(ArchetypeInstance.class, root.getArchetypes().get(0));
        assertInstanceOf(UnresolvedArchetype.class, root.getArchetypes().get(1));
    }

    @Test
    void writeIsIdempotentAndKeepsEverythingElse() throws Exception {
        GltfSceneReader reader = new GltfSceneReader();
        GltfSceneWriter writer = new GltfSceneWriter();

        JsonNode first = writer.write(reader.read(json(SAMPLE), registry())).deepCopy();
        JsonNode second = writer.write(reader.read(first.deepCopy(), registry())).deepCopy();

        assertEquals(first, second);

        JsonNode original = json(SAMPLE);
        assertEquals(original.get("meshes"), first.get("meshes"));
        assertEquals(original.get("asset"), first.get("asset"));
        assertEquals(original.get("nodes").get(1).get("mesh"), first.get("nodes").get(1).get("mesh"));
        assertEquals(42, first.get("nodes").get(0).get("extras").get("other").asInt());
        assertEquals(
            original.get("nodes").get(0).get("extras").get("kebab").get("archetypes").get(1),
            first.get("nodes").get(0).get("extras").get("kebab").get("archetypes").get(1)
        );
        assertEquals(0.123456789012, first.get("nodes").get(0).get("translation").get(0).doubleValue());
    }

    @Test
    void nodesWithoutIdsGetStableIdsOnSave() throws Exception {
        Scene scene = new GltfSceneReader().read(json(SAMPLE), registry());
        JsonNode written = new GltfSceneWriter().write(scene);

        String id = written.get("nodes").get(2).get("extras").get("kebab").get("id").asText();
        assertEquals(scene.getRootNodes().get(1).getId().value().toString(), id);
    }

    @Test
    void addedNodesAreAppendedAndRemovedOnesStayAsOrphans() throws Exception {
        Scene scene = new GltfSceneReader().read(json(SAMPLE), registry());
        SceneNode root = scene.getRootNodes().get(0);
        SceneNode added = new SceneNode("added");
        scene.add(added, root);
        scene.remove(scene.getRootNodes().get(1));

        JsonNode written = new GltfSceneWriter().write(scene);

        assertEquals(4, written.get("nodes").size());
        assertEquals("added", written.get("nodes").get(3).get("name").asText());
        assertEquals("[1,3]", written.get("nodes").get(0).get("children").toString());
        assertEquals("[0]", written.get("scenes").get(0).get("nodes").toString());
        assertEquals("second", written.get("nodes").get(2).get("name").asText());
    }

    @Test
    void emptySceneGetsMinimalDocument() throws Exception {
        Scene scene = new Scene();
        scene.add(new SceneNode("only"));

        JsonNode written = new GltfSceneWriter().write(scene);

        assertEquals("2.0", written.get("asset").get("version").asText());
        assertEquals(0, written.get("scene").asInt());
        assertEquals("[0]", written.get("scenes").get(0).get("nodes").toString());
        assertEquals("only", written.get("nodes").get(0).get("name").asText());
        assertFalse(written.get("nodes").get(0).has("translation"));
    }

    @Test
    void rejectsUnsupportedOrBrokenNodes() throws Exception {
        String nonUniform = """
            { "scenes": [ { "nodes": [0] } ], "nodes": [ { "scale": [1, 2, 1] } ] }""";
        String matrix = """
            { "scenes": [ { "nodes": [0] } ], "nodes": [ { "matrix": [1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1] } ] }""";
        String twoParents = """
            { "scenes": [ { "nodes": [0, 1] } ], "nodes": [ { "children": [2] }, { "children": [2] }, {} ] }""";
        String outOfRange = """
            { "scenes": [ { "nodes": [5] } ], "nodes": [ {} ] }""";

        for (String doc : new String[] { nonUniform, matrix, twoParents, outOfRange }) {
            assertThrows(
                SceneFormatException.class,
                () -> new GltfSceneReader().read(json(doc), registry()),
                doc
            );
        }
    }

    @Test
    void duplicateNodeIdsAreRejected() throws Exception {
        String doc = """
            { "scenes": [ { "nodes": [0, 1] } ], "nodes": [
              { "extras": { "kebab": { "id": "11111111-1111-1111-1111-111111111111" } } },
              { "extras": { "kebab": { "id": "11111111-1111-1111-1111-111111111111" } } } ] }""";

        assertThrows(SceneException.class, () -> new GltfSceneReader().read(json(doc), registry()));
    }
}
