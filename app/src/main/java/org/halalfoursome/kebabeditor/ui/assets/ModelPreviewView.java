package org.halalfoursome.kebabeditor.ui.assets;

import java.util.List;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.project.EditorSessions;
import org.halalfoursome.kebabeditor.render.DrawableRenderer;
import org.halalfoursome.kebabeditor.render.RenderContext;
import org.halalfoursome.kebabeditor.render.RenderPass;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.ResolvedModel;
import org.halalfoursome.kebabeditor.ui.EditorView;

import com.jogamp.opengl.GL3;

public final class ModelPreviewView extends EditorView {

    private static final float MIN_RADIUS = 0.5f;
    private static final float DISTANCE_IN_RADII = 3f;

    public ModelPreviewView(ResolvedModel model) {
        super(EditorSessions.create(), false);
        List<Drawable> objects = model.parts()
            .stream()
            .map(part -> new Drawable(part.mesh(), part.transform(), part.material(), DrawMode.SOLID))
            .toList();
        DrawableRenderer renderer = drawables();
        addPass(new RenderPass() {
            @Override
            public void init(GL3 gl) {}

            @Override
            public void render(GL3 gl, RenderContext context) {
                renderer.draw(gl, context, objects, false);
            }

            @Override
            public void dispose(GL3 gl) {}
        });
        frame(model);
    }

    private void frame(ResolvedModel model) {
        var bounds = model.bounds();
        Vec3 size = new Vec3(
            bounds.getMaxX() - bounds.getMinX(),
            bounds.getMaxY() - bounds.getMinY(),
            bounds.getMaxZ() - bounds.getMinZ()
        );
        Vec3 center = new Vec3(
            (bounds.getMinX() + bounds.getMaxX()) / 2,
            (bounds.getMinY() + bounds.getMaxY()) / 2,
            (bounds.getMinZ() + bounds.getMaxZ()) / 2
        );
        float radius = Math.max(MIN_RADIUS, size.length() / 2);
        getCamera().getPosition().set(center.sub(getCamera().forward().mulLocal(radius * DISTANCE_IN_RADII)));
        getCamera().setFar(Math.max(2000, radius * 20));
    }
}
