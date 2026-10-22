package org.halalfoursome.kebabeditor.playtest.physics;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * A sphere stretched along its own axes: the unit sphere (radius 0.5) under a
 * node's world matrix. A plain sphere is the case where all semi-axes match.
 */
public final class Ellipsoid implements ConvexShape {

    private static final int BISECTION_STEPS = 64;
    private static final float SPHERE_TOLERANCE = 1e-4f;

    private final ShapeFrame frame;
    private final Aabb bounds;
    private final boolean sphere;

    private Ellipsoid(ShapeFrame frame) {
        this.frame = frame;
        this.bounds = frame.ellipsoidBounds();

        float[] a = frame.half;
        this.sphere = Math.abs(a[0] - a[1]) <= SPHERE_TOLERANCE * a[0]
            && Math.abs(a[0] - a[2]) <= SPHERE_TOLERANCE * a[0];
    }

    /** The sphere of radius 0.5 under {@code world}; none when the matrix flattens it. */
    public static Maybe<Ellipsoid> of(Mat4 world) {
        return ShapeFrame.of(world).map(Ellipsoid::new);
    }

    public Vec3 center() {
        return frame.center.copy();
    }

    public Vec3 semiAxes() {
        return new Vec3(frame.half[0], frame.half[1], frame.half[2]);
    }

    @Override
    public Vec3 closestPoint(Vec3 p) {
        float[] local = frame.toLocal(p);
        float[] a = frame.half;

        if (implicit(local) <= 1f) {
            return p.copy();
        }
        if (sphere) {
            Vec3 offset = p.sub(frame.center);
            return frame.center.add(offset.mulLocal(a[0] / offset.length()));
        }

        return frame.toWorld(closestOnSurface(local, a));
    }

    // Exact on a sphere; on an ellipsoid the radial projection is close to the
    // nearest point and its normal points out, which is all depenetration needs.
    @Override
    public SurfacePoint nearestSurface(Vec3 p) {
        float[] local = frame.toLocal(p);
        float[] a = frame.half;
        float value = implicit(local);

        if (value < 1e-12f) {
            // At the very center: leave along the thinnest axis.
            int thinnest = 0;
            for (int i = 1; i < 3; i++) {
                if (a[i] < a[thinnest]) {
                    thinnest = i;
                }
            }
            local = new float[3];
            local[thinnest] = a[thinnest];
        } else {
            float scale = (float) (1.0 / Math.sqrt(value));
            for (int i = 0; i < 3; i++) {
                local[i] *= scale;
            }
        }

        float[] gradient = new float[3];
        for (int i = 0; i < 3; i++) {
            gradient[i] = local[i] / (a[i] * a[i]);
        }

        return new SurfacePoint(frame.toWorld(local), frame.directionToWorld(gradient).normalizeLocal());
    }

    // In the space where the ellipsoid is the unit sphere the ray stays a line
    // with the same parameter, so a quadratic gives the entry distance.
    @Override
    public Maybe<Float> raycast(Vec3 origin, Vec3 direction, float maxDistance) {
        float[] o = frame.toLocal(origin);
        float[] d = frame.directionToLocal(direction);
        float[] a = frame.half;

        double qa = 0;
        double qb = 0;
        double qc = -1;
        for (int i = 0; i < 3; i++) {
            double oi = o[i] / a[i];
            double di = d[i] / a[i];
            qa += di * di;
            qb += 2 * oi * di;
            qc += oi * oi;
        }

        if (qc <= 0) {
            return Maybe.some(0f);
        }

        double discriminant = qb * qb - 4 * qa * qc;
        if (qa < 1e-12 || discriminant < 0) {
            return Maybe.none();
        }

        double t = (-qb - Math.sqrt(discriminant)) / (2 * qa);
        return t >= 0 && t <= maxDistance ? Maybe.some((float) t) : Maybe.none();
    }

    @Override
    public Aabb bounds() {
        return bounds.copy();
    }

    private float implicit(float[] local) {
        float sum = 0f;
        for (int i = 0; i < 3; i++) {
            float q = local[i] / frame.half[i];
            sum += q * q;
        }
        return sum;
    }

    /**
     * Nearest surface point to an outside point, both in the ellipsoid's space.
     * It is a[i]^2 y[i] / (t + a[i]^2) for the root t > 0 of
     * sum((a[i] y[i] / (t + a[i]^2))^2) = 1, found by bisection (Eberly,
     * "Distance from a Point to an Ellipse, an Ellipsoid, or a Hyperellipsoid").
     */
    private static float[] closestOnSurface(float[] y, float[] a) {
        double length = Math.sqrt(y[0] * y[0] + y[1] * y[1] + y[2] * y[2]);
        double low = 0;
        double high = Math.max(a[0], Math.max(a[1], a[2])) * length;

        for (int step = 0; step < BISECTION_STEPS; step++) {
            double t = (low + high) / 2;
            double sum = 0;
            for (int i = 0; i < 3; i++) {
                double r = a[i] * y[i] / (t + a[i] * a[i]);
                sum += r * r;
            }
            if (sum > 1) {
                low = t;
            } else {
                high = t;
            }
        }

        double t = (low + high) / 2;
        float[] x = new float[3];
        for (int i = 0; i < 3; i++) {
            double aa = (double) a[i] * a[i];
            x[i] = (float) (aa * y[i] / (t + aa));
        }
        return x;
    }
}
