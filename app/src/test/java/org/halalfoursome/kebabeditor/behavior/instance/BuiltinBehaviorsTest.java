package org.halalfoursome.kebabeditor.behavior.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.junit.jupiter.api.Test;

class BuiltinBehaviorsTest {

    @Test
    void modelColliderAndTriggerArchetypesHaveBehaviors() {
        assertTrue(BuiltinBehaviors.behaviorOf(BuiltinArchetypes.MODEL_REF).isSome());
        assertTrue(BuiltinBehaviors.behaviorOf(BuiltinArchetypes.COLLIDER_BOX).isSome());
        assertTrue(BuiltinBehaviors.behaviorOf(BuiltinArchetypes.COLLIDER_SPHERE).isSome());
        assertTrue(BuiltinBehaviors.behaviorOf(BuiltinArchetypes.TRIGGER).isSome());
    }

    @Test
    void unknownIdsHaveNoBehavior() {
        assertTrue(BuiltinBehaviors.behaviorOf("user_made").isNone());
    }

    // prefab_ref is defined but its behaviour arrives in its own change
    @Test
    void everyBuiltinArchetypeExceptPrefabRefHasABehavior() {
        for (var definition : BuiltinArchetypes.all()) {
            boolean hasBehavior = BuiltinBehaviors.behaviorOf(definition.getId()).isSome();

            assertEquals(
                !definition.getId().equals(BuiltinArchetypes.PREFAB_REF),
                hasBehavior,
                definition.getId()
            );
        }
    }
}
