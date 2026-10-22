package org.halalfoursome.kebabeditor.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aabb {

    private float minX; 
    private float maxX; 
    private float minY; 
    private float maxY; 
    private float minZ; 
    private float maxZ; 

    public static Aabb centered(Vec3 center, float halfExtent) {
        return new Aabb(
            center.getX() - halfExtent,
            center.getX() + halfExtent,
            center.getY() - halfExtent,
            center.getY() + halfExtent,
            center.getZ() - halfExtent,
            center.getZ() + halfExtent
        );
    }

    // Axis-aligned box around the eight transformed corners
    public Aabb transformed(Mat4 matrix) {
        Aabb result = null;

        for (int i = 0; i < 8; i++) {
            Vec3 corner = matrix.transformPoint(new Vec3(
                (i & 1) == 0 ? minX : maxX,
                (i & 2) == 0 ? minY : maxY,
                (i & 4) == 0 ? minZ : maxZ
            ));

            if (result == null) {
                result = new Aabb(
                    corner.getX(), corner.getX(),
                    corner.getY(), corner.getY(),
                    corner.getZ(), corner.getZ()
                );
            } else {
                result.minX = Math.min(result.minX, corner.getX());
                result.maxX = Math.max(result.maxX, corner.getX());
                result.minY = Math.min(result.minY, corner.getY());
                result.maxY = Math.max(result.maxY, corner.getY());
                result.minZ = Math.min(result.minZ, corner.getZ());
                result.maxZ = Math.max(result.maxZ, corner.getZ());
            }
        }

        return result;
    }

    public Aabb copy() {
        return new Aabb(minX, maxX, minY, maxY, minZ, maxZ);
    }

    // Touching boxes count as intersecting
    public boolean intersects(Aabb other) {
        return minX <= other.maxX && maxX >= other.minX
            && minY <= other.maxY && maxY >= other.minY
            && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public Aabb union(Aabb other) {
        return new Aabb(
            Math.min(minX, other.minX),
            Math.max(maxX, other.maxX),
            Math.min(minY, other.minY),
            Math.max(maxY, other.maxY),
            Math.min(minZ, other.minZ),
            Math.max(maxZ, other.maxZ)
        );
    }
}
