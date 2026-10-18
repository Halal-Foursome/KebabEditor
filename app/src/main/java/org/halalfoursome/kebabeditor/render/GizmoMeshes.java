package org.halalfoursome.kebabeditor.render;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.MeshData;

/**
 * Procedural gizmo handle geometry. Axis handles are built along +X with unit
 * length and rotated onto Y and Z by the renderer.
 */
final class GizmoMeshes {

    static final float HANDLE_START = 0.14f;

    private static final float SHAFT_HALF_WIDTH = 0.012f;
    private static final float ARROW_TIP_START = 0.80f;
    private static final float ARROW_TIP_RADIUS = 0.055f;
    private static final float SCALE_TIP_CENTER = 0.94f;
    private static final float SCALE_TIP_HALF = 0.05f;
    static final float RING_RADIUS = 0.9f;
    private static final float RING_TUBE = 0.014f;
    static final float PLANE_START = 0.24f;
    static final float PLANE_END = 0.42f;

    private GizmoMeshes() {}

    /** Shaft with a cone tip: the translate handle. */
    static MeshData arrow() {
        Builder b = new Builder();
        b.box(new Vec3(HANDLE_START, -SHAFT_HALF_WIDTH, -SHAFT_HALF_WIDTH), new Vec3(ARROW_TIP_START, SHAFT_HALF_WIDTH, SHAFT_HALF_WIDTH));
        b.cone(ARROW_TIP_START, 1f, ARROW_TIP_RADIUS, 16);
        return b.build();
    }

    /** Shaft with a cube tip: the scale handle. */
    static MeshData scaleHandle() {
        Builder b = new Builder();
        b.box(new Vec3(HANDLE_START, -SHAFT_HALF_WIDTH, -SHAFT_HALF_WIDTH),
            new Vec3(SCALE_TIP_CENTER - SCALE_TIP_HALF, SHAFT_HALF_WIDTH, SHAFT_HALF_WIDTH));
        b.box(new Vec3(SCALE_TIP_CENTER - SCALE_TIP_HALF, -SCALE_TIP_HALF, -SCALE_TIP_HALF),
            new Vec3(SCALE_TIP_CENTER + SCALE_TIP_HALF, SCALE_TIP_HALF, SCALE_TIP_HALF));
        return b.build();
    }

    /** A thin torus around the X axis (in the YZ plane): the rotate handle. */
    static MeshData ring() {
        Builder b = new Builder();
        b.torus(RING_RADIUS, RING_TUBE, 72, 8);
        return b.build();
    }

    static MeshData centerCube(float halfSize) {
        Builder b = new Builder();
        b.box(new Vec3(-halfSize, -halfSize, -halfSize), new Vec3(halfSize, halfSize, halfSize));
        return b.build();
    }

    /**
     * Square handle for moving in a plane, spanned by the two given unit axes.
     * Double-sided by construction (both windings are emitted).
     */
    static MeshData plane(Vec3 u, Vec3 v) {
        Vec3 n = u.cross(v);
        Vec3 a = u.mul(PLANE_START).addLocal(v.mul(PLANE_START));
        Vec3 b = u.mul(PLANE_END).addLocal(v.mul(PLANE_START));
        Vec3 c = u.mul(PLANE_END).addLocal(v.mul(PLANE_END));
        Vec3 d = u.mul(PLANE_START).addLocal(v.mul(PLANE_END));

        Builder builder = new Builder();
        builder.quad(a, b, c, d, n);
        builder.quad(a, d, c, b, n.negate());
        return builder.build();
    }

    private static final class Builder {

        private final List<Float> positions = new ArrayList<>();
        private final List<Float> normals = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();

        private int vertex(Vec3 p, Vec3 n) {
            positions.add(p.getX());
            positions.add(p.getY());
            positions.add(p.getZ());
            normals.add(n.getX());
            normals.add(n.getY());
            normals.add(n.getZ());
            return positions.size() / 3 - 1;
        }

