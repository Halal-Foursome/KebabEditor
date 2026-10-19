package org.halalfoursome.kebabeditor.render;

import java.awt.Color;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.jogamp.opengl.GL3;

import lombok.Getter;
import lombok.Setter;

/**
 * Transform gizmo for the primary selected node, 
 * drawn after everything else.
 */
public class GizmoRenderer implements RenderPass {

    private static final Color[] AXIS_COLORS = {
        new Color(232, 66, 80),
        new Color(124, 204, 52),
        new Color(64, 128, 240),
    };
    private static final Color CENTER_COLOR = new Color(235, 235, 235);
    private static final Color HIGHLIGHT = new Color(255, 214, 64);
    private static final float PLANE_ALPHA = 0.45f;

    // Rotations taking the +X-built handles onto X, Y and Z.
    private static final Mat4[] AXIS_ROTATIONS = {
        Mat4.identity(),
        Mat4.rotation(Quat.fromAxisAngle(Vec3.unitZ(), (float) Math.PI / 2)),
        Mat4.rotation(Quat.fromAxisAngle(Vec3.unitY(), (float) -Math.PI / 2)),
    };

    @Getter
    @Setter
    private GizmoMode mode = GizmoMode.TRANSLATE;

    /** The handle under the cursor or being dragged; drawn highlighted. */
    @Setter
    private Maybe<GizmoHandle> highlight = Maybe.none();

    private ShaderProgram shader;
    private GpuMesh arrow;
    private GpuMesh scaleHandle;
    private GpuMesh ring;
    private GpuMesh translateCenter;
    private GpuMesh scaleCenter;
    private GpuMesh[] planes;

    @Override
    public void init(GL3 gl) {
        shader = ShaderProgram.load(gl, "gizmo");
        arrow = GpuMesh.upload(gl, GizmoMeshes.arrow());
        scaleHandle = GpuMesh.upload(gl, GizmoMeshes.scaleHandle());
        ring = GpuMesh.upload(gl, GizmoMeshes.ring());
        translateCenter = GpuMesh.upload(gl, GizmoMeshes.centerCube(0.045f));
        scaleCenter = GpuMesh.upload(gl, GizmoMeshes.centerCube(0.075f));
        // Each plane handle takes the color of the axis it is perpendicular to.
        planes = new GpuMesh[] {
            GpuMesh.upload(gl, GizmoMeshes.plane(Vec3.unitY(), Vec3.unitZ())),
            GpuMesh.upload(gl, GizmoMeshes.plane(Vec3.unitZ(), Vec3.unitX())),
            GpuMesh.upload(gl, GizmoMeshes.plane(Vec3.unitX(), Vec3.unitY())),
        };
    }

    @Override
    public void render(GL3 gl, RenderContext context) {
        if (context.selection().isEmpty()) {
            return;
        }

        GizmoFrame frame = GizmoFrame.of(context.selection().getLast(), mode, context.camera());
        Mat4 base = frame.matrix();

        gl.glClear(GL3.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GL3.GL_CULL_FACE);

        shader.use(gl);
        shader.setMat4(gl, "uViewProj", context.viewProjection());
        shader.setMat4(gl, "uView", context.view());

        switch (mode) {
            case TRANSLATE -> {
                drawAxes(gl, base, arrow);
                draw(gl, base, translateCenter, color(GizmoHandle.CENTER, CENTER_COLOR), 1f);

                gl.glEnable(GL3.GL_BLEND);
                gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
                gl.glDepthMask(false);
                for (int axis = 0; axis < 3; axis++) {
                    GizmoHandle handle = GizmoHandle.plane(axis);
                    boolean lit = highlight.isSomeAnd(h -> h == handle);
                    draw(gl, base, planes[axis], color(handle, AXIS_COLORS[axis]), lit ? 0.8f : PLANE_ALPHA);
                }
                gl.glDepthMask(true);
                gl.glDisable(GL3.GL_BLEND);
            }
            case ROTATE -> drawAxes(gl, base, ring);
            case SCALE -> {
                drawAxes(gl, base, scaleHandle);
                draw(gl, base, scaleCenter, color(GizmoHandle.CENTER, CENTER_COLOR), 1f);
            }
        }

        gl.glEnable(GL3.GL_CULL_FACE);
    }

    private void drawAxes(GL3 gl, Mat4 base, GpuMesh handle) {
        for (int axis = 0; axis < 3; axis++) {
            draw(gl, base.mul(AXIS_ROTATIONS[axis]), handle, color(GizmoHandle.axis(axis), AXIS_COLORS[axis]), 1f);
        }
    }

    private Color color(GizmoHandle handle, Color normal) {
        return highlight.isSomeAnd(h -> h == handle) ? HIGHLIGHT : normal;
    }

    private void draw(GL3 gl, Mat4 model, GpuMesh mesh, Color color, float alpha) {
        shader.setMat4(gl, "uModel", model);
        shader.setVec4(gl, "uColor", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha);
        mesh.draw(gl);
    }

    @Override
    public void dispose(GL3 gl) {
        if (shader == null) {
            return;
        }
        shader.dispose(gl);
        arrow.dispose(gl);
        scaleHandle.dispose(gl);
        ring.dispose(gl);
        translateCenter.dispose(gl);
        scaleCenter.dispose(gl);
        for (GpuMesh plane : planes) {
            plane.dispose(gl);
        }
    }
}
