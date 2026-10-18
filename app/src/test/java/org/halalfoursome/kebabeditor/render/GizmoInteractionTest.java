package org.halalfoursome.kebabeditor.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Ray;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;

class GizmoInteractionTest {

    private static final float EPS = 0.02f;

    /** A viewport of the default camera with the same projection math as EditorView. */
    private static final class FakeViewport implements GizmoInteraction.Viewport {
        final Camera camera = new Camera();
        final float width = 1200, height = 700;

        public Camera camera() {
            return camera;
        }

        public Ray rayAt(int x, int y) {
            return camera.ray(2f * x / width - 1f, 1f - 2f * y / height, width / height);
        }

        public Maybe<Point2D.Float> project(Vec3 p) {
            float[] m = camera.projection(width / height).mul(camera.view()).toArray();
            float x = m[0] * p.getX() + m[4] * p.getY() + m[8] * p.getZ() + m[12];
            float y = m[1] * p.getX() + m[5] * p.getY() + m[9] * p.getZ() + m[13];
            float w = m[3] * p.getX() + m[7] * p.getY() + m[11] * p.getZ() + m[15];
            return w <= 1e-5f
                ? Maybe.none()
                : Maybe.some(new Point2D.Float((x / w + 1f) / 2f * width, (1f - y / w) / 2f * height));
        }

        int[] pixel(Vec3 world) {
            Point2D.Float p = project(world).unwrap();
            return new int[] { Math.round(p.x), Math.round(p.y) };
        }
    }

    private final FakeViewport viewport = new FakeViewport();
    private final GizmoRenderer renderer = new GizmoRenderer();
    private Maybe<Float> snap = Maybe.some(0.5f);
    private final GizmoInteraction gizmo = new GizmoInteraction(renderer, () -> snap);

    private GizmoFrame frame(SceneNode node) {
        return GizmoFrame.of(node, renderer.getMode(), viewport.camera());
    }

    // Presses at `from` and drags to `to`, both world points.
    private void drag(SceneNode node, Vec3 from, Vec3 to) {
        int[] a = viewport.pixel(from);
        int[] b = viewport.pixel(to);
        assertTrue(gizmo.press(viewport, Maybe.some(node), a[0], a[1]), "press should grab a handle");
        gizmo.drag(viewport, b[0], b[1]);
        gizmo.release();
    }

    @Test
    void pressingAwayFromTheGizmoGrabsNothing() {
        SceneNode node = new SceneNode("n");
        assertFalse(gizmo.press(viewport, Maybe.some(node), 5, 5));
        assertFalse(gizmo.press(viewport, Maybe.none(), 600, 350));
    }

    @Test
    void movingAlongXSnapsToTheGrid() {
        SceneNode node = new SceneNode("n");
        GizmoFrame f = frame(node);
        Vec3 grab = f.axis(0).mul(0.6f * f.size());

        drag(node, grab, grab.add(new Vec3(2.3f, 0, 0)));

        assertTrue(new Vec3(2.5f, 0, 0).approxEquals(node.getTransform().getTranslation(), 1e-4f),
            "was " + node.getTransform().getTranslation());
    }

    @Test
    void movingWithoutSnappingFollowsTheMouse() {
        snap = Maybe.none();
        SceneNode node = new SceneNode("n");
        GizmoFrame f = frame(node);
        Vec3 grab = f.axis(1).mul(0.6f * f.size());

        drag(node, grab, grab.add(new Vec3(0, 1.3f, 0)));

        assertEquals(1.3f, node.getTransform().getTranslation().getY(), EPS);
        assertEquals(0f, node.getTransform().getTranslation().getX(), 1e-6f);
    }

    @Test
    void movingAChildKeepsItsWorldPositionConsistent() throws Exception {
        Scene scene = new Scene();
        SceneNode parent = new SceneNode("parent");
        parent.setTransform(new Transform(new Vec3(10, 0, 0), Quat.fromAxisAngle(Vec3.unitY(), (float) Math.PI / 2), 1f));
        SceneNode child = new SceneNode("child");
        scene.add(parent);
        scene.add(child, parent);

        GizmoFrame f = frame(child);
        Vec3 grab = f.center().add(f.axis(0).mul(0.6f * f.size()));
        drag(child, grab, grab.add(new Vec3(2f, 0, 0)));

        assertTrue(new Vec3(12, 0, 0).approxEquals(child.worldMatrix().getTranslation(), 1e-3f),
            "world " + child.worldMatrix().getTranslation());
    }

