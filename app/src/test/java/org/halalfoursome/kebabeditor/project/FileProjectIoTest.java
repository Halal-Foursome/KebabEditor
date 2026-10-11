package org.halalfoursome.kebabeditor.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.CompositeModelExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.GltfMeshExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class FileProjectIoTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectWriter writer = new FileProjectWriter(new GltfSceneWriter(mapper), mapper);
    private final ProjectLoader loader = new FileProjectLoader(
        new GltfSceneReader(mapper),
        mapper,
        new CompositeModelExtractor(List.of(new GltfMeshExtractor()))
    );

    @TempDir
    Path directory;

    @Test
    void newProjectLoadsAsAnEmptySceneWithBuiltinsAvailable() throws Exception {
        Path file = writer.createProject(directory, "level");

        ProjectRepository project = loader.load(file);

        assertEquals(file, project.filePath());
        assertTrue(project.sceneData().getRootNodes().isEmpty());
        assertTrue(project.archetypeCatalog().find("collider_box").isSome());
        assertTrue(project.archetypeCatalog().getRegistry().getArchetypes().isEmpty());
    }

    @Test
    void savedArchetypesComeBackButBuiltinsAreNotWrittenToTheFile() throws Exception {
        Path file = writer.createProject(directory, "level");
        ProjectRepository project = loader.load(file);
        project.archetypeCatalog().getRegistry().add(new ArchetypeDefinition("mine"));

        writer.saveArchetypes(file, project.archetypeCatalog().getRegistry());

        assertTrue(loader.load(file).archetypeCatalog().find("mine").isSome());
        assertTrue(!Files.readString(ProjectTemplates.archetypesFileFor(file)).contains("collider_box"));
    }

    @Test
    void savedSceneComesBack() throws Exception {
        Path file = writer.createProject(directory, "level");
        ProjectRepository project = loader.load(file);
        project.sceneData().add(new SceneNode("crate"));

        writer.saveScene(file, project.sceneData());

        ProjectRepository reloaded = loader.load(file);
        assertEquals(1, reloaded.sceneData().getRootNodes().size());
        assertEquals("crate", reloaded.sceneData().getRootNodes().get(0).getName());
    }

    @Test
    void eachLoadedProjectGetsItsOwnResolverRootedAtItsFolder() throws Exception {
        Path first = writer.createProject(directory.resolve("one"), "level");
        Path second = writer.createProject(directory.resolve("two"), "level");
        Files.writeString(directory.resolve("one/tri.gltf"), TRIANGLE_GLTF);

        ProjectRepository one = loader.load(first);
        ProjectRepository two = loader.load(second);

        assertNotSame(one.modelResolver(), two.modelResolver());
        assertInstanceOf(ModelResult.ResolvedModel.class, one.modelResolver().resolve(first, "tri.gltf"));
        // The other project cannot see the first project's files
        assertInstanceOf(ModelResult.BrokenReference.class, two.modelResolver().resolve(second, "../one/tri.gltf"));
    }

    // Three float positions in a base64 buffer: the smallest drawable glTF
    private static final String TRIANGLE_GLTF = "{\"asset\":{\"version\":\"2.0\"},"
        + "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0}}]}],"
        + "\"buffers\":[{\"uri\":\"data:application/octet-stream;base64,"
        + java.util.Base64.getEncoder().encodeToString(triangleBytes()) + "\",\"byteLength\":36}],"
        + "\"bufferViews\":[{\"buffer\":0,\"byteLength\":36}],"
        + "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"}]}";

    private static byte[] triangleBytes() {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(36).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (float value : new float[] { 0, 0, 0, 1, 0, 0, 0, 1, 0 }) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }
}
