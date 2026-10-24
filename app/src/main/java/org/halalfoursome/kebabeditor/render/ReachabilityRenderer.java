package org.halalfoursome.kebabeditor.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.playtest.reach.ReachabilityMarks;
import org.halalfoursome.kebabeditor.playtest.reach.ReachabilityReport;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.jogamp.opengl.GL3;

/** Boxes around the platforms of the last reachability check, coloured by status, and the way it found. */
public class ReachabilityRenderer implements RenderPass {

    /** Boxes sit this far outside the platform, so its faces do not hide them. */
    private static final float INFLATE = 0.03f;
    /** Lifted off the platform tops so the way does not sink into them. */
    private static final float LIFT = 0.05f;
    private static final float LINE_WIDTH = 2f;
    private static final Color WAY = new Color(235, 235, 235);

    private final DrawableRenderer drawableObjects;
    private final ReachabilityMarks marks;

    private Maybe<ReachabilityReport> built = Maybe.none();
    private Maybe<MeshData> way = Maybe.none();

    public ReachabilityRenderer(DrawableRenderer drawableObjects, ReachabilityMarks marks) {
        this.drawableObjects = drawableObjects;
        this.marks = marks;
    }

    @Override
    public void render(GL3 gl, RenderContext context) {
        Maybe<ReachabilityReport> report = marks.report();

        // A new check replaces the way of the old one.
        if (!sameReport(report, built)) {
            way.ifSome(drawableObjects::release);
            way = report.andThen(r -> wayMesh(r.path()));
            built = report;
        }

        if (report.isNone()) {
            return;
        }

        List<Drawable> drawables = new ArrayList<>();
        for (ReachabilityReport.Mark mark : report.unwrap().marks()) {
            drawables.add(new Drawable(
                Meshes.CUBE_LINES,
                boxAround(mark.platform().bounds()),
                Material.solid(color(mark.status())),
                DrawMode.SOLID,
                LINE_WIDTH
            ));
        }
        way.ifSome(mesh -> drawables.add(
            new Drawable(mesh, Mat4.identity(), Material.solid(WAY), DrawMode.SOLID, LINE_WIDTH)
        ));

        drawableObjects.draw(gl, context, drawables, false);
    }

    static Color color(ReachabilityReport.Status status) {
        return switch (status) {
            case START -> new Color(70, 190, 255);
            case REACHABLE -> new Color(110, 230, 110);
            case ONE_WAY -> new Color(255, 220, 70);
            case UNREACHABLE -> new Color(240, 90, 90);
        };
    }

    private static Mat4 boxAround(Aabb bounds) {
        Vec3 center = new Vec3(
            (bounds.getMinX() + bounds.getMaxX()) / 2,
            (bounds.getMinY() + bounds.getMaxY()) / 2,
            (bounds.getMinZ() + bounds.getMaxZ()) / 2
        );
        Vec3 size = new Vec3(
            bounds.getMaxX() - bounds.getMinX() + 2 * INFLATE,
            bounds.getMaxY() - bounds.getMinY() + 2 * INFLATE,
            bounds.getMaxZ() - bounds.getMinZ() + 2 * INFLATE
        );
        return Mat4.translation(center).mul(Mat4.scaling(size));
    }

    // One line through every take-off and landing in order.
    private static Maybe<MeshData> wayMesh(List<Vec3> points) {
        if (points.size() < 2) {
            return Maybe.none();
        }

        float[] positions = new float[points.size() * 3];
        for (int i = 0; i < points.size(); i++) {
            Vec3 p = points.get(i);
            positions[i * 3] = p.getX();
            positions[i * 3 + 1] = p.getY() + LIFT;
            positions[i * 3 + 2] = p.getZ();
        }

        int[] indices = new int[(points.size() - 1) * 2];
        for (int i = 0; i + 1 < points.size(); i++) {
            indices[i * 2] = i;
            indices[i * 2 + 1] = i + 1;
        }

        return Maybe.some(MeshData.lines(positions, indices));
    }

    private static boolean sameReport(Maybe<ReachabilityReport> a, Maybe<ReachabilityReport> b) {
        return a.isNone() && b.isNone()
            || a.isSome() && b.isSomeAnd(report -> report == a.unwrap());
    }

    @Override
    public void init(GL3 gl) {
        // Uses the shared DrawableRenderer, which the viewport initializes.
    }

    @Override
    public void dispose(GL3 gl) {
        // GPU meshes are owned by the shared DrawableRenderer.
        built = Maybe.none();
        way = Maybe.none();
    }
}
