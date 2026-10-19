package org.halalfoursome.kebabeditor.render;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;

/**
 * Where the gizmo sits for a node this frame. Drawing and hit-testing both use
 * it, so what is drawn is exactly what can be grabbed.
 *
 * @param center   the node's world position
 * @param rotation world axes for translate; the node's world rotation for rotate and scale
 * @param size     world length of a handle, keeping it a constant size on screen
 */
public record GizmoFrame(Vec3 center, Quat rotation, float size) {

    /** Handle length as a fraction of the viewport's half-height. */
    static final float SCREEN_FRACTION = 0.36f;

    public static GizmoFrame of(SceneNode node, GizmoMode mode, Camera camera) {
        Mat4 world = node.worldMatrix();
        Vec3 center = world.getTranslation();
        Quat rotation = mode == GizmoMode.TRANSLATE ? Quat.identity() : Quat.fromMatrix(world);
        return new GizmoFrame(center, rotation, handleScale(camera, center));
    }

    /**
     * World length that appears the same on screen at any distance. Uses view
     * depth rather than distance, so the size doesn't change towards the edges.
     */
    static float handleScale(Camera camera, Vec3 position) {
        float depth = position.sub(camera.getPosition()).dot(camera.forward());
        float halfHeight = Math.max(depth, camera.getNear()) * (float) Math.tan(camera.getFovY() / 2);
        return halfHeight * SCREEN_FRACTION;
    }

    /** Unit world direction of axis 0, 1 or 2. */
    public Vec3 axis(int index) {
        Vec3 unit = switch (index) {
            case 0 -> Vec3.unitX();
            case 1 -> Vec3.unitY();
            default -> Vec3.unitZ();
        };
        return rotation.rotate(unit);
    }

    /** Places geometry built around the origin with unit handle length. */
    public Mat4 matrix() {
        return Mat4.compose(center, rotation, new Vec3(size, size, size));
    }
}
