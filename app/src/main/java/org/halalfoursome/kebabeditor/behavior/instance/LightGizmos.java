package org.halalfoursome.kebabeditor.behavior.instance;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.BehaviorProblem;
import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.behavior.PlainBehaviorProblem;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.halalfoursome.kebabeditor.utils.Params;

public final class LightGizmos {

    public static final String COLOR = "color";
    public static final String INTENSITY = "intensity";
    public static final String RANGE = "range";
    public static final String INNER_ANGLE = "inner_angle";
    public static final String OUTER_ANGLE = "outer_angle";

    public static final float MARKER_DIAMETER = 0.4f;
    public static final float ARROW_LENGTH = 1.5f;

    private static final int GIZMO_ALPHA = 200;
    private static final int INNER_CONE_ALPHA = 110;
    private static final float MAX_HALF_ANGLE = 90f;

    private LightGizmos() {}

    public static Drawable marker(Mat4 world, Color color) {
        return lines(Meshes.SPHERE_LINES, world.mul(Mat4.scaling(MARKER_DIAMETER)), color, GIZMO_ALPHA);
    }

    public static Drawable rangeSphere(Mat4 world, Color color, float range) {
        return lines(Meshes.SPHERE_LINES, world.mul(Mat4.scaling(2f * range)), color, INNER_CONE_ALPHA);
    }

    public static Drawable arrow(Mat4 world, Color color) {
        return lines(Meshes.ARROW_LINES, world.mul(Mat4.scaling(ARROW_LENGTH)), color, GIZMO_ALPHA);
    }

    public static Drawable cone(Mat4 world, Color color, float length, float halfAngleDegrees, boolean inner) {
        float radius = length * (float) Math.tan(Math.toRadians(halfAngleDegrees));

        return lines(
            Meshes.CONE_LINES,
            world.mul(Mat4.scaling(new Vec3(radius, radius, length))),
            color,
            inner ? INNER_CONE_ALPHA : GIZMO_ALPHA
        );
    }

    public static Maybe<Aabb> boundsOf(List<Drawable> gizmos) {
        Aabb union = null;

        for (Drawable gizmo : gizmos) {
            Aabb box = gizmo.mesh().bounds().transformed(gizmo.world());
            union = union == null ? box : union.union(box);
        }

        return Maybe.ofNullable(union);
    }

    public static Color colorOf(ArchetypeInstance archetype) {
        return Params.color(archetype, COLOR).unwrapOr(Color.WHITE);
    }

    public static Maybe<Float> validRange(ArchetypeInstance archetype) {
        return Params.number(archetype, RANGE).filter(Params::positive);
    }

    public static Maybe<Float> validOuterAngle(ArchetypeInstance archetype) {
        return Params.number(archetype, OUTER_ANGLE)
            .filter(angle -> Float.isFinite(angle) && angle > 0f && angle < MAX_HALF_ANGLE);
    }

    public static Maybe<Float> validInnerAngle(ArchetypeInstance archetype) {
        return validOuterAngle(archetype).andThen(outer ->
            Params.number(archetype, INNER_ANGLE)
                .filter(angle -> Float.isFinite(angle) && angle >= 0f && angle <= outer)
        );
    }

    public static List<BehaviorProblem> validateIntensity(String label, ArchetypeInstance archetype) {
        Maybe<Float> intensity = Params.number(archetype, INTENSITY);

        if (intensity.isNone()) {
            return List.of(problem(label + ": 'intensity' is missing or not a Float"));
        }

        float value = intensity.unwrap();

        if (!Float.isFinite(value) || value < 0f) {
            return List.of(problem(label + ": intensity must not be negative, got " + value));
        }

        return List.of();
    }

    public static List<BehaviorProblem> validateRange(String label, ArchetypeInstance archetype) {
        Maybe<Float> range = Params.number(archetype, RANGE);

        if (range.isNone()) {
            return List.of(problem(label + ": 'range' is missing or not a Float"));
        }
        if (validRange(archetype).isNone()) {
            return List.of(problem(label + ": range must be positive, got " + range.unwrap()));
        }

        return List.of();
    }

    public static List<BehaviorProblem> validateAngles(String label, ArchetypeInstance archetype) {
        List<BehaviorProblem> problems = new ArrayList<>();
        Maybe<Float> outer = Params.number(archetype, OUTER_ANGLE);
        Maybe<Float> inner = Params.number(archetype, INNER_ANGLE);

        if (outer.isNone() || inner.isNone()) {
            problems.add(problem(label + ": 'inner_angle' and 'outer_angle' must be Floats"));

            return problems;
        }
        if (validOuterAngle(archetype).isNone()) {
            problems.add(problem(label + ": outer_angle must be above 0 and below 90 degrees, got " + outer.unwrap()));
        } else if (validInnerAngle(archetype).isNone()) {
            problems.add(problem(
                label + ": inner_angle must be between 0 and outer_angle (" + outer.unwrap() + "), got " + inner.unwrap()
            ));
        }

        return problems;
    }

    private static Drawable lines(
        MeshData mesh,
        Mat4 world,
        Color color,
        int alpha
    ) {
        Color shown = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        return new Drawable(mesh, world, Material.solid(shown), DrawMode.OVERLAY);
    }

    private static BehaviorProblem problem(String message) {
        return new PlainBehaviorProblem(message);
    }
}
