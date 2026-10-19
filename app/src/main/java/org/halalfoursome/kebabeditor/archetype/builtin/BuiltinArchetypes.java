package org.halalfoursome.kebabeditor.archetype.builtin;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon;
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
    public static final String POINT_LIGHT = "point_light";
    public static final String DIR_LIGHT = "dir_light";
    public static final String SPOT_LIGHT = "spot_light";
    public static final String SNAP_DISTANCE = "snap_distance";

    /** Snap step used when a scene has no snap_distance object. */
    public static final float DEFAULT_SNAP_DISTANCE = 0.5f;

    private static final Map<String, ArchetypeDefinition> ALL = index(
        new ArchetypeDefinition(PREFAB_REF)
            .withDisplayName("Prefab Reference")
            .withSingle()
            .withParam("path", ParamType.REL_PATH),

        new ArchetypeDefinition(MODEL_REF)
            .withDisplayName("Model Reference")
            .withSingle()
            .withParam("path", ParamType.REL_PATH),

        new ArchetypeDefinition(COLLIDER_BOX)
            .withDisplayName("Box Collider")
            .withIcon(new ArchetypeIcon.Bundled("archetypes/cube.svg"))
            .withParam("size", ParamType.VEC3, Vec3.one()),

        new ArchetypeDefinition(COLLIDER_SPHERE)
            .withDisplayName("Sphere Collider")
            .withIcon(new ArchetypeIcon.Bundled("archetypes/sphere.svg"))
            .withParam("radius", ParamType.FLOAT, 0.5f),

        new ArchetypeDefinition(TRIGGER)
            .withDisplayName("Trigger")
            .withParam("size", ParamType.VEC3, Vec3.one()),

        new ArchetypeDefinition(POINT_LIGHT)
            .withDisplayName("Point Light")
            .withIcon(new ArchetypeIcon.Bundled("archetypes/light.svg"))
            .withSingle()
            .withParam("color",     ParamType.COLOR, Color.WHITE)
            .withParam("intensity", ParamType.FLOAT, 1000.0f)
            .withParam("range",     ParamType.FLOAT, 10.0f)
            .withParam("shadows",   ParamType.BOOL,  false),

        new ArchetypeDefinition(DIR_LIGHT)
            .withDisplayName("Directional Light")
            .withIcon(new ArchetypeIcon.Bundled("archetypes/sun.svg"))
            .withSingle()
            .withParam("color",     ParamType.COLOR, Color.WHITE)
            .withParam("intensity", ParamType.FLOAT, 1.0f)
            .withParam("shadows",   ParamType.BOOL,  false),

        new ArchetypeDefinition(SPOT_LIGHT)
            .withDisplayName("Spot Light")
            .withIcon(new ArchetypeIcon.Bundled("archetypes/spotlight.svg"))
            .withSingle()
            .withParam("color",       ParamType.COLOR, Color.WHITE)
            .withParam("intensity",   ParamType.FLOAT, 1000.0f)
            .withParam("range",       ParamType.FLOAT, 10.0f)
            .withParam("inner_angle", ParamType.FLOAT, 25.0f)
            .withParam("outer_angle", ParamType.FLOAT, 35.0f)
            .withParam("shadows",     ParamType.BOOL,  false),

        // TODO: replace with "kebab-project.json" project settings
        // Placed in a level, its distance is the grid step for snapping in the viewport
        new ArchetypeDefinition(SNAP_DISTANCE)
            .withDisplayName("Snap Distance")
            .withSingle()
            .withParam("distance", ParamType.FLOAT, DEFAULT_SNAP_DISTANCE)
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