        private void triangle(int a, int b, int c) {
            indices.add(a);
            indices.add(b);
            indices.add(c);
        }

        // Corners counter-clockwise when seen from the side n points to.
        void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 n) {
            int ia = vertex(a, n), ib = vertex(b, n), ic = vertex(c, n), id = vertex(d, n);
            triangle(ia, ib, ic);
            triangle(ia, ic, id);
        }

        void box(Vec3 min, Vec3 max) {
            float x0 = min.getX(), y0 = min.getY(), z0 = min.getZ();
            float x1 = max.getX(), y1 = max.getY(), z1 = max.getZ();

            quad(new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x1, y0, z1), Vec3.unitX());
            quad(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), Vec3.unitX().negate());
            quad(new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), new Vec3(x1, y1, z0), Vec3.unitY());
            quad(new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x0, y0, z1), Vec3.unitY().negate());
            quad(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), Vec3.unitZ());
            quad(new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y0, z0), Vec3.unitZ().negate());
        }

        // Cone along +X from a disc at baseX to a point at tipX.
        void cone(float baseX, float tipX, float radius, int segments) {
            float slope = radius / (tipX - baseX);
            Vec3 baseCenter = new Vec3(baseX, 0, 0);
            Vec3 tip = new Vec3(tipX, 0, 0);

            for (int s = 0; s < segments; s++) {
                double a0 = 2 * Math.PI * s / segments;
                double a1 = 2 * Math.PI * (s + 1) / segments;
                Vec3 r0 = new Vec3(0, (float) Math.cos(a0), (float) Math.sin(a0));
                Vec3 r1 = new Vec3(0, (float) Math.cos(a1), (float) Math.sin(a1));
                Vec3 p0 = baseCenter.add(r0.mul(radius));
                Vec3 p1 = baseCenter.add(r1.mul(radius));

                // Side: the normal leans forward by the cone's slope.
                Vec3 n0 = r0.add(new Vec3(slope, 0, 0)).normalizeLocal();
                Vec3 n1 = r1.add(new Vec3(slope, 0, 0)).normalizeLocal();
                Vec3 nTip = r0.add(r1).mulLocal(0.5f).addLocal(slope, 0, 0).normalizeLocal();
                triangle(vertex(p0, n0), vertex(p1, n1), vertex(tip, nTip));

                // Base cap faces -X.
                Vec3 back = Vec3.unitX().negate();
                triangle(vertex(baseCenter, back), vertex(p1, back), vertex(p0, back));
            }
        }

        void torus(float radius, float tube, int segments, int sides) {
            int first = positions.size() / 3;

            for (int s = 0; s <= segments; s++) {
                double a = 2 * Math.PI * s / segments;
                Vec3 radial = new Vec3(0, (float) Math.cos(a), (float) Math.sin(a));

                for (int t = 0; t <= sides; t++) {
                    double b = 2 * Math.PI * t / sides;
                    Vec3 n = radial.mul((float) Math.cos(b)).addLocal(new Vec3((float) Math.sin(b), 0, 0));
                    vertex(radial.mul(radius).addLocal(n.mul(tube)), n);
                }
            }

            int columns = sides + 1;
            for (int s = 0; s < segments; s++) {
                for (int t = 0; t < sides; t++) {
                    int a = first + s * columns + t;
                    int b = a + columns;
                    triangle(a, b, a + 1);
                    triangle(a + 1, b, b + 1);
                }
            }
        }

        MeshData build() {
            float[] p = new float[positions.size()];
            float[] n = new float[normals.size()];
            int[] i = new int[indices.size()];
            for (int k = 0; k < p.length; k++) {
                p[k] = positions.get(k);
                n[k] = normals.get(k);
            }
            for (int k = 0; k < i.length; k++) {
                i[k] = indices.get(k);
            }
            return MeshData.triangles(p, n, new float[0], i);
        }
    }
}
