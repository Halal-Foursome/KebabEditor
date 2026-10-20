package org.halalfoursome.kebabeditor.project;

import java.nio.file.*;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EditorSessionsTest {
    @TempDir Path dir;
    @Test void separateSessionsHaveIndependentSceneSelectionResolversAndSaveTargets() throws Exception {
        Editor one = EditorSessions.create(), two = EditorSessions.create();
        one.createProject(dir.resolve("one"), "one");
        two.createProject(dir.resolve("two"), "two");
        one.spawnEmpty(Vec3.zero());
        one.saveScene();
        assertTrue(two.sceneNodes().isEmpty());
        assertTrue(two.selection().nodes().isEmpty());
        assertNotSame(one.modelResolver().unwrap(), two.modelResolver().unwrap());
        assertNotSame(one.prefabResolver().unwrap(), two.prefabResolver().unwrap());
        byte[] first = Files.readAllBytes(dir.resolve("one/one.gltf"));
        byte[] registry = Files.readAllBytes(dir.resolve("two/archetypes.json"));
        two.spawnEmpty(Vec3.zero());
        two.saveScene();
        assertArrayEquals(first, Files.readAllBytes(dir.resolve("one/one.gltf")));
        assertArrayEquals(registry, Files.readAllBytes(dir.resolve("two/archetypes.json")));
    }
    @Test void twoWindowsOnSameFileCannotSilentlyOverwriteEachOther() throws Exception {
        Editor one = EditorSessions.create(), two = EditorSessions.create();
        one.createProject(dir, "scene");
        two.openProject(dir.resolve("scene.gltf"));
        one.spawnEmpty(Vec3.zero());
        one.saveScene();
        assertThrows(java.io.IOException.class, two::saveScene);
        assertTrue(two.sceneNodes().isEmpty());
    }
}
