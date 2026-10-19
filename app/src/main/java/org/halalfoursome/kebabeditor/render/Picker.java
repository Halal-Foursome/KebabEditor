package org.halalfoursome.kebabeditor.render;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Ray;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.PrimitiveType;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Finds an object a ray hits first. Each candidate is tested in its own local space
 */
public final class Picker {

    private static final float EPSILON = 1e-6f;
    private static final Aabb UNIT_BOX = centeredCube(0.5f);

    // MeshData returns copies, so keep one per mesh while it is alive.
    private final Map<MeshData, float[]> positions = new WeakHashMap<>();
    private final Map<MeshData, int[]> indices = new WeakHashMap<>();

    public Maybe<PickHit> pick(Scene scene, Maybe<BehaviorContext> behaviors, Ray ray, boolean ground) {
        Nearest nearest = new Nearest();

        scene.traverse().forEach(node ->
            Greybox.of(node).ifSome(greybox -> pickGreybox(node, greybox, ray, nearest))
        );

        behaviors.ifSome(context -> SceneArchetypes.of(scene).forEach(entry -> {
            List<Drawable> solids = entry.behavior().drawableObjects(entry.node(), entry.archetype(), context).stream()
                .filter(d -> d.mesh().type() == PrimitiveType.TRIANGLES)
                .toList();

            if (!solids.isEmpty()) {
                solids.forEach(d -> pickTriangles(entry.node(), d, ray, nearest));
                return;
            }

            entry.behavior().gizmoSelectionShape(entry.node(), entry.archetype(), context).ifSome(shape -> {
                SelectionShapes.Placed placed = SelectionShapes.place(shape, entry.node().worldMatrix());
                pickLocal(entry.node(), placed.pickWorld(), ray, nearest,
                    local -> placed.sphere() ? sphere(local) : box(local, UNIT_BOX));
            });
        }));

        Vec3 o = ray.origin();
        Vec3 d = ray.direction();
        if (ground && Math.abs(d.getY()) > EPSILON) {
            float t = -o.getY() / d.getY();
            if (t > 0 && t < nearest.distance) {
                Vec3 normal = new Vec3(0, o.getY() >= 0 ? 1 : -1, 0);
                nearest.hit = Maybe.some(new PickHit(Maybe.none(), t, ray.point(t), normal));
                nearest.distance = t;
            }
        }

        return nearest.hit;
    }

    private void pickGreybox(SceneNode node, Greybox greybox, Ray ray, Nearest nearest) {
        Mat4 sized = node.worldMatrix().mul(Mat4.scaling(greybox.size()));
        pickLocal(node, sized, ray, nearest, local -> greybox.shape() == GreyboxShape.SPHERE
            ? sphere(local)
            : box(local, UNIT_BOX));
    }

    private void pickTriangles(SceneNode node, Drawable drawable, Ray ray, Nearest nearest) {
        // Cheap rejection against the mesh's box before testing triangles.
        Maybe<float[]> bounds = local(drawable.world(), ray).andThen(local -> box(local, drawable.mesh().bounds(), true));
        if (bounds.isNone() || bounds.unwrap()[0] >= nearest.distance) {
            return;
        }

        pickLocal(node, drawable.world(), ray, nearest, local -> triangles(local, drawable.mesh()));
    }

    private record LocalRay(Vec3 origin, Vec3 direction, Mat4 inverse) {}

    private interface LocalTest {

        Maybe<float[]> hit(LocalRay ray);
    }

    private static Maybe<LocalRay> local(Mat4 world, Ray ray) {
        try {
            Mat4 inverse = world.inverse();
            return Maybe.some(new LocalRay(
                inverse.transformPoint(ray.origin()),
                inverse.transformDirection(ray.direction()),
                inverse
            ));
        } catch (ArithmeticException e) {
            return Maybe.none(); // zero-size object
        }
    }

    private static void pickLocal(SceneNode node, Mat4 world, Ray ray, Nearest nearest, LocalTest test) {
        Maybe<LocalRay> local = local(world, ray);
        if (local.isNone()) {
            return;
        }

        Maybe<float[]> hit = test.hit(local.unwrap());
        if (hit.isNone() || hit.unwrap()[0] >= nearest.distance) {
            return;
        }

        float[] h = hit.unwrap();
        // Normals go back to world space by the inverse-transpose.
        Vec3 normal = local.unwrap().inverse().transpose()
            .transformDirection(new Vec3(h[1], h[2], h[3]))
            .normalizeLocal();
        if (normal.dot(ray.direction()) > 0) {
            normal.negateLocal();
        }

        nearest.distance = h[0];
        nearest.hit = Maybe.some(new PickHit(Maybe.some(node), h[0], ray.point(h[0]), normal));
    }

