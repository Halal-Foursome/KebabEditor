package org.halalfoursome.kebabeditor.archetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistryJson;
import org.halalfoursome.kebabeditor.logging.LogLevel;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ParamSpecJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void clearLog() {
        LogService.get().clear();
    }

    private ArchetypeDefinition load(String params) throws IOException {
        String json = "{\"archetypes\":[{\"id\":\"thing\",\"displayName\":\"Thing\",\"params\":" + params + "}]}";
        ArchetypeRegistry registry = mapper.readValue(json, ArchetypeRegistryJson.class).toRegistry();

        return registry.find("thing").unwrap();
    }

    private boolean warned(String fragment) {
        return LogService.get().entries().stream()
            .anyMatch(entry -> entry.level() == LogLevel.WARN && entry.message().contains(fragment));
    }

    @Test
    void theOlderTypeOnlyFormStillLoadsWithTheTypeDefault() throws Exception {
        ArchetypeDefinition thing = load("{\"size\":\"vec3\",\"count\":\"int\"}");

        assertEquals(ParamSpec.of(ParamType.VEC3), thing.getParams().get("size"));
        assertEquals(ParamSpec.of(ParamType.INT), thing.getParams().get("count"));
    }

    @Test
    void theObjectFormCarriesItsDefault() throws Exception {
        ArchetypeDefinition thing = load(
            "{\"size\":{\"type\":\"vec3\",\"default\":\"(1.0, 2.0, 3.0)\"},"
                + "\"radius\":{\"type\":\"float\",\"default\":0.5},"
                + "\"on\":{\"type\":\"bool\",\"default\":true},"
                + "\"label\":{\"type\":\"string\",\"default\":\"hi\"}}"
        );

        assertEquals(new Vec3(1, 2, 3), thing.getParams().get("size").defaultValue());
        assertEquals(0.5f, thing.getParams().get("radius").defaultValue());
        assertEquals(true, thing.getParams().get("on").defaultValue());
        assertEquals("hi", thing.getParams().get("label").defaultValue());
    }

    @Test
    void aMalformedDefaultFallsBackToTheTypeDefaultAndWarnsInTheLog() throws Exception {
        ArchetypeDefinition thing = load(
            "{\"size\":{\"type\":\"vec3\",\"default\":\"not a vector\"},"
                + "\"count\":{\"type\":\"int\",\"default\":\"seven\"},"
                + "\"fine\":{\"type\":\"int\",\"default\":7}}"
        );

        assertEquals(ParamSpec.of(ParamType.VEC3), thing.getParams().get("size"));
        assertEquals(ParamSpec.of(ParamType.INT), thing.getParams().get("count"));
        assertEquals(7, thing.getParams().get("fine").defaultValue());
        assertTrue(warned("'size'"), LogService.get().entries().toString());
        assertTrue(warned("'count'"), LogService.get().entries().toString());
    }

    @Test
    void anUnknownTypeStopsTheLoadBecauseThereIsNoSafeFallback() {
        assertThrows(IOException.class, () -> load("{\"size\":\"matrix\"}"));
        assertThrows(IOException.class, () -> load("{\"size\":{\"default\":1}}"));
        assertThrows(IOException.class, () -> load("{\"size\":5}"));
    }

    @Test
    void typeDefaultsAreWrittenInTheShortFormAndCustomOnesAsObjects() throws Exception {
        ArchetypeDefinition thing = new ArchetypeDefinition("thing")
            .withParam("plain", ParamType.VEC3)
            .withParam("custom", ParamType.VEC3, new Vec3(1, 1, 1))
            .withParam("radius", ParamType.FLOAT, 0.5f);

        JsonNode written = mapper.valueToTree(ArchetypeRegistryJson.from(registryOf(thing)));
        JsonNode params = written.get("archetypes").get(0).get("params");

        assertEquals("vec3", params.get("plain").textValue());
        assertEquals("vec3", params.get("custom").get("type").textValue());
        assertEquals("(1.0, 1.0, 1.0)", params.get("custom").get("default").textValue());
        assertEquals(0.5, params.get("radius").get("default").doubleValue(), 1e-6);
    }

    @Test
    void definitionsSurviveAWriteAndReadRoundTrip() throws Exception {
        ArchetypeDefinition thing = new ArchetypeDefinition("thing")
            .withDisplayName("Thing")
            .withParam("plain", ParamType.INT)
            .withParam("size", ParamType.VEC3, new Vec3(4, 5, 6))
            .withParam("name", ParamType.STRING, "crate")
            .withParam("path", ParamType.REL_PATH, "models/crate.gltf")
            .withParam("solid", ParamType.BOOL, true);

        String json = mapper.writeValueAsString(ArchetypeRegistryJson.from(registryOf(thing)));
        ArchetypeDefinition loaded = mapper.readValue(json, ArchetypeRegistryJson.class)
            .toRegistry()
            .find("thing")
            .unwrap();

        assertEquals(thing.getParams(), loaded.getParams());
    }

    private static ArchetypeRegistry registryOf(ArchetypeDefinition definition) {
        ArchetypeRegistry registry = new ArchetypeRegistry();
        registry.add(definition);

        return registry;
    }
}
