package org.halalfoursome.kebabeditor.behavior.instance;

import static org.halalfoursome.kebabeditor.behavior.instance.BehaviorTestSupport.EPS;
import static org.halalfoursome.kebabeditor.behavior.instance.BehaviorTestSupport.instanceOf;
import static org.halalfoursome.kebabeditor.behavior.instance.BehaviorTestSupport.nodeAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.model.TextureSlot;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.CachingModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.ModelExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelRefBehaviorTest {

    private static final Material CRATE_MATERIAL = Material.solid(new Color(10, 20, 30));

    @TempDir
    Path project;

    private List<ModelPart> parts = List.of(
        new ModelPart(Meshes.CUBE_TRIANGLES, CRATE_MATERIAL, Mat4.translation(new Vec3(2, 0, 0)))
    );

    private final ModelRefBehavior behavior = new ModelRefBehavior();

    private BehaviorContext context() throws Exception {
        Path scene = project.resolve("level.gltf");
        Files.write(scene, new byte[] { 1 });
        Files.write(project.resolve("crate.model"), new byte[] { 1 });

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

        return new BehaviorContext(
            new CachingModelResolver(extractor, project),
            BehaviorTestSupport.NO_PREFABS,
            scene
        );
    }

    private static ArchetypeInstance modelRef(String path) throws Exception {
        ArchetypeInstance instance = instanceOf(BuiltinArchetypes.MODEL_REF);
        instance.set("path", path);

        return instance;
    }

    @Test
    void drawsEveryPartAtTheNodeComposedWithThePartTransform() throws Exception {
        SceneNode node = nodeAt(new Vec3(10, 0, 0), 1f);

        List<Drawable> drawableObjects = behavior.drawableObjects(node, modelRef("crate.model"), context());

        assertEquals(1, drawableObjects.size());
        Drawable drawable = drawableObjects.get(0);
        assertSame(Meshes.CUBE_TRIANGLES, drawable.mesh());
        assertSame(CRATE_MATERIAL, drawable.material());
        assertEquals(DrawMode.SOLID, drawable.mode());
        Vec3 origin = drawable.world().transformPoint(Vec3.zero());
        assertTrue(origin.approxEquals(new Vec3(12, 0, 0), EPS), origin.toString());
    }

    @Test
    void boundsAreTheModelBoundsInWorldSpace() throws Exception {
        Aabb bounds = behavior
            .bounds(nodeAt(new Vec3(10, 0, 0), 2f), modelRef("crate.model"), context())
            .unwrap();

        // The cube sits at x = 2 in model space; scaled by 2 and moved by 10 that is 12 to 16
        assertEquals(13f, bounds.getMinX(), EPS);
        assertEquals(15f, bounds.getMaxX(), EPS);
        assertEquals(-1f, bounds.getMinY(), EPS);
        assertEquals(1f, bounds.getMaxY(), EPS);
    }

    @Test
    void selectionShapeIsTheModelBoundsInLocalSpace() throws Exception {
        GizmoShape shape = behavior
            .gizmoSelectionShape(nodeAt(Vec3.zero(), 5f), modelRef("crate.model"), context())
            .unwrap();

        Aabb box = assertInstanceOf(GizmoShape.GizmoAabb.class, shape).box();
        assertEquals(1.5f, box.getMinX(), EPS);
        assertEquals(2.5f, box.getMaxX(), EPS);
    }

    @Test
    void missingModelIsDrawnAsAMissingTextureCubeWithTheReason() throws Exception {
        List<Drawable> drawableObjects = behavior.drawableObjects(nodeAt(Vec3.zero(), 1f), modelRef("gone.model"), context());

        assertEquals(1, drawableObjects.size());
        assertSame(Meshes.CUBE_TRIANGLES, drawableObjects.get(0).mesh());
        TextureSlot.Missing missing = assertInstanceOf(
            TextureSlot.Missing.class,
            drawableObjects.get(0).material().baseTexture()
        );
        assertTrue(missing.reason().contains("not found"), missing.reason());
    }

    @Test
    void missingModelStaysSelectableAndReportsTheProblem() throws Exception {
        SceneNode node = nodeAt(new Vec3(4, 0, 0), 1f);
        ArchetypeInstance ref = modelRef("gone.model");
        BehaviorContext context = context();

        Aabb bounds = behavior.bounds(node, ref, context).unwrap();
        GizmoShape shape = behavior.gizmoSelectionShape(node, ref, context).unwrap();
        var problems = behavior.validate(node, ref, context);

        assertEquals(3.5f, bounds.getMinX(), EPS);
        assertEquals(4.5f, bounds.getMaxX(), EPS);
        assertEquals(new GizmoShape.GizmoBox(Vec3.one()), shape);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().startsWith("model_ref:"), problems.get(0).getMessage());
        assertTrue(problems.get(0).getMessage().contains("not found"), problems.get(0).getMessage());
    }

    @Test
    void aNewModelRefWithNoPathIsReportedAsHavingNoPath() throws Exception {
        ArchetypeInstance fresh = instanceOf(BuiltinArchetypes.MODEL_REF);

        var problems = behavior.validate(nodeAt(Vec3.zero(), 1f), fresh, context());

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).getMessage().contains("No model path"), problems.get(0).getMessage());
    }

}
