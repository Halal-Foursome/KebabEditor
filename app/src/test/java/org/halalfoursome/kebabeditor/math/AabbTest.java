package org.halalfoursome.kebabeditor.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AabbTest {

    private static final float EPS = 1e-4f;

    @Test
    void centeredBuildsSymmetricBox() {
        Aabb box = Aabb.centered(new Vec3(1, 2, 3), 0.5f);

        assertEquals(new Aabb(0.5f, 1.5f, 1.5f, 2.5f, 2.5f, 3.5f), box);
    }

    @Test
    void transformedCoversAllCorners() {
        Aabb unit = new Aabb(-0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f);
        Mat4 matrix = Mat4.translation(new Vec3(1, 0, 0)).mul(Mat4.scaling(new Vec3(2, 4, 6)));

        Aabb box = unit.transformed(matrix);

        assertEquals(0f, box.getMinX(), EPS);
        assertEquals(2f, box.getMaxX(), EPS);
        assertEquals(-2f, box.getMinY(), EPS);
        assertEquals(2f, box.getMaxY(), EPS);
        assertEquals(-3f, box.getMinZ(), EPS);
        assertEquals(3f, box.getMaxZ(), EPS);
    }

    @Test
    void transformedDoesNotMutateTheSource() {
        Aabb unit = new Aabb(-0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f);

        unit.transformed(Mat4.scaling(10f));

        assertEquals(0.5f, unit.getMaxX());
    }
}
