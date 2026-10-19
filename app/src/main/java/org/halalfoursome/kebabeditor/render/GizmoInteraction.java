package org.halalfoursome.kebabeditor.render;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.function.Supplier;

import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Ray;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Dragging the gizmo's handles to move, rotate and scale the selected node.
 */
public final class GizmoInteraction {

    /** What the interaction needs from the viewport. */
    public interface Viewport {
        Camera camera();

        Ray rayAt(int x, int y);

        Maybe<Point2D.Float> project(Vec3 world);
    }

    private static final float GRAB_PIXELS = 9f;
    private static final float CENTER_GRAB_PIXELS = 12f;
    private static final int RING_SAMPLES = 64;
    private static final float MIN_RING_FACING = 0.15f;
    private static final float ROTATE_SNAP = (float) Math.toRadians(15);
    private static final float MIN_SCALE = 0.01f;
    private static final float MIN_SIZE = 0.05f;
    private static final float CENTER_SCALE_PIXELS = 150f;

    private final GizmoRenderer renderer;
    private final Supplier<Maybe<Float>> snapStep;

    private Maybe<Drag> drag = Maybe.none();

    /** Everything captured when a drag starts, so each move is relative to the start, not the last step. */
    private record Drag(
        SceneNode node,
        GizmoHandle handle,
        GizmoMode mode,
        GizmoFrame frame,
        Transform startLocal,
        Quat startWorldRotation,
        Maybe<Greybox> startGreybox,
        Vec3 startHit,
        float startParam,
        int startX,
        int startY
    ) {}

    /** @param snapStep the grid step, or none while snapping is off */
    public GizmoInteraction(GizmoRenderer renderer, Supplier<Maybe<Float>> snapStep) {
        this.renderer = renderer;
        this.snapStep = snapStep;
    }

    public boolean isDragging() {
        return drag.isSome();
    }

    /** Highlights the handle under the cursor. */
    public void hover(Viewport viewport, Maybe<SceneNode> node, int x, int y) {
        if (isDragging()) {
            return;
        }
        renderer.setHighlight(node.andThen(n -> handleAt(viewport, n, x, y)));
    }

    /** Starts a drag if a handle of {@code node}'s gizmo is under the cursor. */
    public boolean press(Viewport viewport, Maybe<SceneNode> target, int x, int y) {
        if (target.isNone()) {
            return false;
        }

        SceneNode node = target.unwrap();
        Maybe<GizmoHandle> grabbed = handleAt(viewport, node, x, y);
        if (grabbed.isNone()) {
            return false;
        }

        GizmoHandle handle = grabbed.unwrap();
        GizmoMode mode = renderer.getMode();
        GizmoFrame frame = GizmoFrame.of(node, mode, viewport.camera());
        Ray ray = viewport.rayAt(x, y);
        Vec3 center = frame.center();

        Vec3 startHit = center;
        float startParam = 0;

        switch (mode) {
            case TRANSLATE -> {
                if (handle.isPlane() || handle == GizmoHandle.CENTER) {
                    Maybe<Vec3> hit = rayPlane(ray, center, dragPlaneNormal(handle, frame, viewport.camera()));
                    if (hit.isNone()) {
                        return false;
                    }
                    startHit = hit.unwrap();
                } else {
                    startParam = axisParam(ray, center, frame.axis(handle.axis()));
                    if (Float.isNaN(startParam)) {
                        return false;
                    }
                }
            }
            case ROTATE -> {
                Maybe<Vec3> hit = rayPlane(ray, center, frame.axis(handle.axis()));
                if (hit.isNone()) {
                    return false; // ring seen edge-on
                }
                startHit = hit.unwrap();
            }
            case SCALE -> {
                if (handle != GizmoHandle.CENTER) {
                    startParam = axisParam(ray, center, frame.axis(handle.axis()));
                    if (Float.isNaN(startParam) || Math.abs(startParam) < 1e-4f) {
                        return false;
                    }
                }
            }
        }

        drag = Maybe.some(new Drag(
            node, handle, mode, frame,
            node.getTransform().copy(),
            Quat.fromMatrix(node.worldMatrix()),
            Greybox.of(node),
            startHit, startParam, x, y
        ));
        renderer.setHighlight(Maybe.some(handle));
        return true;
    }

    public void drag(Viewport viewport, int x, int y) {
        if (drag.isNone()) {
            return;
        }

        Drag d = drag.unwrap();
        Ray ray = viewport.rayAt(x, y);

        switch (d.mode()) {
            case TRANSLATE -> translate(d, ray, viewport.camera());
            case ROTATE -> rotate(d, ray);
            case SCALE -> scale(d, ray, x, y);
        }
    }

    /** Ends the drag. @return whether a drag was in progress */
    public boolean release() {
        if (drag.isNone()) {
            return false;
        }

        Drag d = drag.unwrap();
        drag = Maybe.none();
        renderer.setHighlight(Maybe.none());

        Transform t = d.node().getTransform();
        String what = switch (d.mode()) {
            case TRANSLATE -> "Moved '" + d.node().getName() + "' to " + t.getTranslation();
            case ROTATE -> "Rotated '" + d.node().getName() + "'";
            case SCALE -> "Scaled '" + d.node().getName() + "'"
                + Greybox.of(d.node()).map(g -> " to size " + g.size()).unwrapOr(" to " + t.getScale());
        };
        LogService.get().info(what);
        return true;
    }

