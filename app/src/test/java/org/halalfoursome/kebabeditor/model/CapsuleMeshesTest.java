package org.halalfoursome.kebabeditor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.junit.jupiter.api.Test;

class CapsuleMeshesTest {

    private static final float EPS = 1e-4f;

    private static void assertSpans(Aabb bounds, float radius, float height) {
        assertEquals(-radius, bounds.getMinX(), EPS);
        assertEquals(radius, bounds.getMaxX(), EPS);
        assertEquals(0f, bounds.getMinY(), EPS);
        assertEquals(height, bounds.getMaxY(), EPS);
        assertEquals(-radius, bounds.getMinZ(), EPS);
        assertEquals(radius, bounds.getMaxZ(), EPS);
    }

    @Test
    void trianglesStandOnTheOriginAtFullSize() {
        MeshData mesh = Meshes.capsuleTriangles(0.35f, 1.8f, 6, 24);

        assertEquals(PrimitiveType.TRIANGLES, mesh.type());
        assertSpans(mesh.bounds(), 0.35f, 1.8f);
    }

    @Test
    void trianglesHaveUnitNormalsPointingAwayFromTheAxis() {
        MeshData mesh = Meshes.capsuleTriangles(0.5f, 2f, 4, 12);
        float[] p = mesh.positions();
        float[] n = mesh.normals();

        for (int v = 0; v < mesh.vertexCount(); v++) {
            float nx = n[v * 3];
            float ny = n[v * 3 + 1];
            float nz = n[v * 3 + 2];
            assertEquals(1f, (float) Math.sqrt(nx * nx + ny * ny + nz * nz), EPS);
            // Horizontally the normal points the same way as the vertex.
            assertEquals(0f, Math.min(0f, nx * p[v * 3] + nz * p[v * 3 + 2]), EPS);
        }
    }

    @Test
    void linesCoverTheSameCapsule() {
        MeshData mesh = Meshes.capsuleLines(0.35f, 1.8f, 24);

        assertEquals(PrimitiveType.LINES, mesh.type());
        assertSpans(mesh.bounds(), 0.35f, 1.8f);
    }

    @Test
    void rejectsCapsulesShorterThanTheirWidth() {
        assertThrows(IllegalArgumentException.class, () -> Meshes.capsuleTriangles(1f, 1f, 4, 12));
        assertThrows(IllegalArgumentException.class, () -> Meshes.capsuleLines(0.3f, 1f, 10));
    }
}
