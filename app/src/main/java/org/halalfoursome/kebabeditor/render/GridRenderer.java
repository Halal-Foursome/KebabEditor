package org.halalfoursome.kebabeditor.render;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.MeshData;

import com.jogamp.opengl.GL3;

/**
 * Ground grid on the XZ plane with the X (red) and Z (blue) axes.
 *
 * The grid mesh is finite but follows the camera in whole major-cell steps, so
 * lines stay anchored to world coordinates while it looks endless. Lines fade
 * out with distance from the camera.
 */
public class GridRenderer implements RenderPass {

    private static final int HALF_EXTENT = 100;
    private static final int MAJOR_STEP = 10;
    private static final float AXIS_EXTENT = 1000f;

    private ShaderProgram shader;
    private GpuMesh minorLines;
    private GpuMesh majorLines;
    private GpuMesh xAxis;
    private GpuMesh zAxis;

    @Override
    public void init(GL3 gl) {
        shader = ShaderProgram.load(gl, "grid");
        minorLines = GpuMesh.upload(gl, gridLines(false));
        majorLines = GpuMesh.upload(gl, gridLines(true));
        xAxis = GpuMesh.upload(gl, line(-AXIS_EXTENT, 0, AXIS_EXTENT, 0));
        zAxis = GpuMesh.upload(gl, line(0, -AXIS_EXTENT, 0, AXIS_EXTENT));
    }

    @Override
    public void render(GL3 gl, RenderContext context) {
        Vec3 eye = context.cameraPosition();
        float snapX = Math.round(eye.getX() / MAJOR_STEP) * (float) MAJOR_STEP;
        float snapZ = Math.round(eye.getZ() / MAJOR_STEP) * (float) MAJOR_STEP;
        Mat4 follow = Mat4.translation(new Vec3(snapX, 0, snapZ));

        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthMask(false);

        shader.use(gl);
        shader.setMat4(gl, "uViewProj", context.viewProjection());
        shader.setVec3(gl, "uCameraPos", eye.getX(), eye.getY(), eye.getZ());
        // Fade further out when the camera is high up, so the grid doesn't vanish from above.
        shader.setFloat(gl, "uFadeDistance", HALF_EXTENT * 0.8f + Math.abs(eye.getY()) * 2f);

        shader.setMat4(gl, "uModel", follow);
        shader.setVec4(gl, "uColor", 0.38f, 0.40f, 0.44f, 0.35f);
        minorLines.draw(gl);
        shader.setVec4(gl, "uColor", 0.50f, 0.53f, 0.58f, 0.7f);
        majorLines.draw(gl);

        shader.setMat4(gl, "uModel", Mat4.identity());
        shader.setVec4(gl, "uColor", 0.86f, 0.30f, 0.30f, 1f);
        xAxis.draw(gl);
        shader.setVec4(gl, "uColor", 0.30f, 0.50f, 0.92f, 1f);
        zAxis.draw(gl);

        gl.glDepthMask(true);
        gl.glDisable(GL3.GL_BLEND);
    }

    @Override
    public void dispose(GL3 gl) {
        if (shader == null) {
            return;
        }
        shader.dispose(gl);
        minorLines.dispose(gl);
        majorLines.dispose(gl);
        xAxis.dispose(gl);
        zAxis.dispose(gl);
    }

    // Lines on the axes themselves are skipped; the axis meshes draw over them.
    private static MeshData gridLines(boolean major) {
        List<float[]> segments = new ArrayList<>();

        for (int i = -HALF_EXTENT; i <= HALF_EXTENT; i++) {
            if (i == 0 || (i % MAJOR_STEP == 0) != major) {
                continue;
            }
            segments.add(new float[] { i, -HALF_EXTENT, i, HALF_EXTENT });
            segments.add(new float[] { -HALF_EXTENT, i, HALF_EXTENT, i });
        }

        float[] positions = new float[segments.size() * 6];
        int[] indices = new int[segments.size() * 2];

        for (int s = 0; s < segments.size(); s++) {
            float[] seg = segments.get(s);
            positions[s * 6] = seg[0];
            positions[s * 6 + 2] = seg[1];
            positions[s * 6 + 3] = seg[2];
            positions[s * 6 + 5] = seg[3];
            indices[s * 2] = s * 2;
            indices[s * 2 + 1] = s * 2 + 1;
        }

        return MeshData.lines(positions, indices);
    }

    private static MeshData line(float x1, float z1, float x2, float z2) {
        return MeshData.lines(new float[] { x1, 0, z1, x2, 0, z2 }, new int[] { 0, 1 });
    }
}