    @Test
    void rotatingSnapsTo15Degrees() {
        renderer.setMode(GizmoMode.ROTATE);
        SceneNode node = new SceneNode("n");
        GizmoFrame f = frame(node);
        float radius = GizmoMeshes.RING_RADIUS * f.size();
        double angle = Math.toRadians(50);

        // The Y ring spans Z and X; sweep 50 degrees from +Z towards +X.
        Vec3 from = f.axis(2).mul(radius);
        Vec3 to = f.axis(2).mul((float) Math.cos(angle) * radius).add(f.axis(0).mul((float) Math.sin(angle) * radius));
        drag(node, from, to);

        Vec3 expected = Quat.fromAxisAngle(Vec3.unitY(), (float) Math.toRadians(45)).rotate(Vec3.unitX());
        Vec3 actual = node.getTransform().getRotation().rotate(Vec3.unitX());
        assertTrue(expected.approxEquals(actual, 1e-3f), "expected " + expected + " but was " + actual);
    }

    @Test
    void scalingAGreyboxChangesItsSizeOnThatAxisOnly() {
        renderer.setMode(GizmoMode.SCALE);
        SceneNode node = new SceneNode("box");
        Greybox.unit(GreyboxShape.BOX).writeTo(node);
        GizmoFrame f = frame(node);
        Vec3 tip = f.axis(0).mul(0.94f * f.size());

        drag(node, tip, tip.mul(2.6f));

        Greybox scaled = Greybox.of(node).unwrap();
        assertTrue(new Vec3(2.5f, 1, 1).approxEquals(scaled.size(), 1e-4f), "size " + scaled.size());
        assertEquals(1f, node.getTransform().getScale());
    }

    @Test
    void scaleNeverReachesZero() {
        renderer.setMode(GizmoMode.SCALE);
        SceneNode node = new SceneNode("box");
        Greybox.unit(GreyboxShape.BOX).writeTo(node);
        GizmoFrame f = frame(node);
        Vec3 tip = f.axis(1).mul(0.94f * f.size());

        drag(node, tip, tip.mul(-3f)); // dragged through the center to the far side

        assertEquals(0.5f, Greybox.of(node).unwrap().size().getY(), 1e-6f);
    }

    @Test
    void scalingOtherNodesIsUniformAndClamped() {
        renderer.setMode(GizmoMode.SCALE);
        snap = Maybe.none();
        SceneNode node = new SceneNode("model");
        GizmoFrame f = frame(node);
        Vec3 tip = f.axis(0).mul(0.94f * f.size());

        drag(node, tip, tip.mul(2f));
        assertEquals(2f, node.getTransform().getScale(), 0.05f);

        // Handles keep their size on screen, so the tip is where it was.
        f = frame(node);
        tip = f.axis(0).mul(0.94f * f.size());
        drag(node, tip, tip.mul(-1f));
        assertTrue(node.getTransform().getScale() > 0f);
    }

    @Test
    void cancelPutsTheNodeBack() {
        SceneNode node = new SceneNode("n");
        node.getTransform().setTranslation(new Vec3(0.25f, 0, 0));
        Mat4 before = node.worldMatrix();
        GizmoFrame f = frame(node);
        Vec3 grab = f.center().add(f.axis(0).mul(0.6f * f.size()));

        int[] a = viewport.pixel(grab);
        int[] b = viewport.pixel(grab.add(new Vec3(3, 0, 0)));
        assertTrue(gizmo.press(viewport, Maybe.some(node), a[0], a[1]));
        gizmo.drag(viewport, b[0], b[1]);
        gizmo.cancel();

        assertTrue(before.getTranslation().approxEquals(node.worldMatrix().getTranslation(), 1e-6f));
        assertFalse(gizmo.isDragging());
    }
}
