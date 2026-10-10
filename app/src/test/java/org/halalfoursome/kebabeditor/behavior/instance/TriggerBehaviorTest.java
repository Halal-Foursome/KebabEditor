package org.halalfoursome.kebabeditor.behavior.instance;

import static org.halalfoursome.kebabeditor.behavior.instance.BehaviorTestSupport.instanceOf;
import static org.halalfoursome.kebabeditor.behavior.instance.BehaviorTestSupport.nodeAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.junit.jupiter.api.Test;

class TriggerBehaviorTest {

    private final TriggerBehavior behavior = new TriggerBehavior();

    @Test
    void drawsAnOverlayBoxInADifferentColourFromColliders() throws Exception {
        ArchetypeInstance trigger = instanceOf(BuiltinArchetypes.TRIGGER);
        trigger.set("size", new Vec3(2, 2, 2));
        SceneNode node = nodeAt(Vec3.zero(), 1f);

        Drawable drawable = behavior.drawableObjects(node, trigger, null).get(0);

        assertEquals(Meshes.CUBE_LINES, drawable.mesh());
        assertEquals(DrawMode.OVERLAY, drawable.mode());

        ArchetypeInstance box = instanceOf(BuiltinArchetypes.COLLIDER_BOX);
        box.set("size", new Vec3(2, 2, 2));
        Drawable boxDrawable = new ColliderBoxBehavior().drawableObjects(node, box, null).get(0);
        assertNotEquals(boxDrawable.material().baseColor(), drawable.material().baseColor());
    }

    @Test
    void validationMessageNamesTheTrigger() {
        ArchetypeInstance trigger = instanceOf(BuiltinArchetypes.TRIGGER);

        var problems = behavior.validate(nodeAt(Vec3.zero(), 1f), trigger);

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().startsWith("trigger:"));
    }
}
