package org.halalfoursome.kebabeditor.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.AlphaMode;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.TextureSlot;

import com.jogamp.opengl.GL3;

/**
 * The placement tool's feedback: a half-transparent ghost of the object about
 * to be spawned, the pointer ray leading to it, and, while it floats, a drop
 * line to the ground so its height is readable.
 */
public class PlacementRenderer implements RenderPass {

    private static final Color GHOST = new Color(150, 205, 255, 110);
    private static final Color GHOST_EDGES = new Color(170, 220, 255);
    private static final Color RAY = new Color(90, 200, 255);
    private static final Color DROP_LINE = new Color(150, 160, 175);

    // A segment from the origin to +X, stretched onto any two points by lineBetween().
    private static final MeshData UNIT_LINE = MeshData.lines(new float[] { 0, 0, 0, 1, 0, 0 }, new int[] { 0, 1 });

    private final DrawableRenderer drawables;

    public PlacementRenderer(DrawableRenderer drawables) {
        this.drawables = drawables;
    }

    @Override
    public void init(GL3 gl) {
        // Uses the shared DrawableRenderer, which the viewport initializes.
    }

    @Override
    public void render(GL3 gl, RenderContext context) {
        if (context.placement().isNone()) {
            return;
        }

        PlacementPreview preview = context.placement().unwrap();
        Mat4 world = Mat4.translation(preview.center()).scaleLocal(preview.size());
        List<Drawable> ghost = new ArrayList<>();
        List<Drawable> lines = new ArrayList<>();

        ghost.add(new Drawable(
            GreyboxRenderer.solidMesh(preview.shape()), world,
            new Material(GHOST, TextureSlot.NONE, AlphaMode.BLEND, false), DrawMode.SOLID
        ));
        lines.add(new Drawable(GreyboxRenderer.edgeMesh(preview.shape()), world, Material.solid(GHOST_EDGES), DrawMode.OVERLAY));
        lines.add(line(preview.rayStart(), preview.target(), RAY, DrawMode.OVERLAY));

        Vec3 center = preview.center();
        if (!preview.onSurface() && Math.abs(center.getY()) > 1e-3f) {
            lines.add(line(center, new Vec3(center.getX(), 0, center.getZ()), DROP_LINE, DrawMode.SOLID));
        }

        drawables.draw(gl, context, ghost, false);
        drawables.draw(gl, context, lines, false);
    }

    private static Drawable line(Vec3 from, Vec3 to, Color color, DrawMode mode) {
        return new Drawable(UNIT_LINE, lineBetween(from, to), Material.solid(color), mode);
    }

    // Maps (0,0,0) to `from` and (1,0,0) to `to`; the other two axes are any unit
    // perpendiculars, which keeps the matrix invertible.
    private static Mat4 lineBetween(Vec3 from, Vec3 to) {
        Vec3 along = to.sub(from);
        Vec3 helper = Math.abs(along.normalize().getY()) < 0.9f ? Vec3.unitY() : Vec3.unitX();
        Vec3 side = along.cross(helper).normalizeLocal();
        Vec3 up = side.cross(along).normalizeLocal();

        return new Mat4(
            along.getX(), along.getY(), along.getZ(), 0,
            side.getX(), side.getY(), side.getZ(), 0,
            up.getX(), up.getY(), up.getZ(), 0,
            from.getX(), from.getY(), from.getZ(), 1
        );
    }

    @Override
    public void dispose(GL3 gl) {
        // GPU meshes are owned by the shared DrawableRenderer.
    }
}