    /** Ends the drag and puts the node back as it was. */
    public void cancel() {
        drag.ifSome(d -> {
            d.node().setTransform(d.startLocal().copy());
            d.startGreybox().ifSome(g -> g.writeTo(d.node()));
        });
        drag = Maybe.none();
        renderer.setHighlight(Maybe.none());
    }

    private void translate(Drag d, Ray ray, Camera camera) {
        GizmoFrame frame = d.frame();
        Vec3 target;

        if (d.handle().isPlane() || d.handle() == GizmoHandle.CENTER) {
            Maybe<Vec3> hit = rayPlane(ray, frame.center(), dragPlaneNormal(d.handle(), frame, camera));
            if (hit.isNone()) {
                return;
            }
            target = frame.center().add(hit.unwrap().sub(d.startHit()));
        } else {
            Vec3 axis = frame.axis(d.handle().axis());
            float param = axisParam(ray, frame.center(), axis);
            if (Float.isNaN(param)) {
                return;
            }
            target = frame.center().add(axis.mul(param - d.startParam()));
        }

        // Translate handles are world-aligned, so snap the world coordinates being dragged.
        Maybe<Float> step = snapStep.get();
        if (step.isSome()) {
            float s = step.unwrap();
            GizmoHandle.Axes moved = d.handle().movedAxes();
            target.set(
                moved.x() ? snap(target.getX(), s) : target.getX(),
                moved.y() ? snap(target.getY(), s) : target.getY(),
                moved.z() ? snap(target.getZ(), s) : target.getZ()
            );
        }

        setWorldPosition(d.node(), target);
    }

    private void rotate(Drag d, Ray ray) {
        GizmoFrame frame = d.frame();
        Vec3 axis = frame.axis(d.handle().axis());
        Maybe<Vec3> hit = rayPlane(ray, frame.center(), axis);
        if (hit.isNone()) {
            return;
        }

        Vec3 from = d.startHit().sub(frame.center());
        Vec3 to = hit.unwrap().sub(frame.center());
        float angle = (float) Math.atan2(axis.dot(from.cross(to)), from.dot(to));

        if (snapStep.get().isSome()) {
            angle = Math.round(angle / ROTATE_SNAP) * ROTATE_SNAP;
        }

        Quat world = Quat.fromAxisAngle(axis, angle).mulLocal(d.startWorldRotation()).normalizeLocal();
        Quat parent = d.node().getParent().map(p -> Quat.fromMatrix(p.worldMatrix())).unwrapOr(Quat.identity());
        d.node().getTransform().setRotation(parent.conjugate().mulLocal(world).normalizeLocal());
    }

    private void scale(Drag d, Ray ray, int x, int y) {
        float factor;
        if (d.handle() == GizmoHandle.CENTER) {
            factor = 1f + ((x - d.startX()) - (y - d.startY())) / CENTER_SCALE_PIXELS;
        } else {
            float param = axisParam(ray, d.frame().center(), d.frame().axis(d.handle().axis()));
            if (Float.isNaN(param)) {
                return;
            }
            factor = param / d.startParam();
        }
        factor = Math.max(factor, MIN_SCALE);

        if (d.startGreybox().isSome()) {
            // Greyboxes keep a uniform node scale and change their per-axis size instead.
            Greybox start = d.startGreybox().unwrap();
            float[] size = start.size().toArray();
            for (int axis = 0; axis < 3; axis++) {
                if (d.handle() == GizmoHandle.CENTER || d.handle().axis() == axis) {
                    size[axis] = snapSize(size[axis] * factor);
                }
            }
            new Greybox(start.shape(), new Vec3(size[0], size[1], size[2]), start.color()).writeTo(d.node());
        } else {
            d.node().getTransform().setScale(Math.max(d.startLocal().getScale() * factor, MIN_SCALE));
        }
    }

    /** The handle under the cursor: the center first, then plane handles, then the nearest axis. */
    public Maybe<GizmoHandle> handleAt(Viewport viewport, SceneNode node, int x, int y) {
        GizmoMode mode = renderer.getMode();
        GizmoFrame frame = GizmoFrame.of(node, mode, viewport.camera());
        Point2D.Float mouse = new Point2D.Float(x, y);
        Maybe<Point2D.Float> center = viewport.project(frame.center());
        if (center.isNone()) {
            return Maybe.none();
        }

        if (mode != GizmoMode.ROTATE && center.unwrap().distance(mouse) <= CENTER_GRAB_PIXELS) {
            return Maybe.some(GizmoHandle.CENTER);
        }

        if (mode == GizmoMode.TRANSLATE) {
            Maybe<GizmoHandle> plane = planeAt(frame, viewport.rayAt(x, y));
            if (plane.isSome()) {
                return plane;
            }
        }

        GizmoHandle best = null;
        double bestDistance = GRAB_PIXELS;
        Ray ray = viewport.rayAt(x, y);

        for (int axis = 0; axis < 3; axis++) {
            if (mode == GizmoMode.ROTATE && Math.abs(frame.axis(axis).dot(ray.direction())) < MIN_RING_FACING) {
                continue;
            }
            double distance = mode == GizmoMode.ROTATE
                ? ringDistance(viewport, frame, axis, mouse)
                : segmentDistance(viewport,
                    frame.center().add(frame.axis(axis).mul(GizmoMeshes.HANDLE_START * frame.size())),
                    frame.center().add(frame.axis(axis).mul(frame.size())),
                    mouse);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = GizmoHandle.axis(axis);
            }
        }

