package org.halalfoursome.kebabeditor.behavior.instance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.junit.jupiter.api.Test;

class BuiltinBehaviorsTest {

    @Test
    void unknownIdsHaveNoBehavior() {
        assertTrue(BuiltinBehaviors.behaviorOf("user_made").isNone());
    }

    @Test
    void everyBuiltinArchetypeHasABehavior() {
        for (var definition : BuiltinArchetypes.all()) {
            assertTrue(BuiltinBehaviors.behaviorOf(definition.getId()).isSome(), definition.getId());
        }
    }
}
