package org.halalfoursome.kebabeditor.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Ray;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.CachingModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.ModelExtractor;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PickerTest {

    private static final float EPS = 1e-4f;

    @TempDir
    Path dir;

    private final Picker picker = new Picker();

    private static SceneNode greybox(Scene scene, GreyboxShape shape, Vec3 position, Vec3 size) throws Exception {
        SceneNode node = new SceneNode(shape.displayName());
        node.getTransform().setTranslation(position);
        new Greybox(shape, size, Color.GRAY).writeTo(node);
        scene.add(node);
        return node;
    }

    private static Ray down(float x, float z) {
        return new Ray(new Vec3(x, 10, z), new Vec3(0, -1, 0));
    }

    @Test
    void hitsTopFaceOfNonUniformBox() throws Exception {
        Scene scene = new Scene();
        SceneNode platform = greybox(scene, GreyboxShape.BOX, new Vec3(0, 1, 0), new Vec3(4, 0.5f, 2));

        PickHit hit = picker.pick(scene, Maybe.none(), down(1.9f, 0.9f), false).unwrap();

        assertSame(platform, hit.node().unwrap());
        assertEquals(8.75f, hit.distance(), EPS); // top face at y = 1.25
        assertTrue(new Vec3(0, 1, 0).approxEquals(hit.normal(), EPS), "normal " + hit.normal());
        assertTrue(picker.pick(scene, Maybe.none(), down(2.1f, 0), false).isNone(), "just outside the 4-wide box");
    }

    @Test
    void followsRotation() throws Exception {
        Scene scene = new Scene();
        SceneNode node = greybox(scene, GreyboxShape.BOX, Vec3.zero(), new Vec3(4, 1, 0.5f));
        // Long side turned from X onto Z.
        node.setTransform(new Transform(Vec3.zero(), Quat.fromAxisAngle(Vec3.unitY(), (float) Math.PI / 2), 1f));

        assertTrue(picker.pick(scene, Maybe.none(), down(0, 1.9f), false).isSome());
        assertTrue(picker.pick(scene, Maybe.none(), down(1.9f, 0), false).isNone());
    }

    @Test
    void hitsSphereSurfaceWithRadialNormal() throws Exception {
        Scene scene = new Scene();
        greybox(scene, GreyboxShape.SPHERE, new Vec3(0, 0, 0), new Vec3(2, 2, 2));

        PickHit hit = picker.pick(scene, Maybe.none(), new Ray(new Vec3(-10, 0, 0), Vec3.unitX()), false).unwrap();

        assertEquals(9f, hit.distance(), EPS);
        assertTrue(new Vec3(-1, 0, 0).approxEquals(hit.normal(), EPS));
        // A ray that passes the sphere's corner misses, unlike its bounding box.
        assertTrue(picker.pick(scene, Maybe.none(), new Ray(new Vec3(-10, 0.95f, 0.95f), Vec3.unitX()), false).isNone());
    }

    @Test
    void returnsTheNearestOfOverlappingObjects() throws Exception {
        Scene scene = new Scene();
        greybox(scene, GreyboxShape.BOX, new Vec3(0, 0, 0), Vec3.one());
        SceneNode upper = greybox(scene, GreyboxShape.BOX, new Vec3(0, 3, 0), Vec3.one());

        assertSame(upper, picker.pick(scene, Maybe.none(), down(0, 0), false).unwrap().node().unwrap());
    }

    @Test
    void groundIsHitOnlyWhenAsked() throws Exception {
        Scene scene = new Scene();

        assertTrue(picker.pick(scene, Maybe.none(), down(3, 3), false).isNone());

        PickHit ground = picker.pick(scene, Maybe.none(), down(3, 3), true).unwrap();
        assertEquals(Maybe.none(), ground.node());
        assertTrue(new Vec3(3, 0, 3).approxEquals(ground.point(), EPS));
    }

    @Test
    void ignoresBoxesTheRayStartsInside() throws Exception {
        Scene scene = new Scene();
        greybox(scene, GreyboxShape.BOX, Vec3.zero(), new Vec3(10, 10, 10));

        assertTrue(picker.pick(scene, Maybe.none(), new Ray(Vec3.zero(), Vec3.unitX()), false).isNone());
    }

    @Test
    void picksModelRefTrianglesNotJustTheirBounds() throws Exception {
        // A model with one triangle, corners (0,0,0), (2,0,0), (0,0,2).
        MeshData triangle = MeshData.triangles(
            new float[] { 0, 0, 0, 2, 0, 0, 0, 0, 2 },
            new float[] { 0, 1, 0, 0, 1, 0, 0, 1, 0 },
            new float[0],
            new int[] { 0, 1, 2 }
        );
        BehaviorContext context = modelContext(List.of(new ModelPart(triangle, Material.DEFAULT, Mat4.identity())));

        Scene scene = new Scene();
        SceneNode node = new SceneNode("tri");
        ArchetypeInstance modelRef = new ArchetypeInstance(BuiltinArchetypes.find(BuiltinArchetypes.MODEL_REF).unwrap());
        modelRef.set("path", "tri.model");
        node.addArchetype(modelRef);
        scene.add(node);

        assertSame(node, picker.pick(scene, Maybe.some(context), down(0.5f, 0.5f), false).unwrap().node().unwrap());
        // Inside the bounding box but past the hypotenuse.
        assertTrue(picker.pick(scene, Maybe.some(context), down(1.5f, 1.5f), false).isNone());
    }

    @Test
    void picksLineOnlyArchetypesByTheirSelectionShape() throws Exception {
        Scene scene = new Scene();
        SceneNode node = new SceneNode("collider");
        node.getTransform().setTranslation(new Vec3(5, 0, 0));
        ArchetypeInstance collider = new ArchetypeInstance(BuiltinArchetypes.find(BuiltinArchetypes.COLLIDER_BOX).unwrap());
        collider.set("size", new Vec3(2, 2, 2));
        node.addArchetype(collider);
        scene.add(node);

        BehaviorContext context = modelContext(List.of());

        assertSame(node, picker.pick(scene, Maybe.some(context), down(5.9f, 0), false).unwrap().node().unwrap());
        assertTrue(picker.pick(scene, Maybe.some(context), down(6.1f, 0), false).isNone());
    }

    // A project whose every model file yields the given parts.
    private BehaviorContext modelContext(List<ModelPart> parts) throws Exception {
        Path scene = dir.resolve("level.gltf");
        Files.write(scene, new byte[] { 1 });
        Files.write(dir.resolve("tri.model"), new byte[] { 1 });

        ModelExtractor extractor = new ModelExtractor() {
            @Override
            public boolean supports(Path file) {
                return true;
            }

            @Override
            public List<ModelPart> extract(Path file, Path assetRoot) {
                return parts;
            }
        };
        return new BehaviorContext(new CachingModelResolver(extractor, dir), scene);
    }
}
