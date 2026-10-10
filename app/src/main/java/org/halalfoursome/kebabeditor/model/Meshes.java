package org.halalfoursome.kebabeditor.model;

// Shared unit meshes that fit the [-0.5, 0.5] cube, so scaling by a full size gives that size.
public final class Meshes {

    private static final int DEFAULT_SEGMENTS = 32;
    private static final int DEFAULT_RINGS = 16;

    public static final MeshData CUBE_TRIANGLES = cubeTriangles();
    public static final MeshData CUBE_LINES = cubeLines();
    public static final MeshData SPHERE_TRIANGLES = sphereTriangles(DEFAULT_RINGS, DEFAULT_SEGMENTS);
    public static final MeshData SPHERE_LINES = sphereLines(DEFAULT_SEGMENTS);

    private Meshes() {}

    public static MeshData cubeTriangles() {
        // Each face: normal n and tangents u, v with u x v = n, so corners wind counter-clockwise.
        float[][] faces = {
            { 1, 0, 0,   0, 1, 0,   0, 0, 1 },
            { -1, 0, 0,  0, 0, 1,   0, 1, 0 },
            { 0, 1, 0,   0, 0, 1,   1, 0, 0 },
            { 0, -1, 0,  1, 0, 0,   0, 0, 1 },
            { 0, 0, 1,   1, 0, 0,   0, 1, 0 },
            { 0, 0, -1,  0, 1, 0,   1, 0, 0 },
        };
        float[][] corners = { { -1, -1 }, { 1, -1 }, { 1, 1 }, { -1, 1 } };
        float[][] cornerUvs = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } };

        float[] positions = new float[24 * 3];
        float[] normals = new float[24 * 3];
        float[] uvs = new float[24 * 2];
        int[] indices = new int[36];

        for (int f = 0; f < faces.length; f++) {
            float[] face = faces[f];

            for (int c = 0; c < 4; c++) {
                int vertex = f * 4 + c;

                for (int axis = 0; axis < 3; axis++) {
                    positions[vertex * 3 + axis] = 0.5f * face[axis]
                        + 0.5f * corners[c][0] * face[3 + axis]
                        + 0.5f * corners[c][1] * face[6 + axis];
                    normals[vertex * 3 + axis] = face[axis];
                }

                uvs[vertex * 2] = cornerUvs[c][0];
                uvs[vertex * 2 + 1] = cornerUvs[c][1];
            }

            int base = f * 4;
            int offset = f * 6;
            indices[offset] = base;
            indices[offset + 1] = base + 1;
            indices[offset + 2] = base + 2;
            indices[offset + 3] = base;
            indices[offset + 4] = base + 2;
            indices[offset + 5] = base + 3;
        }

        return MeshData.triangles(positions, normals, uvs, indices);
    }

    public static MeshData cubeLines() {
        float[] positions = new float[8 * 3];

        for (int i = 0; i < 8; i++) {
            positions[i * 3] = (i & 1) == 0 ? -0.5f : 0.5f;
            positions[i * 3 + 1] = (i & 2) == 0 ? -0.5f : 0.5f;
            positions[i * 3 + 2] = (i & 4) == 0 ? -0.5f : 0.5f;
        }

        // An edge joins two corners whose indices differ in exactly one bit.
        int[] indices = new int[12 * 2];
        int next = 0;

        for (int a = 0; a < 8; a++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                int b = a | bit;

                if (b != a) {
                    indices[next++] = a;
                    indices[next++] = b;
                }
            }
        }

        return MeshData.lines(positions, indices);
    }

    public static MeshData sphereTriangles(int rings, int segments) {
        if (rings < 2 || segments < 3) {
            throw new IllegalArgumentException("need at least 2 rings and 3 segments");
        }

        int columns = segments + 1;
        int vertexCount = (rings + 1) * columns;
        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        float[] uvs = new float[vertexCount * 2];

        for (int r = 0; r <= rings; r++) {
            double theta = Math.PI * r / rings;

            for (int s = 0; s <= segments; s++) {
                double phi = 2 * Math.PI * s / segments;
                int vertex = r * columns + s;
                float x = (float) (Math.sin(theta) * Math.cos(phi));
                float y = (float) Math.cos(theta);
                float z = (float) (Math.sin(theta) * Math.sin(phi));

                positions[vertex * 3] = 0.5f * x;
                positions[vertex * 3 + 1] = 0.5f * y;
                positions[vertex * 3 + 2] = 0.5f * z;
                normals[vertex * 3] = x;
                normals[vertex * 3 + 1] = y;
                normals[vertex * 3 + 2] = z;
                uvs[vertex * 2] = (float) s / segments;
                uvs[vertex * 2 + 1] = (float) r / rings;
            }
        }

        // The pole rows collapse to a point, so one triangle of each pole quad is degenerate.
        int[] indices = new int[(rings * segments * 2 - 2 * segments) * 3];
        int next = 0;

        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < segments; s++) {
                int a = r * columns + s;
                int b = a + columns;

                if (r != 0) {
                    indices[next++] = a;
                    indices[next++] = a + 1;
                    indices[next++] = b;
                }
                if (r != rings - 1) {
                    indices[next++] = a + 1;
                    indices[next++] = b + 1;
                    indices[next++] = b;
                }
            }
        }

        return MeshData.triangles(positions, normals, uvs, indices);
    }

    public static MeshData sphereLines(int segments) {
        if (segments < 3) {
            throw new IllegalArgumentException("need at least 3 segments");
        }

        float[] positions = new float[3 * segments * 3];
        int[] indices = new int[3 * segments * 2];

        for (int circle = 0; circle < 3; circle++) {
            for (int i = 0; i < segments; i++) {
                double angle = 2 * Math.PI * i / segments;
                float c = 0.5f * (float) Math.cos(angle);
                float s = 0.5f * (float) Math.sin(angle);
                int vertex = circle * segments + i;

                // Great circles in the XY, YZ and XZ planes.
                switch (circle) {
                    case 0 -> set(positions, vertex, c, s, 0);
                    case 1 -> set(positions, vertex, 0, c, s);
                    default -> set(positions, vertex, c, 0, s);
                }

                int index = vertex * 2;
                indices[index] = vertex;
                indices[index + 1] = circle * segments + (i + 1) % segments;
            }
        }

        return MeshData.lines(positions, indices);
    }

    private static void set(float[] positions, int vertex, float x, float y, float z) {
        positions[vertex * 3] = x;
        positions[vertex * 3 + 1] = y;
        positions[vertex * 3 + 2] = z;
    }
}