    // Slab test. Rays starting inside the box don't hit it, so you can't pick what encloses the camera.
    private static Maybe<float[]> box(LocalRay ray, Aabb box) {
        return box(ray, box, false);
    }

    /** @param insideHits a ray starting inside counts as hitting at t = 0 (for rejection tests) */
    private static Maybe<float[]> box(LocalRay ray, Aabb box, boolean insideHits) {
        float[] o = ray.origin().toArray();
        float[] d = ray.direction().toArray();
        float[] min = { box.getMinX(), box.getMinY(), box.getMinZ() };
        float[] max = { box.getMaxX(), box.getMaxY(), box.getMaxZ() };

        float near = Float.NEGATIVE_INFINITY;
        float far = Float.POSITIVE_INFINITY;
        int nearAxis = -1;

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(d[axis]) < EPSILON) {
                if (o[axis] < min[axis] || o[axis] > max[axis]) {
                    return Maybe.none();
                }
                continue;
            }
            float t1 = (min[axis] - o[axis]) / d[axis];
            float t2 = (max[axis] - o[axis]) / d[axis];
            if (t1 > t2) {
                float swap = t1;
                t1 = t2;
                t2 = swap;
            }
            if (t1 > near) {
                near = t1;
                nearAxis = axis;
            }
            far = Math.min(far, t2);
            if (near > far) {
                return Maybe.none();
            }
        }

        if (near < 0 || nearAxis < 0) {
            return insideHits && far >= 0 ? Maybe.some(new float[] { 0, 0, 1, 0 }) : Maybe.none();
        }

        float[] hit = { near, 0, 0, 0 };
        hit[1 + nearAxis] = d[nearAxis] > 0 ? -1 : 1;
        return Maybe.some(hit);
    }

    // Sphere of radius 0.5 at the origin (the unit greybox sphere).
    private static Maybe<float[]> sphere(LocalRay ray) {
        Vec3 o = ray.origin();
        Vec3 d = ray.direction();
        float a = d.dot(d);
        float b = 2 * o.dot(d);
        float c = o.dot(o) - 0.25f;
        float discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return Maybe.none();
        }

        float t = (-b - (float) Math.sqrt(discriminant)) / (2 * a);
        if (t < 0) {
            return Maybe.none();
        }

        Vec3 p = o.add(d.mul(t));
        return Maybe.some(new float[] { t, p.getX(), p.getY(), p.getZ() });
    }

    // Moeller–Trumbore intersection algorithm against every triangle
    private Maybe<float[]> triangles(LocalRay ray, MeshData mesh) {
        float[] p = positions.computeIfAbsent(mesh, MeshData::positions);
        int[] idx = indices.computeIfAbsent(mesh, MeshData::indices);
        Vec3 o = ray.origin();
        Vec3 d = ray.direction();

        float best = Float.POSITIVE_INFINITY;
        Vec3 bestNormal = null;

        for (int i = 0; i + 2 < idx.length; i += 3) {
            Vec3 v0 = vertex(p, idx[i]);
            Vec3 e1 = vertex(p, idx[i + 1]).subLocal(v0);
            Vec3 e2 = vertex(p, idx[i + 2]).subLocal(v0);

            Vec3 h = d.cross(e2);
            float det = e1.dot(h);
            if (Math.abs(det) < EPSILON) {
                continue;
            }
            float inv = 1f / det;
            Vec3 s = o.sub(v0);
            float u = s.dot(h) * inv;
            if (u < 0 || u > 1) {
                continue;
            }
            Vec3 q = s.crossLocal(e1);
            float v = d.dot(q) * inv;
            if (v < 0 || u + v > 1) {
                continue;
            }
            float t = e2.dot(q) * inv;
            if (t > EPSILON && t < best) {
                best = t;
                bestNormal = e1.cross(e2);
            }
        }

        return bestNormal == null
            ? Maybe.none()
            : Maybe.some(new float[] { best, bestNormal.getX(), bestNormal.getY(), bestNormal.getZ() });
    }

    private static Vec3 vertex(float[] p, int index) {
        return new Vec3(p[index * 3], p[index * 3 + 1], p[index * 3 + 2]);
    }

    private static Aabb centeredCube(float half) {
        Aabb box = new Aabb();
        box.setMinX(-half);
        box.setMinY(-half);
        box.setMinZ(-half);
        box.setMaxX(half);
        box.setMaxY(half);
        box.setMaxZ(half);
        return box;
    }

    private static final class Nearest {
        float distance = Float.POSITIVE_INFINITY;
        Maybe<PickHit> hit = Maybe.none();
    }
}
