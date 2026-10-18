package org.halalfoursome.kebabeditor.render;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Ray;
import org.halalfoursome.kebabeditor.math.Vec3;

import lombok.Getter;
import lombok.Setter;

/**
 * Free-flight perspective camera.
 *
 * Yaw rotates around +Y and pitch tilts up/down; yaw = 0, pitch = 0 looks down -Z,
 * matching the OpenGL view-space convention.
 */
@Getter
public class Camera {

    private static final float PITCH_LIMIT = (float) Math.toRadians(89);

    private final Vec3 position = new Vec3(0, 6, 12);
    private float yaw = 0f;
    private float pitch = (float) Math.toRadians(-25);

    @Setter
    private float fovY = (float) Math.toRadians(60);
    @Setter
    private float near = 0.05f;
    @Setter
    private float far = 2000f;

    public void rotate(float deltaYaw, float deltaPitch) {
        yaw += deltaYaw;
        pitch = Math.clamp(pitch + deltaPitch, -PITCH_LIMIT, PITCH_LIMIT);
    }

    public void move(Vec3 delta) {
        position.addLocal(delta);
    }

    public Vec3 forward() {
        float cosPitch = (float) Math.cos(pitch);
        return new Vec3(
            (float) -Math.sin(yaw) * cosPitch,
            (float) Math.sin(pitch),
            (float) -Math.cos(yaw) * cosPitch
        );
    }

    // Always horizontal, so strafing never changes height.
    public Vec3 right() {
        return new Vec3((float) Math.cos(yaw), 0, (float) -Math.sin(yaw));
    }

    public Vec3 up() {
        return right().crossLocal(forward());
    }

    /**
     * The ray from the eye through a point on screen.
     *
     * @param ndcX -1 at the left edge, +1 at the right
     * @param ndcY -1 at the bottom edge, +1 at the top
     */
    public Ray ray(float ndcX, float ndcY, float aspect) {
        float tanHalf = (float) Math.tan(fovY / 2);
        Vec3 direction = forward()
            .addLocal(right().mulLocal(ndcX * tanHalf * aspect))
            .addLocal(up().mulLocal(ndcY * tanHalf));
        return new Ray(position, direction);
    }

    public Mat4 view() {
        return Mat4.lookAt(position, position.add(forward()), Vec3.unitY());
    }

    public Mat4 projection(float aspect) {
        return Mat4.perspective(fovY, aspect, near, far);
    }
}
