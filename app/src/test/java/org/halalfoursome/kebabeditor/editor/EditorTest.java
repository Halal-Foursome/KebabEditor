package org.halalfoursome.kebabeditor.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.project.FileProjectLoader;
import org.halalfoursome.kebabeditor.project.FileProjectWriter;
import org.halalfoursome.kebabeditor.project.ModelImporter;
import org.halalfoursome.kebabeditor.project.ProjectManager;
import org.halalfoursome.kebabeditor.project.RecentProjects;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.CompositeModelExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.GltfMeshExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.ResolvedModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class EditorTest {

    @TempDir
    Path projectDir;

    @TempDir
    Path elsewhere;

    private Editor editor;

    // Keeps the user's real recent-projects file out of tests.
    private static final class InMemoryRecentProjects implements RecentProjects {
        private final List<Path> paths = new ArrayList<>();

        public List<Path> paths() { return List.copyOf(paths); }
        public void add(Path file) { paths.add(file); }
        public void remove(Path file) { paths.remove(file); }
        public void clear() { paths.clear(); }
    }

    @BeforeEach
    void openNewProject() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProjectManager projects = new ProjectManager(
            new InMemoryRecentProjects(),
            new FileProjectWriter(new GltfSceneWriter(mapper), mapper),
            new FileProjectLoader(new GltfSceneReader(mapper), mapper,
                new CompositeModelExtractor(List.of(new GltfMeshExtractor())))
        );
        editor = new Editor(projects, new ModelImporter(mapper));
        editor.createProject(projectDir, "level");
    }

    @Test
    void snapDistanceDefaultsWithoutASnapDistanceObject() {
        assertEquals(BuiltinArchetypes.DEFAULT_SNAP_DISTANCE, editor.snapDistance());
    }

    @Test
    void settingTheSnapDistanceAddsASnapDistanceObjectThatIsSaved() throws Exception {
        editor.setSnapDistance(0.25f);
        editor.setSnapDistance(2f); // updates the same object rather than adding another

        long markers = editor.scene().unwrap().traverse()
            .filter(n -> n.getArchetypes().stream().anyMatch(a -> a.archetypeId().equals(BuiltinArchetypes.SNAP_DISTANCE)))
            .count();
        assertEquals(1, markers);
        assertEquals(2f, editor.snapDistance());

        editor.saveProject();
        editor.openProject(editor.currentProjectFile().unwrap());
        assertEquals(2f, editor.snapDistance());
    }

    @Test
    void rejectsNonPositiveSnapDistances() {
        assertThrows(IllegalArgumentException.class, () -> editor.setSnapDistance(0f));
        assertThrows(IllegalArgumentException.class, () -> editor.setSnapDistance(-1f));
        assertThrows(IllegalArgumentException.class, () -> editor.setSnapDistance(Float.NaN));
    }

    @Test
    void playtestingFlagIsOffUntilSet() {
        assertFalse(editor.isPlaytesting());

        editor.setPlaytesting(true);
        assertTrue(editor.isPlaytesting());

        editor.setPlaytesting(false);
        assertFalse(editor.isPlaytesting());
    }

    @Test
    void snapStepFollowsTheSnappingSwitch() {
        assertEquals(0.5f, editor.snapStep().unwrap());

        editor.setSnapping(false);
        assertTrue(editor.snapStep().isNone());
    }

    @Test
    void importedModelIsCopiedInAndShownThroughModelRef() throws Exception {
        Path model = writeTriangleGltf(elsewhere.resolve("tri.gltf"));

        SceneNode node = editor.importModel(model, new Vec3(1, 0, 2));

        assertTrue(Files.isRegularFile(projectDir.resolve("models/tri/tri.gltf")));
        assertTrue(Files.isRegularFile(projectDir.resolve("models/tri/tri.bin")));

        ArchetypeInstance modelRef = assertInstanceOf(ArchetypeInstance.class, node.getArchetypes().getFirst());
        assertEquals(BuiltinArchetypes.MODEL_REF, modelRef.archetypeId());
        assertEquals("models/tri/tri.gltf", modelRef.get("path"));

        ResolvedModel resolved = assertInstanceOf(ResolvedModel.class,
            editor.modelResolver().unwrap().resolve(editor.currentProjectFile().unwrap(), "models/tri/tri.gltf"));
        assertEquals(1, resolved.parts().size());
        assertSame(node, editor.selection().primary().unwrap());
    }

    @Test
    void undoRemovesASpawnedObjectAndRedoBringsItBack() {
        SceneNode box = editor.spawnGreybox(GreyboxShape.BOX, Vec3.zero());

        editor.undo();
        assertTrue(editor.scene().unwrap().find(box.getId()).isNone());
        assertTrue(editor.selection().nodes().isEmpty());
        assertTrue(editor.history().canRedo());

        editor.redo();
        assertSame(box, editor.scene().unwrap().find(box.getId()).unwrap());
        assertSame(box, editor.selection().primary().unwrap());
    }

    @Test
    void undoingADeleteRestoresTheSameNodesWithTheirChildren() throws Exception {
        SceneNode parent = editor.spawnEmpty(Vec3.zero());
        SceneNode child = new SceneNode("child");
        editor.scene().unwrap().add(child, parent);

        editor.delete(editor.planDeletion(List.of(parent)));
        assertTrue(editor.scene().unwrap().find(child.getId()).isNone());

        editor.undo();
        assertSame(parent, editor.scene().unwrap().find(parent.getId()).unwrap());
        assertSame(child, editor.scene().unwrap().find(child.getId()).unwrap());
        assertSame(parent, child.getParent().unwrap());
        assertEquals(List.of(child), parent.getChildren());
    }

    @Test
    void aDragIsOneStepAndAnUnchangedOneIsNotRecorded() {
        SceneNode node = editor.spawnEmpty(Vec3.zero());

        editor.history().begin();
        assertFalse(editor.history().canUndo());
        node.getTransform().setTranslation(new Vec3(1, 0, 0));
        node.getTransform().setTranslation(new Vec3(2, 0, 0));
        editor.history().commit("move");

        editor.history().begin();
        editor.history().commit("move");

        editor.undo();
        assertEquals(Vec3.zero(), node.getTransform().getTranslation());

        editor.undo();
        assertTrue(editor.scene().unwrap().find(node.getId()).isNone());
        assertFalse(editor.history().canUndo());
    }

    @Test
    void undoRestoresParamValuesAndANewEditClearsRedo() throws Exception {
        editor.setSnapDistance(2f);
        ArchetypeInstance snap = editor.sceneNodes().stream()
            .flatMap(n -> n.getArchetypes().stream())
            .map(ArchetypeInstance.class::cast)
            .findFirst().orElseThrow();
        editor.setParams(snap, Map.of("distance", 4f));

        editor.undo();
        assertEquals(2f, snap.get("distance"));

        editor.renameNode(editor.sceneNodes().getFirst(), "Grid");
        assertFalse(editor.history().canRedo());
    }

    @Test
    void reopeningTheProjectClearsTheHistory() throws Exception {
        editor.spawnEmpty(Vec3.zero());
        editor.openProject(editor.currentProjectFile().unwrap());

        assertFalse(editor.history().canUndo());
    }

    // One triangle with positions in an external .bin next to the .gltf.
    static Path writeTriangleGltf(Path gltf) throws Exception {
        ByteBuffer bin = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : new float[] { 0, 0, 0, 1, 0, 0, 0, 0, 1 }) {
            bin.putFloat(f);
        }
        Files.write(gltf.resolveSibling("tri.bin"), bin.array());
        Files.writeString(gltf, """
            {
              "asset": { "version": "2.0" },
              "scenes": [ { "nodes": [0] } ],
              "nodes": [ { "mesh": 0 } ],
              "meshes": [ { "primitives": [ { "attributes": { "POSITION": 0 } } ] } ],
              "buffers": [ { "uri": "tri.bin", "byteLength": 36 } ],
              "bufferViews": [ { "buffer": 0, "byteLength": 36 } ],
              "accessors": [ { "bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3",
                               "min": [0, 0, 0], "max": [1, 0, 1] } ]
            }
            """);
        return gltf;
    }
}
