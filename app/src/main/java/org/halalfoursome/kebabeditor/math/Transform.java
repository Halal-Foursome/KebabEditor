package org.halalfoursome.kebabeditor.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transform {

    private Vec3 translation;
    private Quat rotation;
    private float scale;

    public static Transform identity() {
        return new Transform(Vec3.zero(), Quat.identity(), 1f);
    }

    public Transform copy() {
        return new Transform(translation.copy(), rotation.copy(), scale);
    }

    public Transform set(Transform o) {
        translation.set(o.translation);
        rotation.set(o.rotation);
        scale = o.scale;
        return this;
    }

    public Transform setIdentity() {
        translation.set(0, 0, 0);
        rotation.setIdentity();
        scale = 1f;
        return this;
    }

    public Vec3 applyLocal(Vec3 point) {
        return rotation.rotateLocal(point.mulLocal(scale)).addLocal(translation);
    }

    public Transform mulLocal(Transform child) {
        float tx = translation.getX();
        float ty = translation.getY();
        float tz = translation.getZ();
        translation.set(child.translation).mulLocal(scale);
        rotation.rotateLocal(translation).addLocal(tx, ty, tz);
        rotation.mulLocal(child.rotation).normalizeLocal();
        scale *= child.scale;
        return this;
    }

    public Transform inverseLocal() {
        float invScale = 1f / scale;
        rotation.inverseLocal();
        rotation.rotateLocal(translation).mulLocal(-invScale);
        scale = invScale;
        return this;
    }

    public Transform lerpLocal(Transform o, float t) {
        translation.lerpLocal(o.translation, t);
        rotation.slerpLocal(o.rotation, t);
        scale += (o.scale - scale) * t;
        return this;
    }

    public Mat4 toMat4() {
        return new Mat4().setCompose(translation, rotation, scale);
    }

    public Mat4 toMat4(Mat4 dest) {
        return dest.setCompose(translation, rotation, scale);
    }

    public Vec3 apply(Vec3 point) {
        return applyLocal(point.copy());
    }

    public Transform mul(Transform child) {
        return copy().mulLocal(child);
    }

    public Transform inverse() {
        return copy().inverseLocal();
    }

    public Transform lerp(Transform o, float t) {
        return copy().lerpLocal(o, t);
    }
}
