package org.halalfoursome.kebabeditor.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.PrimitiveType;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;

/**
 * GPU copy of a {@link MeshData}: one VAO over an interleaved vertex buffer
 * (position, normal, uv) and a 32-bit index buffer. Missing normals or uvs
 * are uploaded as zeros so every mesh shares the same vertex layout.
 */
public final class GpuMesh {

    public static final int POSITION = 0;
    public static final int NORMAL = 1;
    public static final int UV = 2;

    private static final int FLOATS_PER_VERTEX = 8;
    private static final int STRIDE = FLOATS_PER_VERTEX * Float.BYTES;

    private final int vao;
    private final int vbo;
    private final int ibo;
    private final int indexCount;
    private final int glMode;

    private GpuMesh(int vao, int vbo, int ibo, int indexCount, int glMode) {
        this.vao = vao;
        this.vbo = vbo;
        this.ibo = ibo;
        this.indexCount = indexCount;
        this.glMode = glMode;
    }

    public static GpuMesh upload(GL3 gl, MeshData mesh) {
        float[] positions = mesh.positions();
        float[] normals = mesh.normals();
        float[] uvs = mesh.uvs();
        int vertexCount = mesh.vertexCount();

        FloatBuffer vertices = Buffers.newDirectFloatBuffer(vertexCount * FLOATS_PER_VERTEX);
        for (int v = 0; v < vertexCount; v++) {
            vertices.put(positions, v * 3, 3);
            if (normals.length != 0) {
                vertices.put(normals, v * 3, 3);
            } else {
                vertices.put(0).put(0).put(0);
            }
            if (uvs.length != 0) {
                vertices.put(uvs, v * 2, 2);
            } else {
                vertices.put(0).put(0);
            }
        }
        vertices.flip();

        IntBuffer indices = Buffers.newDirectIntBuffer(mesh.indices());

        int[] ids = new int[2];
        gl.glGenVertexArrays(1, ids, 0);
        int vao = ids[0];
        gl.glGenBuffers(2, ids, 0);
        int vbo = ids[0];
        int ibo = ids[1];

        gl.glBindVertexArray(vao);

        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL3.GL_ARRAY_BUFFER, (long) vertices.remaining() * Float.BYTES, vertices, GL3.GL_STATIC_DRAW);

        gl.glEnableVertexAttribArray(POSITION);
        gl.glVertexAttribPointer(POSITION, 3, GL3.GL_FLOAT, false, STRIDE, 0);
        gl.glEnableVertexAttribArray(NORMAL);
        gl.glVertexAttribPointer(NORMAL, 3, GL3.GL_FLOAT, false, STRIDE, 3L * Float.BYTES);
        gl.glEnableVertexAttribArray(UV);
        gl.glVertexAttribPointer(UV, 2, GL3.GL_FLOAT, false, STRIDE, 6L * Float.BYTES);

        // The element buffer binding is VAO state, so it stays bound to this VAO.
        gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, ibo);
        gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, (long) indices.remaining() * Integer.BYTES, indices, GL3.GL_STATIC_DRAW);

        restoreDefaultVao(gl);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

        int glMode = mesh.type() == PrimitiveType.LINES ? GL3.GL_LINES : GL3.GL_TRIANGLES;
        return new GpuMesh(vao, vbo, ibo, mesh.indexCount(), glMode);
    }

    public void draw(GL3 gl) {
        gl.glBindVertexArray(vao);
        gl.glDrawElements(glMode, indexCount, GL3.GL_UNSIGNED_INT, 0);
        restoreDefaultVao(gl);
    }

    // In core profiles JOGL keeps its own VAO bound and draws with it (e.g. the GLJPanel
    // blit). Unbinding to VAO 0 instead breaks that and leaves the panel black.
    static void restoreDefaultVao(GL3 gl) {
        gl.glBindVertexArray(gl.getContext().getDefaultVAO());
    }

    public boolean isLines() {
        return glMode == GL3.GL_LINES;
    }

    public void dispose(GL3 gl) {
        gl.glDeleteBuffers(2, new int[] { vbo, ibo }, 0);
        gl.glDeleteVertexArrays(1, new int[] { vao }, 0);
    }
}
