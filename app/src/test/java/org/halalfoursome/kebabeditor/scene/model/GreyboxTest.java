package org.halalfoursome.kebabeditor.scene.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class GreyboxTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void roundTripsThroughNodeExtras() {
        SceneNode node = new SceneNode("platform");
        Greybox written = new Greybox(GreyboxShape.BOX, new Vec3(4, 0.5f, 2), new Color(200, 100, 50, 128));

        written.writeTo(node);
        Greybox read = Greybox.of(node).unwrap();

        assertEquals(GreyboxShape.BOX, read.shape());
        assertTrue(new Vec3(4, 0.5f, 2).approxEquals(read.size(), 1e-6f));
        assertEquals(written.color(), read.color());
    }

    @Test
    void survivesSaveAndLoad() throws Exception {
        Scene scene = new Scene();
        SceneNode node = new SceneNode("ball");
        Greybox.unit(GreyboxShape.SPHERE).writeTo(node);
        scene.add(node);

        String json = MAPPER.writeValueAsString(new GltfSceneWriter().write(scene));
        Scene loaded = new GltfSceneReader().read(MAPPER.readTree(json), new ArchetypeCatalog(new ArchetypeRegistry()));

        assertEquals(GreyboxShape.SPHERE, Greybox.of(loaded.getRootNodes().getFirst()).unwrap().shape());
    }

    @Test
    void nodesWithoutGreyboxOrWithMalformedDataHaveNone() throws Exception {
        assertTrue(Greybox.of(new SceneNode("plain")).isNone());

        SceneNode unknownShape = node("{ \"extras\": { \"kebab\": { \"greybox\": { \"shape\": \"cone\" } } } }");
        SceneNode badSize = node("{ \"extras\": { \"kebab\": { \"greybox\": { \"shape\": \"box\", \"size\": [1, 2] } } } }");

        assertTrue(Greybox.of(unknownShape).isNone());
        assertTrue(Greybox.of(badSize).isNone());
    }

    @Test
    void missingSizeAndColorFallBackToDefaults() throws Exception {
        Greybox greybox = Greybox.of(node("{ \"extras\": { \"kebab\": { \"greybox\": { \"shape\": \"box\" } } } }")).unwrap();

        assertTrue(Vec3.one().approxEquals(greybox.size(), 0f));
        assertEquals(Greybox.DEFAULT_COLOR, greybox.color());
    }

    private static SceneNode node(String rawJson) throws Exception {
        SceneNode node = new SceneNode("n");
        node.setRawJson(MAPPER.readTree(rawJson));
        return node;
    }
}
