package org.halalfoursome.kebabeditor.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.AlphaMode;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.model.TextureSlot;

import com.jogamp.opengl.GL3;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ArchetypeIconRenderer implements RenderPass {

    private static final float BACKDROP_SCALE = 1.3f;
    private static final Material BACKDROP = new Material(
        new Color(Palette.SELECTION.getRed(), Palette.SELECTION.getGreen(), Palette.SELECTION.getBlue(), 150),
        TextureSlot.NONE,
        AlphaMode.BLEND,
        true
    );

    private final DrawableRenderer drawables;
    private final ArchetypeIcons icons;
    private final BooleanSupplier visible;

    @Override
    public void init(GL3 gl) {}

    @Override
    public void render(GL3 gl, RenderContext context) {
        if (!visible.getAsBoolean() || context.scene().isNone()) {
            return;
        }

        List<IconLayout.Placement> placements = IconLayout.of(
            context.scene().unwrap(),
            context.camera(),
            context.width(),
            context.height(),
            icons
        );

        if (placements.isEmpty()) {
            return;
        }

        // Icons ignore depth, so they overlap in drawing order: far ones first, near ones on top
        placements.sort(Comparator.comparingDouble(IconLayout.Placement::depth).reversed());

        Vec3 right = context.camera().right();
        Vec3 up = context.camera().up();
        List<Drawable> quads = new ArrayList<>();

        for (IconLayout.Placement placement : placements) {
            if (context.isSelected(placement.node())) {
                Mat4 backdrop = billboard(placement.center(), placement.worldSize() * BACKDROP_SCALE, right, up);
                quads.add(new Drawable(Meshes.QUAD, backdrop, BACKDROP, DrawMode.OVERLAY));
            }

            Material material = new Material(Color.WHITE, placement.texture(), AlphaMode.BLEND, true);
            Mat4 world = billboard(placement.center(), placement.worldSize(), right, up);
            quads.add(new Drawable(Meshes.QUAD, world, material, DrawMode.OVERLAY));
        }

        drawables.draw(gl, context, quads, false);
    }

    @Override
    public void dispose(GL3 gl) {}

    // Columns are the camera's right and up axes scaled to the size, and the axis pointing back at the camera
    static Mat4 billboard(Vec3 center, float size, Vec3 right, Vec3 up) {
        Vec3 back = right.cross(up);

        return new Mat4().set(
            right.getX() * size, right.getY() * size, right.getZ() * size, 0,
            up.getX() * size, up.getY() * size, up.getZ() * size, 0,
            back.getX(), back.getY(), back.getZ(), 0,
            center.getX(), center.getY(), center.getZ(), 1
        );
    }
}
