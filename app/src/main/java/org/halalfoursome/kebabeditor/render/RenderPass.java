package org.halalfoursome.kebabeditor.render;

import com.jogamp.opengl.GL3;

/**
 * One ordered stage of a viewport frame. All methods run on the thread that owns
 * the GL context, with that context current.
 */
public interface RenderPass {

    void init(GL3 gl);

    void render(GL3 gl, RenderContext context);

    void dispose(GL3 gl);
}