        return Maybe.ofNullable(best);
    }

    private static Maybe<GizmoHandle> planeAt(GizmoFrame frame, Ray ray) {
        GizmoHandle best = null;
        float bestT = Float.POSITIVE_INFINITY;

        for (int k = 0; k < 3; k++) {
            Maybe<Vec3> hit = rayPlane(ray, frame.center(), frame.axis(k));
            if (hit.isNone()) {
                continue;
            }
            // Same axis pairing as GizmoMeshes.plane(u, v) in GizmoRenderer.
            Vec3 local = hit.unwrap().sub(frame.center());
            float a = local.dot(frame.axis((k + 1) % 3)) / frame.size();
            float b = local.dot(frame.axis((k + 2) % 3)) / frame.size();
            float t = hit.unwrap().distance(ray.origin());

            if (inside(a) && inside(b) && t < bestT) {
                bestT = t;
                best = GizmoHandle.plane(k);
            }
        }
        return Maybe.ofNullable(best);
    }

    private static boolean inside(float value) {
        return value >= GizmoMeshes.PLANE_START && value <= GizmoMeshes.PLANE_END;
    }

    private static double ringDistance(Viewport viewport, GizmoFrame frame, int axis, Point2D.Float mouse) {
        Vec3 u = frame.axis((axis + 1) % 3);
        Vec3 v = frame.axis((axis + 2) % 3);
        float radius = GizmoMeshes.RING_RADIUS * frame.size();
        double best = Double.POSITIVE_INFINITY;
        Vec3 previous = null;

        for (int i = 0; i <= RING_SAMPLES; i++) {
            double angle = 2 * Math.PI * i / RING_SAMPLES;
            Vec3 point = frame.center()
                .add(u.mul((float) Math.cos(angle) * radius))
                .addLocal(v.mul((float) Math.sin(angle) * radius));
            if (previous != null) {
                best = Math.min(best, segmentDistance(viewport, previous, point, mouse));
            }
            previous = point;
        }
        return best;
    }

    private static double segmentDistance(Viewport viewport, Vec3 a, Vec3 b, Point2D.Float mouse) {
        Maybe<Point2D.Float> pa = viewport.project(a);
        Maybe<Point2D.Float> pb = viewport.project(b);
        if (pa.isNone() || pb.isNone()) {
            return Double.POSITIVE_INFINITY;
        }
        return Line2D.ptSegDist(pa.unwrap().x, pa.unwrap().y, pb.unwrap().x, pb.unwrap().y, mouse.x, mouse.y);
    }

    private static Vec3 dragPlaneNormal(GizmoHandle handle, GizmoFrame frame, Camera camera) {
        return handle == GizmoHandle.CENTER ? camera.forward() : frame.axis(handle.axis());
    }

    /** Parameter along {@code center + axis * s} of the point closest to the ray; NaN if they are parallel. */
    static float axisParam(Ray ray, Vec3 center, Vec3 axis) {
        Vec3 d = ray.direction();
        Vec3 w0 = center.sub(ray.origin());
        float b = axis.dot(d);
        float denominator = 1f - b * b;
        if (denominator < 1e-4f) {
            return Float.NaN; // looking straight down the axis
        }
        return (b * d.dot(w0) - axis.dot(w0)) / denominator;
    }

    static Maybe<Vec3> rayPlane(Ray ray, Vec3 point, Vec3 normal) {
        float denominator = normal.dot(ray.direction());
        if (Math.abs(denominator) < 1e-5f) {
            return Maybe.none();
        }
        float t = normal.dot(point.sub(ray.origin())) / denominator;
        return t < 0 ? Maybe.none() : Maybe.some(ray.point(t));
    }

    private static void setWorldPosition(SceneNode node, Vec3 world) {
        try {
            Vec3 local = node.getParent()
                .map(p -> p.worldMatrix().inverse().transformPoint(world))
                .unwrapOr(world);
            node.getTransform().setTranslation(local);
        } catch (ArithmeticException e) {
            // A zero-scale parent has no inverse; leave the node where it is.
        }
    }

    private float snapSize(float size) {
        Maybe<Float> step = snapStep.get();
        if (step.isNone()) {
            return Math.max(size, MIN_SIZE);
        }
        float s = step.unwrap();
        return Math.max(snap(size, s), s);
    }

    private static float snap(float value, float step) {
        return Math.round(value / step) * step;
    }
}
