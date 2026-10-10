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
import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.junit.jupiter.api.Test;

class ColliderBoxBehaviorTest {

    private final ColliderBoxBehavior behavior = new ColliderBoxBehavior();

    @Test
    void drawsUnitCubeScaledToSizeAndPlacedByNode() throws Exception {
        ArchetypeInstance box = instanceOf(BuiltinArchetypes.COLLIDER_BOX);
        box.set("size", new Vec3(1, 2, 3));
        SceneNode node = nodeAt(new Vec3(1, 2, 3), 2f);

        List<Drawable> drawableObjects = behavior.drawableObjects(node, box, null);

        assertEquals(1, drawableObjects.size());
        Drawable drawable = drawableObjects.get(0);
        assertSame(Meshes.CUBE_LINES, drawable.mesh());
        assertEquals(DrawMode.OVERLAY, drawable.mode());
        Vec3 corner = drawable.world().transformPoint(new Vec3(0.5f, 0.5f, 0.5f));
        assertTrue(corner.approxEquals(new Vec3(2, 4, 6), EPS), corner.toString());
    }

    @Test
    void boundsAreWorldSpace() throws Exception {
        ArchetypeInstance box = instanceOf(BuiltinArchetypes.COLLIDER_BOX);
        box.set("size", new Vec3(1, 2, 3));
        SceneNode node = nodeAt(new Vec3(1, 2, 3), 2f);

        Aabb bounds = behavior.bounds(node, box).unwrap();

        assertEquals(0f, bounds.getMinX(), EPS);
        assertEquals(2f, bounds.getMaxX(), EPS);
        assertEquals(0f, bounds.getMinY(), EPS);
        assertEquals(4f, bounds.getMaxY(), EPS);
        assertEquals(0f, bounds.getMinZ(), EPS);
        assertEquals(6f, bounds.getMaxZ(), EPS);
    }

    @Test
    void selectionShapeIsLocalBox() throws Exception {
        ArchetypeInstance box = instanceOf(BuiltinArchetypes.COLLIDER_BOX);
        box.set("size", new Vec3(1, 2, 3));

        GizmoShape shape = behavior.gizmoSelectionShape(nodeAt(Vec3.zero(), 5f), box).unwrap();

        assertEquals(new GizmoShape.GizmoBox(new Vec3(1, 2, 3)), shape);
    }

    @Test
    void defaultZeroSizeIsReportedAndNotDrawn() {
        ArchetypeInstance box = instanceOf(BuiltinArchetypes.COLLIDER_BOX);
        SceneNode node = nodeAt(Vec3.zero(), 1f);

        assertEquals(1, behavior.validate(node, box).size());
        assertTrue(behavior.drawableObjects(node, box, null).isEmpty());
        assertTrue(behavior.bounds(node, box).isNone());
        assertTrue(behavior.gizmoSelectionShape(node, box).isNone());
    }

    @Test
    void anyNonPositiveAxisIsInvalid() throws Exception {
        ArchetypeInstance box = instanceOf(BuiltinArchetypes.COLLIDER_BOX);
        SceneNode node = nodeAt(Vec3.zero(), 1f);

        box.set("size", new Vec3(1, -1, 1));
        assertEquals(1, behavior.validate(node, box).size());

        box.set("size", new Vec3(1, Float.NaN, 1));
        assertEquals(1, behavior.validate(node, box).size());
    }

    @Test
    void positiveSizeIsValid() throws Exception {
        ArchetypeInstance box = instanceOf(BuiltinArchetypes.COLLIDER_BOX);
        box.set("size", new Vec3(1, 1, 1));

        assertTrue(behavior.validate(nodeAt(Vec3.zero(), 1f), box).isEmpty());
    }
}
