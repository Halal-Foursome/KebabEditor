package org.halalfoursome.kebabeditor.behavior.instance;

import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.behavior.ArchetypeBehavior;
import org.halalfoursome.kebabeditor.utils.Maybe;

public final class BuiltinBehaviors {

    // TODO: add model_ref and prefab_ref
    private static final Map<String, ArchetypeBehavior> ALL = Map.of(
        BuiltinArchetypes.COLLIDER_BOX,     new ColliderBoxBehavior(),
        BuiltinArchetypes.COLLIDER_SPHERE,  new ColliderSphereBehavior(),
        BuiltinArchetypes.TRIGGER,          new TriggerBehavior()
    );

    private BuiltinBehaviors() {}

    public static Maybe<ArchetypeBehavior> behaviorOf(String archetypeId) {
        return Maybe.ofNullable(ALL.get(archetypeId));
    }
}
