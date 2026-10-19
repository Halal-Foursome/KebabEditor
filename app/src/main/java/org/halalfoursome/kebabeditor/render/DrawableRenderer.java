package org.halalfoursome.kebabeditor.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.AlphaMode;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.jogamp.opengl.GL3;

/**
 * Draws {@link Drawable}s with the shared lit mesh shader. Not a pass itself:
 * scene passes collect drawableObjects and hand them over, so GPU meshes are shared
 * between passes.
 *
 * Within one call, opaque solids draw first, then blended solids back to
 * front, then overlays without depth testing.
 */
public final class DrawableRenderer {

    private static final Vec3 LIGHT_DIRECTION = new Vec3(-0.35f, -1f, -0.45f).normalize();

    private final GpuMeshCache meshes = new GpuMeshCache();
    private final TextureCache textures = new TextureCache();
    private ShaderProgram shader;

    public void init(GL3 gl) {
        shader = ShaderProgram.load(gl, "mesh");
    }

    public void draw(GL3 gl, RenderContext context, List<Drawable> drawableObjects, boolean worldGrid) {
        if (drawableObjects.isEmpty()) {
            return;
        }

        List<Drawable> opaque = new ArrayList<>();
        List<Drawable> blended = new ArrayList<>();
        List<Drawable> overlay = new ArrayList<>();

        for (Drawable drawable : drawableObjects) {
            if (drawable.mode() == DrawMode.OVERLAY) {
                overlay.add(drawable);
            } else if (drawable.material().alpha() == AlphaMode.BLEND) {
                blended.add(drawable);
            } else {
                opaque.add(drawable);
            }
        }

        Vec3 eye = context.cameraPosition();
        blended.sort(Comparator.comparingDouble(
            (Drawable d) -> d.world().getTranslation().distanceSquared(eye)
        ).reversed());

        shader.use(gl);
        shader.setMat4(gl, "uViewProj", context.viewProjection());
        shader.setVec3(gl, "uLightDir", LIGHT_DIRECTION.getX(), LIGHT_DIRECTION.getY(), LIGHT_DIRECTION.getZ());
        shader.setBool(gl, "uWorldGrid", worldGrid);
        shader.setInt(gl, "uTexture", 0);
        gl.glActiveTexture(GL3.GL_TEXTURE0);

        // Push filled faces back slightly so edge lines drawn on them win the depth test.
        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(1f, 1f);

        opaque.forEach(d -> drawOne(gl, d));

        gl.glEnable(GL3.GL_BLEND);
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthMask(false);
        blended.forEach(d -> drawOne(gl, d));
        gl.glDepthMask(true);

        gl.glDisable(GL3.GL_DEPTH_TEST);
        overlay.forEach(d -> drawOne(gl, d));
        gl.glEnable(GL3.GL_DEPTH_TEST);

        gl.glDisable(GL3.GL_BLEND);
        gl.glDisable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
    }

    private void drawOne(GL3 gl, Drawable drawable) {
        GpuMesh gpu = meshes.get(gl, drawable.mesh());
        Material material = drawable.material();
        Color color = material.baseColor();

        shader.setMat4(gl, "uModel", drawable.world());
        shader.setMat4(gl, "uNormalMatrix", normalMatrix(drawable.world()));
        shader.setVec4(gl, "uColor",
            color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        shader.setBool(gl, "uLit", !gpu.isLines() && drawable.mesh().hasNormals());

        Maybe<Integer> texture = drawable.mesh().hasUvs()
            ? textures.get(gl, material.baseTexture())
            : Maybe.none();
        shader.setBool(gl, "uTextured", texture.isSome());
        texture.ifSome(id -> gl.glBindTexture(GL3.GL_TEXTURE_2D, id));

        if (material.doubleSided()) {
            gl.glDisable(GL3.GL_CULL_FACE);
        } else {
            gl.glEnable(GL3.GL_CULL_FACE);
        }

        gpu.draw(gl);
    }

    // Inverse-transpose keeps normals perpendicular under non-uniform scale.
    private static Mat4 normalMatrix(Mat4 world) {
        try {
            return world.inverse().transposeLocal();
        } catch (ArithmeticException e) {
            return world;
        }
    }

    public void endFrame(GL3 gl) {
        meshes.evictUnused(gl);
    }

    public void dispose(GL3 gl) {
        meshes.dispose(gl);
        textures.dispose(gl);
        if (shader != null) {
            shader.dispose(gl);
        }
    }
}
