package org.halalfoursome.kebabeditor.archetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstanceJson;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Map;

class ParamTypeTest {

    @Test
    void everyDefaultValueIsAnInstanceOfTheTypesJavaClass() {
        for (ParamType type : ParamType.values()) {
            assertTrue(
                type.javaClass().isInstance(type.defaultValue()),
                type + " defaults to " + type.defaultValue().getClass() + " but declares " + type.javaClass()
            );
        }
    }

    @Test
    void aRelPathSurvivesALoadAndSaveRoundTrip() throws Exception {
        ArchetypeCatalog catalog = new ArchetypeCatalog(new org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry());
        ArchetypeInstanceJson saved = new ArchetypeInstanceJson(
            BuiltinArchetypes.MODEL_REF,
            Map.of("path", JsonNodeFactory.instance.textNode("models/crate.gltf"))
        );

        NodeArchetype loaded = saved.toInstance(catalog);

        ArchetypeInstance instance = org.junit.jupiter.api.Assertions.assertInstanceOf(ArchetypeInstance.class, loaded);
        assertEquals("models/crate.gltf", instance.get("path"));
        assertEquals("models/crate.gltf", ArchetypeInstanceJson.from(instance).params().get("path").textValue());
    }
}
