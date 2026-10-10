package org.halalfoursome.kebabeditor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.junit.jupiter.api.Test;

class MeshDataTest {

    private static final float[] TRIANGLE = { 0, 0, 0, 2, 0, 0, 0, 3, -1 };

    @Test
    void computesBoundsAndCounts() {
        MeshData mesh = MeshData.triangles(TRIANGLE, new float[0], new float[0], new int[] { 0, 1, 2 });
        Aabb bounds = mesh.bounds();

        assertEquals(3, mesh.vertexCount());
        assertEquals(1, mesh.primitiveCount());
        assertEquals(0f, bounds.getMinX());
        assertEquals(2f, bounds.getMaxX());
        assertEquals(3f, bounds.getMaxY());
        assertEquals(-1f, bounds.getMinZ());
        assertFalse(mesh.hasNormals());
        assertFalse(mesh.hasUvs());
    }

    @Test
    void isImmutableFromOutside() {
        float[] source = TRIANGLE.clone();
        MeshData mesh = MeshData.lines(source, new int[] { 0, 1 });

        source[3] = 99f;
        mesh.positions()[3] = 99f;
        mesh.bounds().setMaxX(99f);

        assertEquals(2f, mesh.positions()[3]);
        assertEquals(2f, mesh.bounds().getMaxX());
    }

    @Test
    void rejectsIndexOutOfRange() {
        assertThrows(
            IllegalArgumentException.class,
            () -> MeshData.lines(TRIANGLE, new int[] { 0, 3 })
        );
    }

    @Test
    void rejectsIndexCountNotMatchingPrimitive() {
        assertThrows(
            IllegalArgumentException.class,
            () -> MeshData.triangles(TRIANGLE, new float[0], new float[0], new int[] { 0, 1 })
        );
    }

    @Test
    void rejectsMismatchedAttributes() {
        int[] indices = { 0, 1, 2 };

        assertThrows(
            IllegalArgumentException.class,
            () -> MeshData.triangles(TRIANGLE, new float[3], new float[0], indices)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> MeshData.triangles(TRIANGLE, new float[0], new float[4], indices)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> MeshData.triangles(new float[4], new float[0], new float[0], new int[0])
        );
    }

    @Test
    void emptyMeshHasZeroBounds() {
        MeshData mesh = MeshData.lines(new float[0], new int[0]);

        assertEquals(new Aabb(), mesh.bounds());
        assertTrue(mesh.primitiveCount() == 0);
    }
}
