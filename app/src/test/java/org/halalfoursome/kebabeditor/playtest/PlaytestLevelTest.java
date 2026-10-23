package org.halalfoursome.kebabeditor.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.OrientedBox;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;

class PlaytestLevelTest {

    static ArchetypeInstance instanceOf(String archetypeId) {
        return new ArchetypeInstance(BuiltinArchetypes.find(archetypeId).unwrap());
    }

    static SceneNode node(String name, Vec3 at) {
        SceneNode node = new SceneNode(name);
        node.setTransform(new Transform(at, Quat.identity(), 1f));
        return node;
    }

    static SceneNode greybox(String name, GreyboxShape shape, Vec3 at, Vec3 size) {
        SceneNode node = node(name, at);
        new Greybox(shape, size, Color.GRAY).writeTo(node);
        return node;
    }

    @Test
    void greyboxesAndCollidersAreSolidAndTriggersAreNot() throws Exception {
        Scene scene = new Scene();
        scene.add(greybox("floor", GreyboxShape.BOX, new Vec3(0, -0.5f, 0), new Vec3(10, 1, 10)));
        scene.add(greybox("ball", GreyboxShape.SPHERE, new Vec3(3, 1, 0), Vec3.one()));

        SceneNode crate = node("crate", new Vec3(-3, 1, 0));
        crate.addArchetype(instanceOf(BuiltinArchetypes.COLLIDER_BOX));
        scene.add(crate);

        SceneNode goal = node("goal", new Vec3(0, 1, 5));
        goal.addArchetype(instanceOf(BuiltinArchetypes.TRIGGER));
        scene.add(goal);

        SceneNode model = node("statue", Vec3.zero());
        model.addArchetype(instanceOf(BuiltinArchetypes.MODEL_REF));
        scene.add(model);

        PlaytestLevel level = PlaytestLevel.build(scene, PhysicsTestSupport.NO_ASSETS);

        assertEquals(List.of("floor", "ball", "crate"),
            level.world().solids().stream().map(c -> c.name()).toList());
        assertEquals(List.of("goal"), level.world().triggers().stream().map(c -> c.name()).toList());
        assertTrue(level.starts().isEmpty());
    }

    private static SceneNode playerStart(String name) {
        SceneNode node = node(name, Vec3.zero());
        node.addArchetype(instanceOf(BuiltinArchetypes.PLAYER_START));
        return node;
    }

    @Test
    void selectedPlayerStartIsUsed() throws Exception {
        Scene scene = new Scene();
        SceneNode first = playerStart("first");
        SceneNode second = playerStart("second");
        scene.add(first);
        scene.add(second);

        PlaytestLevel level = PlaytestLevel.build(scene, PhysicsTestSupport.NO_ASSETS);

        assertEquals("second", level.startFor(Maybe.some(second)).unwrap().name());
        assertEquals("first", level.startFor(Maybe.none()).unwrap().name());
        // Something else selected: the first start, as with nothing selected.
        assertEquals("first", level.startFor(Maybe.some(node("crate", Vec3.zero()))).unwrap().name());
    }

    @Test
    void levelWithoutPlayerStartHasNoStart() {
        PlaytestLevel level = PlaytestLevel.build(new Scene(), PhysicsTestSupport.NO_ASSETS);

        assertTrue(level.startFor(Maybe.none()).isNone());
    }

    @Test
    void levelWithOnlyTriggersHasNothingToStandOn() throws Exception {
        Scene scene = new Scene();
        SceneNode goal = node("goal", Vec3.zero());
        goal.addArchetype(instanceOf(BuiltinArchetypes.TRIGGER));
        scene.add(goal);

        assertTrue(PlaytestLevel.build(scene, PhysicsTestSupport.NO_ASSETS).hasNothingToStandOn());

        scene.add(greybox("floor", GreyboxShape.BOX, Vec3.zero(), Vec3.one()));
        assertFalse(PlaytestLevel.build(scene, PhysicsTestSupport.NO_ASSETS).hasNothingToStandOn());
    }

    @Test
    void childGreyboxFollowsItsRotatedParent() throws Exception {
        Scene scene = new Scene();
        SceneNode parent = node("pivot", new Vec3(5, 0, 0));
        parent.getTransform().setRotation(Quat.fromAxisAngle(Vec3.unitY(), (float) Math.toRadians(90)));
        scene.add(parent);
        // A 4 m bar along the parent's X, which the rotation turns onto world -Z.
        scene.add(greybox("bar", GreyboxShape.BOX, Vec3.zero(), new Vec3(4, 1, 1)), parent);

        PlaytestLevel level = PlaytestLevel.build(scene, PhysicsTestSupport.NO_ASSETS);
        Aabb bounds = level.world().solids().getFirst().bounds();

        assertTrue(level.world().solids().getFirst().shape() instanceof OrientedBox);
        assertEquals(1f, bounds.getMaxX() - bounds.getMinX(), 1e-3f, "bounds " + bounds);
        assertEquals(4f, bounds.getMaxZ() - bounds.getMinZ(), 1e-3f, "bounds " + bounds);
        assertEquals(5f, (bounds.getMaxX() + bounds.getMinX()) / 2, 1e-3f, "bounds " + bounds);
    }

    @Test
    void playerStartGivesPositionFacingAndSettings() throws Exception {
        Scene scene = new Scene();
        SceneNode start = node("spawn", new Vec3(1, 2, 3));
        start.getTransform().setRotation(Quat.fromAxisAngle(Vec3.unitY(), (float) Math.toRadians(90)));
        ArchetypeInstance archetype = instanceOf(BuiltinArchetypes.PLAYER_START);
        archetype.set(ActorSettings.JUMP_HEIGHT, 2.5f);
        start.addArchetype(archetype);
        scene.add(start);

        PlayerStart found = PlaytestLevel.build(scene, PhysicsTestSupport.NO_ASSETS).starts().getFirst();

        assertTrue(new Vec3(1, 2, 3).approxEquals(found.feet(), 1e-5f));
        assertEquals(Math.PI / 2, found.yaw(), 1e-4);
        assertEquals(2.5f, found.settings().unwrap().jumpHeight());
        assertEquals("spawn", found.name());
    }

    @Test
    void invalidPlayerStartReportsWhy() throws Exception {
        Scene scene = new Scene();
        SceneNode start = node("spawn", Vec3.zero());
        ArchetypeInstance archetype = instanceOf(BuiltinArchetypes.PLAYER_START);
        archetype.set(ActorSettings.HEIGHT, 0.5f);
        start.addArchetype(archetype);
        scene.add(start);

        PlayerStart found = PlaytestLevel.build(scene, PhysicsTestSupport.NO_ASSETS).starts().getFirst();

        assertTrue(found.settings().isNone());
        assertFalse(found.problems().isEmpty());
        assertTrue(found.problems().getFirst().contains("height"), found.problems().toString());
    }
}
