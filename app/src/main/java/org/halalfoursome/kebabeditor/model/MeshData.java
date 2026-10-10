package org.halalfoursome.kebabeditor.model;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Aabb;

/**
 * Immutable CPU-side geometry. 
 * 
 * Equality is by referential identity (a == b) 
 * so a renderer can key its GPU cache on it.
 */
public final class MeshData {

    private static final float[] NONE = new float[0];

    private final PrimitiveType type;
    private final float[] positions;
    private final float[] normals;
    private final float[] uvs;
    private final int[] indices;
    private final Aabb bounds;

    private MeshData(
        PrimitiveType type,
        float[] positions,
        float[] normals,
        float[] uvs,
        int[] indices
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(normals, "normals");
        Objects.requireNonNull(uvs, "uvs");
        Objects.requireNonNull(indices, "indices");

        if (positions.length % 3 != 0) {
            throw new IllegalArgumentException("positions length must be a multiple of 3");
        }

        int vertexCount = positions.length / 3;

        if (normals.length != 0 && normals.length != positions.length) {
            throw new IllegalArgumentException("normals must be empty or match positions");
        }

        if (uvs.length != 0 && uvs.length != vertexCount * 2) {
            throw new IllegalArgumentException("uvs must be empty or have 2 floats per vertex");
        }

        if (indices.length % type.indicesPerPrimitive() != 0) {
            throw new IllegalArgumentException(
                "index count must be a multiple of " + type.indicesPerPrimitive()
            );
        }

        for (int index : indices) {
            if (index < 0 || index >= vertexCount) {
                throw new IllegalArgumentException("index out of range: " + index);
            }
        }

        this.type = type;
        this.positions = positions.clone();
        this.normals = normals.clone();
        this.uvs = uvs.clone();
        this.indices = indices.clone();
        this.bounds = computeBounds(this.positions);
    }

    public static MeshData triangles(
        float[] positions,
        float[] normals,
        float[] uvs,
        int[] indices
    ) {
        return new MeshData(PrimitiveType.TRIANGLES, positions, normals, uvs, indices);
    }

    public static MeshData lines(float[] positions, int[] indices) {
        return new MeshData(PrimitiveType.LINES, positions, NONE, NONE, indices);
    }

    public PrimitiveType type() {
        return type;
    }

    public float[] positions() {
        return positions.clone();
    }

    public float[] normals() {
        return normals.clone();
    }

    public float[] uvs() {
        return uvs.clone();
    }

    public int[] indices() {
        return indices.clone();
    }

    public Aabb bounds() {
        return copyOf(bounds);
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    public int indexCount() {
        return indices.length;
    }

    public int primitiveCount() {
        return indices.length / type.indicesPerPrimitive();
    }

    public boolean hasNormals() {
        return normals.length != 0;
    }

    public boolean hasUvs() {
        return uvs.length != 0;
    }

    private static Aabb computeBounds(float[] positions) {
        Aabb box = new Aabb();

        if (positions.length == 0) {
            return box;
        }

        box.setMinX(positions[0]);
        box.setMaxX(positions[0]);
        box.setMinY(positions[1]);
        box.setMaxY(positions[1]);
        box.setMinZ(positions[2]);
        box.setMaxZ(positions[2]);

        for (int i = 3; i < positions.length; i += 3) {
            box.setMinX(Math.min(box.getMinX(), positions[i]));
            box.setMaxX(Math.max(box.getMaxX(), positions[i]));
            box.setMinY(Math.min(box.getMinY(), positions[i + 1]));
            box.setMaxY(Math.max(box.getMaxY(), positions[i + 1]));
            box.setMinZ(Math.min(box.getMinZ(), positions[i + 2]));
            box.setMaxZ(Math.max(box.getMaxZ(), positions[i + 2]));
        }

        return box;
    }

    private static Aabb copyOf(Aabb source) {
        Aabb copy = new Aabb();
        copy.setMinX(source.getMinX());
        copy.setMaxX(source.getMaxX());
        copy.setMinY(source.getMinY());
        copy.setMaxY(source.getMaxY());
        copy.setMinZ(source.getMinZ());
        copy.setMaxZ(source.getMaxZ());

        return copy;
    }
}
