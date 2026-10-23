package org.halalfoursome.kebabeditor.model;

// Shared unit meshes that fit the [-0.5, 0.5] cube, so scaling by a full size gives that size.
public final class Meshes {

    private static final int DEFAULT_SEGMENTS = 32;
    private static final int DEFAULT_RINGS = 16;
    private static final int DEFAULT_CONE_SEGMENTS = 24;

    public static final MeshData CUBE_TRIANGLES = cubeTriangles();
    public static final MeshData CUBE_LINES = cubeLines();
    public static final MeshData SPHERE_TRIANGLES = sphereTriangles(DEFAULT_RINGS, DEFAULT_SEGMENTS);
    public static final MeshData SPHERE_LINES = sphereLines(DEFAULT_SEGMENTS);
    public static final MeshData QUAD = quad();
    public static final MeshData ARROW_LINES = arrowLines();
    public static final MeshData CONE_LINES = coneLines(DEFAULT_CONE_SEGMENTS);

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

    public static MeshData quad() {
        float[] positions = {
            -0.5f, -0.5f, 0,
            0.5f, -0.5f, 0,
            0.5f, 0.5f, 0,
            -0.5f, 0.5f, 0,
        };
        float[] uvs = {
            0, 1,
            1, 1,
            1, 0,
            0, 0,
        };
        int[] indices = { 0, 1, 2, 0, 2, 3 };

        return MeshData.triangles(positions, new float[0], uvs, indices);
    }

    public static MeshData arrowLines() {
        float[] positions = {
            0, 0, 0,
            0, 0, -1,
            0.1f, 0, -0.8f,
            -0.1f, 0, -0.8f,
            0, 0.1f, -0.8f,
            0, -0.1f, -0.8f,
        };
        int[] indices = { 0, 1, 1, 2, 1, 3, 1, 4, 1, 5 };

        return MeshData.lines(positions, indices);
    }

    public static MeshData coneLines(int segments) {
        if (segments < 4) {
            throw new IllegalArgumentException("need at least 4 segments");
        }

        float[] positions = new float[(segments + 1) * 3];
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            set(positions, i + 1, (float) Math.cos(angle), (float) Math.sin(angle), -1f);
        }

        int[] indices = new int[(segments + 4) * 2];
        int next = 0;

        for (int i = 0; i < segments; i++) {
            indices[next++] = i + 1;
            indices[next++] = (i + 1) % segments + 1;
        }
        for (int side = 0; side < 4; side++) {
            indices[next++] = 0;
            indices[next++] = side * segments / 4 + 1;
        }

        return MeshData.lines(positions, indices);
    }

    /**
     * An upright capsule from y = 0 to y = height, built to size so its ends stay round.
     *
     * @param capRings rings per hemisphere
     */
    public static MeshData capsuleTriangles(float radius, float height, int capRings, int segments) {
        if (capRings < 1 || segments < 3) {
            throw new IllegalArgumentException("need at least 1 cap ring and 3 segments");
        }
        if (radius <= 0 || height < 2 * radius) {
            throw new IllegalArgumentException("need radius > 0 and height >= 2 * radius");
        }

        // The top cap's equator row and the bottom cap's form the straight middle band.
        int rows = 2 * (capRings + 1);
        int columns = segments + 1;
        float[] positions = new float[rows * columns * 3];
        float[] normals = new float[rows * columns * 3];

        for (int row = 0; row < rows; row++) {
            boolean top = row <= capRings;
            int ring = top ? row : row - capRings - 1;
            double theta = Math.PI / 2 * (top ? (double) ring / capRings : 1 + (double) ring / capRings);
            float centerY = top ? height - radius : radius;

            for (int s = 0; s <= segments; s++) {
                double phi = 2 * Math.PI * s / segments;
                int vertex = row * columns + s;
                float x = (float) (Math.sin(theta) * Math.cos(phi));
                float y = (float) Math.cos(theta);
                float z = (float) (Math.sin(theta) * Math.sin(phi));

                set(positions, vertex, radius * x, centerY + radius * y, radius * z);
                set(normals, vertex, x, y, z);
            }
        }

        // Same winding as sphereTriangles; the pole rows skip their degenerate halves.
        int[] indices = new int[((rows - 1) * segments * 2 - 2 * segments) * 3];
        int next = 0;

        for (int r = 0; r < rows - 1; r++) {
            for (int s = 0; s < segments; s++) {
                int a = r * columns + s;
                int b = a + columns;

                if (r != 0) {
                    indices[next++] = a;
                    indices[next++] = a + 1;
                    indices[next++] = b;
                }
                if (r != rows - 2) {
                    indices[next++] = a + 1;
                    indices[next++] = b + 1;
                    indices[next++] = b;
                }
            }
        }

        return MeshData.triangles(positions, normals, new float[0], indices);
    }

    /**
     * Wireframe of the capsule {@link #capsuleTriangles} builds.
     *
     * @param segments a multiple of 4, so the side lines meet ring vertices
     */
    public static MeshData capsuleLines(float radius, float height, int segments) {
        if (segments < 4 || segments % 4 != 0) {
            throw new IllegalArgumentException("segments must be a positive multiple of 4");
        }
        if (radius <= 0 || height < 2 * radius) {
            throw new IllegalArgumentException("need radius > 0 and height >= 2 * radius");
        }

        int half = segments / 2;
        float low = radius;
        float high = height - radius;
        float[] positions = new float[(2 * segments + 4 * (half + 1)) * 3];
        int[] indices = new int[(2 * segments + 4 + 4 * half) * 2];
        int vertex = 0;
        int next = 0;

        for (float y : new float[] { low, high }) {
            int first = vertex;
            for (int i = 0; i < segments; i++) {
                double angle = 2 * Math.PI * i / segments;
                set(positions, vertex++, radius * (float) Math.cos(angle), y, radius * (float) Math.sin(angle));
                indices[next++] = first + i;
                indices[next++] = first + (i + 1) % segments;
            }
        }

        for (int side = 0; side < 4; side++) {
            indices[next++] = side * segments / 4;
            indices[next++] = segments + side * segments / 4;
        }

        // Half circles over the top and under the bottom, across X and across Z.
        for (int arc = 0; arc < 4; arc++) {
            boolean alongX = arc % 2 == 0;
            boolean top = arc < 2;
            int first = vertex;

            for (int i = 0; i <= half; i++) {
                double angle = Math.PI * i / half;
                float across = radius * (float) Math.cos(angle);
                float rise = radius * (float) Math.sin(angle);
                float y = top ? high + rise : low - rise;
                set(positions, vertex++, alongX ? across : 0, y, alongX ? 0 : across);

                if (i > 0) {
                    indices[next++] = first + i - 1;
                    indices[next++] = first + i;
                }
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
