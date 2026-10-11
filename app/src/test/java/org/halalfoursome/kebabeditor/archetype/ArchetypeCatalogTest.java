package org.halalfoursome.kebabeditor.archetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstanceJson;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.archetype.instance.UnresolvedArchetype;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

class ArchetypeCatalogTest {

    private final ArchetypeRegistry registry = new ArchetypeRegistry();
    private final ArchetypeCatalog catalog = new ArchetypeCatalog(registry);

    @Test
    void findsBuiltinsAndProjectArchetypes() {
        ArchetypeDefinition mine = new ArchetypeDefinition("mine");
        registry.add(mine);

        assertSame(BuiltinArchetypes.find(BuiltinArchetypes.COLLIDER_BOX).unwrap(), catalog.find("collider_box").unwrap());
        assertSame(mine, catalog.find("mine").unwrap());
        assertTrue(catalog.find("nothing").isNone());
    }

    @Test
    void isBuiltinOnlyForTheBuiltInSet() {
        registry.add(new ArchetypeDefinition("mine"));

        assertTrue(catalog.isBuiltin(BuiltinArchetypes.TRIGGER));
        assertFalse(catalog.isBuiltin("mine"));
        assertFalse(catalog.isBuiltin("nothing"));
    }

    @Test
    void allListsBuiltinsAndProjectArchetypes() {
        ArchetypeDefinition mine = new ArchetypeDefinition("mine");
        registry.add(mine);

        assertTrue(catalog.all().containsAll(BuiltinArchetypes.all()));
        assertTrue(catalog.all().contains(mine));
        assertEquals(BuiltinArchetypes.all().size() + 1, catalog.all().size());
    }

    @Test
    void aProjectArchetypeShadowingABuiltinLosesInEveryLookup() {
        ArchetypeDefinition shadow = new ArchetypeDefinition(BuiltinArchetypes.COLLIDER_BOX);
        registry.add(shadow);
        ArchetypeDefinition builtin = BuiltinArchetypes.find(BuiltinArchetypes.COLLIDER_BOX).unwrap();

        assertSame(builtin, catalog.find("collider_box").unwrap());
        List<ArchetypeDefinition> boxes = catalog.all().stream()
            .filter(def -> def.getId().equals("collider_box"))
            .toList();
        assertEquals(1, boxes.size());
        assertSame(builtin, boxes.get(0));
    }

    @Test
    void aSavedBuiltinArchetypeLoadsAsARealInstanceNotUnresolved() throws Exception {
        ArchetypeInstanceJson json = new ArchetypeInstanceJson(
            "collider_box",
            java.util.Map.of("size", JsonNodeFactory.instance.textNode(new Vec3(1, 2, 3).toString()))
        );

        NodeArchetype loaded = json.toInstance(catalog);

        ArchetypeInstance instance = assertInstanceOf(ArchetypeInstance.class, loaded);
        assertEquals(new Vec3(1, 2, 3), instance.get("size"));
    }

    @Test
    void anUnknownIdStaysUnresolved() {
        NodeArchetype loaded = new ArchetypeInstanceJson("who_knows", java.util.Map.of()).toInstance(catalog);

        assertInstanceOf(UnresolvedArchetype.class, loaded);
    }
}
