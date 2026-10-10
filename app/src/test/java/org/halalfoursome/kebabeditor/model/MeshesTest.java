package org.halalfoursome.kebabeditor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.junit.jupiter.api.Test;

class MeshesTest {

    private static final float EPS = 1e-5f;

    @Test
    void cubeFitsUnitCube() {
        assertFitsUnitCube(Meshes.CUBE_TRIANGLES.bounds());
        assertFitsUnitCube(Meshes.CUBE_LINES.bounds());
    }

    @Test
    void sphereFitsUnitCube() {
        assertFitsUnitCube(Meshes.SPHERE_TRIANGLES.bounds());
        assertFitsUnitCube(Meshes.SPHERE_LINES.bounds());
    }

    @Test
    void cubeCounts() {
        assertEquals(12, Meshes.CUBE_TRIANGLES.primitiveCount());
        assertEquals(24, Meshes.CUBE_TRIANGLES.vertexCount());
        assertEquals(12, Meshes.CUBE_LINES.primitiveCount());
        assertEquals(8, Meshes.CUBE_LINES.vertexCount());
    }

    @Test
    void cubeLinesAreAxisAlignedEdges() {
        float[] p = Meshes.CUBE_LINES.positions();
        int[] indices = Meshes.CUBE_LINES.indices();

        for (int i = 0; i < indices.length; i += 2) {
            int differing = 0;

            for (int axis = 0; axis < 3; axis++) {
                if (p[indices[i] * 3 + axis] != p[indices[i + 1] * 3 + axis]) {
                    differing++;
                }
            }

            assertEquals(1, differing);
        }
    }

    @Test
    void trianglesWindCounterClockwiseOutward() {
        assertWindsOutward(Meshes.CUBE_TRIANGLES);
        assertWindsOutward(Meshes.SPHERE_TRIANGLES);
    }

    @Test
    void triangleNormalsAreUnitAndAttributesPresent() {
        for (MeshData mesh : new MeshData[] { Meshes.CUBE_TRIANGLES, Meshes.SPHERE_TRIANGLES }) {
            float[] n = mesh.normals();

            assertTrue(mesh.hasNormals());
            assertTrue(mesh.hasUvs());

            for (int i = 0; i < n.length; i += 3) {
                float length = (float) Math.sqrt(n[i] * n[i] + n[i + 1] * n[i + 1] + n[i + 2] * n[i + 2]);

                assertEquals(1f, length, EPS);
            }
        }
    }

    @Test
    void spherePointsLieOnRadiusHalf() {
        for (MeshData mesh : new MeshData[] { Meshes.SPHERE_TRIANGLES, Meshes.SPHERE_LINES }) {
            float[] p = mesh.positions();

            for (int i = 0; i < p.length; i += 3) {
                float radius = (float) Math.sqrt(p[i] * p[i] + p[i + 1] * p[i + 1] + p[i + 2] * p[i + 2]);

                assertEquals(0.5f, radius, EPS);
            }
        }
    }

    @Test
    void sphereHasNoDegenerateTriangles() {
        float[] p = Meshes.SPHERE_TRIANGLES.positions();
        int[] indices = Meshes.SPHERE_TRIANGLES.indices();

        for (int i = 0; i < indices.length; i += 3) {
            float[] normal = faceNormal(p, indices[i], indices[i + 1], indices[i + 2]);

            assertTrue(length(normal) > EPS);
        }
    }

    @Test
    void sphereLinesFormThreeClosedLoops() {
        MeshData mesh = Meshes.sphereLines(8);

        assertEquals(24, mesh.vertexCount());
        assertEquals(24, mesh.primitiveCount());
    }

    @Test
    void rejectsTooCoarseSpheres() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Meshes.sphereTriangles(1, 8)
        );
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Meshes.sphereLines(2)
        );
    }

    private static void assertFitsUnitCube(Aabb box) {
        assertEquals(-0.5f, box.getMinX(), EPS);
        assertEquals(0.5f, box.getMaxX(), EPS);
        assertEquals(-0.5f, box.getMinY(), EPS);
        assertEquals(0.5f, box.getMaxY(), EPS);
        assertEquals(-0.5f, box.getMinZ(), EPS);
        assertEquals(0.5f, box.getMaxZ(), EPS);
    }

    // Every triangle's geometric normal must agree with the direction from the centre to its centroid.
    private static void assertWindsOutward(MeshData mesh) {
        float[] p = mesh.positions();
        int[] indices = mesh.indices();

        for (int i = 0; i < indices.length; i += 3) {
            int a = indices[i];
            int b = indices[i + 1];
            int c = indices[i + 2];
            float[] normal = faceNormal(p, a, b, c);

            if (length(normal) <= EPS) {
                continue;
            }

            float dot = 0;

            for (int axis = 0; axis < 3; axis++) {
                float centroid = (p[a * 3 + axis] + p[b * 3 + axis] + p[c * 3 + axis]) / 3f;
                dot += normal[axis] * centroid;
            }

            assertTrue(dot > 0, "triangle " + (i / 3) + " faces inward");
        }
    }

    private static float[] faceNormal(float[] p, int a, int b, int c) {
        float[] ab = new float[3];
        float[] ac = new float[3];

        for (int axis = 0; axis < 3; axis++) {
            ab[axis] = p[b * 3 + axis] - p[a * 3 + axis];
            ac[axis] = p[c * 3 + axis] - p[a * 3 + axis];
        }

        return new float[] {
            ab[1] * ac[2] - ab[2] * ac[1],
            ab[2] * ac[0] - ab[0] * ac[2],
            ab[0] * ac[1] - ab[1] * ac[0],
        };
    }

    private static float length(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }
}
