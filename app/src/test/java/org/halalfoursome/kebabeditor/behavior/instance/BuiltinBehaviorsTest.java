package org.halalfoursome.kebabeditor.behavior.instance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.junit.jupiter.api.Test;

class BuiltinBehaviorsTest {

    @Test
    void colliderAndTriggerArchetypesHaveBehaviors() {
        assertTrue(BuiltinBehaviors.behaviorOf(BuiltinArchetypes.COLLIDER_BOX).isSome());
        assertTrue(BuiltinBehaviors.behaviorOf(BuiltinArchetypes.COLLIDER_SPHERE).isSome());
        assertTrue(BuiltinBehaviors.behaviorOf(BuiltinArchetypes.TRIGGER).isSome());
    }

    @Test
    void unknownIdsHaveNoBehavior() {
        assertTrue(BuiltinBehaviors.behaviorOf("user_made").isNone());
    }

    @Test
    void everyBehaviorBelongsToABuiltinArchetype() {
        for (var definition : BuiltinArchetypes.all()) {
            // Archetypes without a behavior yet (model_ref, prefab_ref) are allowed; the reverse is not
            BuiltinBehaviors.behaviorOf(definition.getId());
        }

        assertTrue(BuiltinBehaviors.behaviorOf(BuiltinArchetypes.MODEL_REF).isNone());
    }
}
