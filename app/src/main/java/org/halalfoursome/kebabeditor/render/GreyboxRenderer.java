package org.halalfoursome.kebabeditor.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;

import com.jogamp.opengl.GL3;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
public class GreyboxRenderer implements RenderPass {

    private final DrawableRenderer drawableObjects;

    @Override
    public void render(GL3 gl, RenderContext context) {
        if (context.scene().isNone()) {
            return;
        }

        List<Drawable> solids = new ArrayList<>();
        List<Drawable> edges = new ArrayList<>();

        context.scene().unwrap().traverse().forEach(node ->
            Greybox.of(node).ifSome(greybox -> collect(context, node, greybox, solids, edges))
        );

        drawableObjects.draw(gl, context, solids, true);
        drawableObjects.draw(gl, context, edges, false);
    }

    private static void collect(
        RenderContext context,
        SceneNode node,
        Greybox greybox,
        List<Drawable> solids,
        List<Drawable> edges
    ) {
        Mat4 world = node.worldMatrix().mul(Mat4.scaling(greybox.size()));
        boolean selected = context.isSelected(node);

        solids.add(new Drawable(solidMesh(greybox.shape()), world, Material.solid(greybox.color()), DrawMode.SOLID));
        edges.add(new Drawable(
            edgeMesh(greybox.shape()),
            world,
            Material.solid(selected ? Palette.SELECTION : edgeColor(greybox.color())),
            selected ? DrawMode.OVERLAY : DrawMode.SOLID
        ));
    }

    @Override
    public void dispose(GL3 gl) {
        // GPU meshes are owned by the shared DrawableRenderer.
    }

    @Override
    public void init(GL3 gl) {
        // Uses the shared DrawableRenderer, which the viewport initializes.
    }

    static MeshData solidMesh(GreyboxShape shape) {
        return switch (shape) {
            case BOX -> Meshes.CUBE_TRIANGLES;
            case SPHERE -> Meshes.SPHERE_TRIANGLES;
        };
    }

    static MeshData edgeMesh(GreyboxShape shape) {
        return switch (shape) {
            case BOX -> Meshes.CUBE_LINES;
            case SPHERE -> Meshes.SPHERE_LINES;
        };
    }

    private static Color edgeColor(Color base) {
        return new Color(
            (int) (base.getRed() * 0.45f),
            (int) (base.getGreen() * 0.45f),
            (int) (base.getBlue() * 0.45f),
            base.getAlpha()
        );
    }
}
