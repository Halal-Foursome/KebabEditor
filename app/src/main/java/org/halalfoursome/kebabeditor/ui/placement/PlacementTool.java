package org.halalfoursome.kebabeditor.ui.placement;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.math.Ray;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.render.Camera;
import org.halalfoursome.kebabeditor.render.PickHit;
import org.halalfoursome.kebabeditor.render.Picker;
import org.halalfoursome.kebabeditor.render.PlacementPreview;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.Getter;

/**
 * Blockout placement: a ray from the camera through the cursor carries a ghost
 * of the next greybox. It rests on whatever surface the ray hits (ground
 * included); with nothing hit, or while Alt is held, it floats at a distance
 * set with the mouse wheel. A click spawns the object there.
 *
 * With snapping on, the ghost snaps to the scene's snap distance on every axis,
 * the vertical one included. On a surface, the axis along the surface normal is
 * left alone so the object keeps resting on it.
 */
public class PlacementTool {

    private static final float MIN_DISTANCE = 1f;
    private static final float MAX_DISTANCE = 200f;
    private static final float WHEEL_FACTOR = 1.15f;

    private final EditorView view;
    private final Editor editor;
    private final Picker picker;
    private final List<Runnable> listeners = new ArrayList<>();

    @Getter
    private boolean active;
    @Getter
    private GreyboxShape shape = GreyboxShape.BOX;
    @Getter
    private float distance = 8f;

    private Maybe<Point> cursor = Maybe.none();
    private boolean surfacesIgnored;

    public PlacementTool(EditorView view, Editor editor, Picker picker) {
        this.view = view;
        this.editor = editor;
        this.picker = picker;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            notifyListeners();
        }
    }

    public void setShape(GreyboxShape shape) {
        if (this.shape != shape) {
            this.shape = shape;
            notifyListeners();
        }
    }

    /** Called when the tool's mode or shape changes, e.g. to keep menu items in sync. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    // ---- Input, fed by the viewport ----

    public void cursorMoved(int x, int y) {
        cursor = Maybe.some(new Point(x, y));
    }

    public void cursorLeft() {
        cursor = Maybe.none();
    }

    public void setSurfacesIgnored(boolean ignored) {
        surfacesIgnored = ignored;
    }

    /** Wheel away from the user pushes the ghost further out. */
    public void wheel(double rotation) {
        distance = (float) Math.clamp(distance * Math.pow(WHEEL_FACTOR, -rotation), MIN_DISTANCE, MAX_DISTANCE);
    }

    /** Spawns the previewed object; the new node becomes selected. */
    public void place() {
        preview().ifSome(p -> editor.placeGreybox(p.shape(), p.center()));
    }

    // ---- Preview ----

    /** What would be spawned right now, or none when inactive or the cursor is outside the view. */
    public Maybe<PlacementPreview> preview() {
        if (!active || cursor.isNone() || editor.scene().isNone()) {
            return Maybe.none();
        }

        Scene scene = editor.scene().unwrap();
        Ray ray = view.rayAt(cursor.unwrap().x, cursor.unwrap().y);
        Vec3 size = Greybox.unit(shape).size();
        Maybe<Float> step = editor.snapStep();

        Maybe<PickHit> hit = surfacesIgnored ? Maybe.none() : picker.pick(scene, view.behaviorContext(), ray, true);

        if (hit.isNone()) {
            Vec3 center = ray.point(distance);
            step.ifSome(s -> center.set(snap(center.getX(), s), snap(center.getY(), s), snap(center.getZ(), s)));
            return Maybe.some(new PlacementPreview(shape, size, center, rayStart(), center, false));
        }

        PickHit surface = hit.unwrap();
        Vec3 normal = surface.normal();
        Vec3 center = surface.point().add(normal.mul(halfExtentAlong(normal, size)));
        step.ifSome(s -> snapAlongSurface(center, normal, s));

        return Maybe.some(new PlacementPreview(shape, size, center, rayStart(), surface.point(), true));
    }

    // How far the shape reaches from its center towards the surface, so it rests on it.
    private float halfExtentAlong(Vec3 normal, Vec3 size) {
        if (shape == GreyboxShape.SPHERE) {
            return size.getX() / 2f;
        }
        return 0.5f * (Math.abs(normal.getX()) * size.getX()
            + Math.abs(normal.getY()) * size.getY()
            + Math.abs(normal.getZ()) * size.getZ());
    }

    // Just ahead of and below the eye, so the pointer ray is visible rather than seen end-on.
    private Vec3 rayStart() {
        Camera camera = view.getCamera();
        return camera.getPosition()
            .add(camera.forward().mulLocal(0.6f))
            .addLocal(camera.up().mulLocal(-0.3f));
    }

    /**
     * Snaps the axes running along the surface. The axis the surface faces is kept,
     * so the object stays in contact; slanted surfaces (e.g. on a sphere) are left unsnapped.
     */
    private static void snapAlongSurface(Vec3 center, Vec3 normal, float step) {
        float[] n = normal.toArray();
        float[] c = center.toArray();
        int facing = -1;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(n[axis]) > 0.99f) {
                facing = axis;
            }
        }
        if (facing < 0) {
            return;
        }
        for (int axis = 0; axis < 3; axis++) {
            if (axis != facing) {
                c[axis] = snap(c[axis], step);
            }
        }
        center.set(c[0], c[1], c[2]);
    }

    private static float snap(float value, float step) {
        return Math.round(value / step) * step;
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
