package org.halalfoursome.kebabeditor.render;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.playtest.PathRecording;

import com.jogamp.opengl.GL3;

import lombok.RequiredArgsConstructor;

/** The path rays of past playtests; the newest is bright, older ones are dimmed. */
@RequiredArgsConstructor
public class PathRayRenderer implements RenderPass {

    private final DrawableRenderer drawableObjects;
    private final PathRays rays;
    private final Map<PathRecording, PathRayMeshes> built = new IdentityHashMap<>();

    @Override
    public void render(GL3 gl, RenderContext context) {
        List<PathRecording> recordings = rays.recordings();

        // Free the meshes of recordings that were cleared or pushed out.
        built.entrySet().removeIf(entry -> {
            boolean gone = recordings.stream().noneMatch(r -> r == entry.getKey());
            if (gone) {
                entry.getValue().release(drawableObjects);
            }
            return gone;
        });

        if (!rays.isVisible() || recordings.isEmpty()) {
            return;
        }

        List<Drawable> drawables = new ArrayList<>();
        for (int i = 0; i < recordings.size(); i++) {
            PathRecording recording = recordings.get(i);
            PathRayMeshes meshes = built.computeIfAbsent(recording, r -> PathRayMeshes.of(r, true));
            drawables.addAll(meshes.drawables(i == recordings.size() - 1));
        }

        drawableObjects.draw(gl, context, drawables, false);
    }

    @Override
    public void init(GL3 gl) {
        // Uses the shared DrawableRenderer, which the viewport initializes.
    }

    @Override
    public void dispose(GL3 gl) {
        // GPU meshes are owned by the shared DrawableRenderer.
        built.clear();
    }
}
