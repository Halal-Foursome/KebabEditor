package org.halalfoursome.kebabeditor.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class FileProjectIoTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProjectWriter writer = new FileProjectWriter(new GltfSceneWriter(mapper), mapper);
    private final ProjectLoader loader = new FileProjectLoader(new GltfSceneReader(mapper), mapper);

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
    void createProjectRefusesToOverwriteAnExistingScene() throws Exception {
        writer.createProject(directory, "level");

        org.junit.jupiter.api.Assertions.assertThrows(
            java.nio.file.FileAlreadyExistsException.class,
            () -> writer.createProject(directory, "level")
        );
    }
}
