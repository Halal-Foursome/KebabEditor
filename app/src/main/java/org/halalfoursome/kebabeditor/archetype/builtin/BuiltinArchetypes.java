package org.halalfoursome.kebabeditor.archetype.builtin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

public final class BuiltinArchetypes {

    public static final String PREFAB_REF = "prefab_ref";
    public static final String MODEL_REF = "model_ref";
    public static final String COLLIDER_BOX = "collider_box";
    public static final String COLLIDER_SPHERE = "collider_sphere";
    public static final String TRIGGER = "trigger";

    // TODO: add icons for builtin archetypes
    private static final Map<String, ArchetypeDefinition> ALL = index(
        new ArchetypeDefinition(PREFAB_REF)
            .withDisplayName("Prefab Reference")
            .withParam("path", ParamType.REL_PATH),

        new ArchetypeDefinition(MODEL_REF)
            .withDisplayName("Model Reference")
            .withParam("path", ParamType.REL_PATH),

        new ArchetypeDefinition(COLLIDER_BOX)
            .withDisplayName("Box Collider")
            .withParam("size", ParamType.VEC3, Vec3.one()),

        new ArchetypeDefinition(COLLIDER_SPHERE)
            .withDisplayName("Sphere Collider")
            .withParam("radius", ParamType.FLOAT, 0.5f),

        new ArchetypeDefinition(TRIGGER)
            .withDisplayName("Trigger")
            .withParam("size", ParamType.VEC3, Vec3.one())
    );

    private BuiltinArchetypes() {}

    public static Map<String, ArchetypeDefinition> index(ArchetypeDefinition... defs) {
        Map<String, ArchetypeDefinition> map = new HashMap<>();
        for (ArchetypeDefinition def : defs) {
            map.put(def.getId(), def);
        }
        return map;
    }

    public static Collection<ArchetypeDefinition> all() {
        return ALL.values();
    }

    public static Maybe<ArchetypeDefinition> find(String id) {
        return Maybe.ofNullable(ALL.get(id));
    }
}
