package org.halalfoursome.kebabeditor.render;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.Meshes;

/** Turns a behavior's {@link GizmoShape} into concrete geometry in a node's space. */
final class SelectionShapes {

    /**
     * @param outline     lines to draw as the selection highlight
     * @param outlineWorld matrix placing {@code outline}
     * @param pickWorld   matrix placing the unit pick volume (cube, or sphere if {@code sphere})
     */
    record Placed(MeshData outline, Mat4 outlineWorld, Mat4 pickWorld, boolean sphere) {}

    private SelectionShapes() {}

    static Placed place(GizmoShape shape, Mat4 nodeWorld) {
        return switch (shape) {
            case GizmoShape.GizmoBox box -> unit(Meshes.CUBE_LINES, nodeWorld.mul(Mat4.scaling(box.size())), false);
            case GizmoShape.GizmoSphere sphere ->
                unit(Meshes.SPHERE_LINES, nodeWorld.mul(Mat4.scaling(2 * sphere.radius())), true);
            case GizmoShape.GizmoAabb aabb -> unit(Meshes.CUBE_LINES, nodeWorld.mul(boxMatrix(aabb.box())), false);
            case GizmoShape.GizmoLines lines ->
                new Placed(lines.lines(), nodeWorld, nodeWorld.mul(boxMatrix(lines.lines().bounds())), false);
        };
    }

    private static Placed unit(MeshData outline, Mat4 world, boolean sphere) {
        return new Placed(outline, world, world, sphere);
    }

    /** Maps the unit cube onto {@code box}. */
    static Mat4 boxMatrix(Aabb box) {
        Vec3 center = new Vec3(
            (box.getMinX() + box.getMaxX()) / 2,
            (box.getMinY() + box.getMaxY()) / 2,
            (box.getMinZ() + box.getMaxZ()) / 2
        );
        Vec3 size = new Vec3(
            box.getMaxX() - box.getMinX(),
            box.getMaxY() - box.getMinY(),
            box.getMaxZ() - box.getMinZ()
        );
        return Mat4.translation(center).scaleLocal(size);
    }
}
