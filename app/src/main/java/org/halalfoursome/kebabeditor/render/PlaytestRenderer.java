package org.halalfoursome.kebabeditor.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.playtest.ActorSettings;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.jogamp.opengl.GL3;

/**
 * The running playtest: the actor's capsule with an arrow for where it faces,
 * and the trail it has left so far.
 */
public class PlaytestRenderer implements RenderPass {

    private static final Material BODY = Material.solid(new Color(235, 90, 120));
    private static final Material FACING = Material.solid(new Color(255, 255, 255));
    private static final int CAP_RINGS = 6;
    private static final int SEGMENTS = 24;
    /** The trail is rebuilt at most this often, so a long run does not re-upload it every frame. */
    private static final long TRAIL_REBUILD_NANOS = 100_000_000L;

    private final DrawableRenderer drawableObjects;
    private final Supplier<Maybe<PlaytestSession>> session;

    private PlaytestSession builtFor;
    private MeshData body;
    private PathRayMeshes trail;
    private int trailVersion;
    private long trailBuiltAt;

    public PlaytestRenderer(DrawableRenderer drawableObjects, Supplier<Maybe<PlaytestSession>> session) {
        this.drawableObjects = drawableObjects;
        this.session = session;
    }

    @Override
    public void render(GL3 gl, RenderContext context) {
        Maybe<PlaytestSession> current = session.get();
        if (current.isNone()) {
            forget();
            return;
        }

        PlaytestSession running = current.unwrap();
        ActorSettings settings = running.settings();
        if (running != builtFor) {
            forget();
            builtFor = running;
            body = Meshes.capsuleTriangles(settings.radius(), settings.height(), CAP_RINGS, SEGMENTS);
        }
        updateTrail(running);

        Mat4 world = Mat4.translation(running.actorPosition())
            .mul(Mat4.rotation(Quat.fromAxisAngle(Vec3.unitY(), running.actorYaw())));
        Mat4 arrow = world
            .mul(Mat4.translation(new Vec3(0, settings.height() * 0.6f, 0)))
            .mul(Mat4.scaling(settings.radius() * 2.5f));

        List<Drawable> drawables = new ArrayList<>();
        drawables.add(new Drawable(body, world, BODY, DrawMode.SOLID));
        drawables.add(new Drawable(Meshes.ARROW_LINES, arrow, FACING, DrawMode.OVERLAY));
        if (trail != null) {
            drawables.addAll(trail.drawables(true));
        }

        drawableObjects.draw(gl, context, drawables, false);
    }

    private void updateTrail(PlaytestSession running) {
        long now = System.nanoTime();
        int version = running.recorder().version();

        if (trail != null && (version == trailVersion || now - trailBuiltAt < TRAIL_REBUILD_NANOS)) {
            return;
        }

        PathRayMeshes previous = trail;
        trail = PathRayMeshes.of(running.recorder().snapshot(running.elapsed()), false);
        trailVersion = version;
        trailBuiltAt = now;
        if (previous != null) {
            previous.release(drawableObjects);
        }
    }

    private void forget() {
        if (body != null) {
            drawableObjects.release(body);
            body = null;
        }
        if (trail != null) {
            trail.release(drawableObjects);
            trail = null;
        }
        builtFor = null;
    }

    @Override
    public void init(GL3 gl) {
        // Uses the shared DrawableRenderer, which the viewport initializes.
    }

    @Override
    public void dispose(GL3 gl) {
        // GPU meshes are owned by the shared DrawableRenderer.
        body = null;
        trail = null;
        builtFor = null;
    }
}
