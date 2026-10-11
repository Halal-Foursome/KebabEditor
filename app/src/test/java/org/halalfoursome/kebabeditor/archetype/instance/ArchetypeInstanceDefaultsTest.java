package org.halalfoursome.kebabeditor.archetype.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

class ArchetypeInstanceDefaultsTest {

    private final ArchetypeRegistry registry = new ArchetypeRegistry();
    private final ArchetypeCatalog catalog = new ArchetypeCatalog(registry);

    private ArchetypeInstance load(String id, Map<String, JsonNode> params) {
        return assertInstanceOf(ArchetypeInstance.class, new ArchetypeInstanceJson(id, params).toInstance(catalog));
    }

    @Test
    void instancesNeverShareOneMutableDefault() throws Exception {
        ArchetypeDefinition definition = BuiltinArchetypes.find(BuiltinArchetypes.COLLIDER_BOX).unwrap();
        ArchetypeInstance first = new ArchetypeInstance(definition);
        ArchetypeInstance second = new ArchetypeInstance(definition);

        ((Vec3) first.get("size")).setX(50);

        assertNotSame(first.get("size"), second.get("size"));
        assertEquals(Vec3.one(), second.get("size"));
        assertEquals(Vec3.one(), new ArchetypeInstance(definition).get("size"));
    }

    @Test
    void aSceneThatOmitsAParamGetsTheDefinitionsDefault() throws Exception {
        ArchetypeInstance box = load(BuiltinArchetypes.COLLIDER_BOX, Map.of());

        assertEquals(Vec3.one(), box.get("size"));
    }

    @Test
    void aParamAddedLaterGivesOldScenesItsConfiguredDefault() throws Exception {
        registry.add(new ArchetypeDefinition("enemy")
            .withParam("name", ParamType.STRING)
            .withParam("health", ParamType.INT, 100));

        // This scene was saved before "health" existed
        ArchetypeInstance enemy = load(
            "enemy",
            Map.of("name", JsonNodeFactory.instance.textNode("orc"))
        );

        assertEquals("orc", enemy.get("name"));
        assertEquals(100, enemy.get("health"));
    }

    @Test
    void savedInstancesKeepTheirValuesWhenTheDefaultLaterChanges() throws Exception {
        registry.add(new ArchetypeDefinition("enemy").withParam("health", ParamType.INT, 100));
        ArchetypeInstanceJson saved = ArchetypeInstanceJson.from(
            new ArchetypeInstance(registry.find("enemy").unwrap())
        );

        registry.remove(registry.find("enemy").unwrap());
        registry.add(new ArchetypeDefinition("enemy").withParam("health", ParamType.INT, 250));
        ArchetypeInstance reloaded = assertInstanceOf(ArchetypeInstance.class, saved.toInstance(catalog));

        assertEquals(100, reloaded.get("health"));
    }
}
