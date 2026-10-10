package org.halalfoursome.kebabeditor.behavior.instance;

import static org.halalfoursome.kebabeditor.behavior.instance.BehaviorTestSupport.EPS;
import static org.halalfoursome.kebabeditor.behavior.instance.BehaviorTestSupport.instanceOf;
import static org.halalfoursome.kebabeditor.behavior.instance.BehaviorTestSupport.nodeAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.junit.jupiter.api.Test;

class ColliderSphereBehaviorTest {

    private final ColliderSphereBehavior behavior = new ColliderSphereBehavior();

    @Test
    void drawnSphereHasTheRequestedWorldRadius() throws Exception {
        ArchetypeInstance sphere = instanceOf(BuiltinArchetypes.COLLIDER_SPHERE);
        sphere.set("radius", 1.5f);
        SceneNode node = nodeAt(new Vec3(1, 2, 3), 2f);

        List<Drawable> drawableObjects = behavior.drawableObjects(node, sphere, null);

        assertEquals(1, drawableObjects.size());
        assertSame(Meshes.SPHERE_LINES, drawableObjects.get(0).mesh());
        // The mesh has radius 0.5, so its +X point must land 3 (= 1.5 * 2) from the centre
        Vec3 point = drawableObjects.get(0).world().transformPoint(new Vec3(0.5f, 0, 0));
        assertTrue(point.approxEquals(new Vec3(4, 2, 3), EPS), point.toString());
    }

    @Test
    void boundsAreCentreAndScaledRadius() throws Exception {
        ArchetypeInstance sphere = instanceOf(BuiltinArchetypes.COLLIDER_SPHERE);
        sphere.set("radius", 1.5f);

        Aabb bounds = behavior.bounds(nodeAt(new Vec3(1, 2, 3), 2f), sphere).unwrap();

        assertEquals(-2f, bounds.getMinX(), EPS);
        assertEquals(4f, bounds.getMaxX(), EPS);
        assertEquals(-1f, bounds.getMinY(), EPS);
        assertEquals(5f, bounds.getMaxY(), EPS);
        assertEquals(0f, bounds.getMinZ(), EPS);
        assertEquals(6f, bounds.getMaxZ(), EPS);
    }

    @Test
    void selectionShapeIsLocalSphere() throws Exception {
        ArchetypeInstance sphere = instanceOf(BuiltinArchetypes.COLLIDER_SPHERE);
        sphere.set("radius", 1.5f);

        GizmoShape shape = behavior.gizmoSelectionShape(nodeAt(Vec3.zero(), 4f), sphere).unwrap();

        assertEquals(new GizmoShape.GizmoSphere(1.5f), shape);
    }

    @Test
    void defaultZeroRadiusIsReportedAndNotDrawn() {
        ArchetypeInstance sphere = instanceOf(BuiltinArchetypes.COLLIDER_SPHERE);
        SceneNode node = nodeAt(Vec3.zero(), 1f);

        assertEquals(1, behavior.validate(node, sphere).size());
        assertTrue(behavior.drawableObjects(node, sphere, null).isEmpty());
        assertTrue(behavior.bounds(node, sphere).isNone());
    }

    @Test
    void positiveRadiusIsValid() throws Exception {
        ArchetypeInstance sphere = instanceOf(BuiltinArchetypes.COLLIDER_SPHERE);
        sphere.set("radius", 0.25f);

        assertTrue(behavior.validate(nodeAt(Vec3.zero(), 1f), sphere).isEmpty());
    }
}
