package org.halalfoursome.kebabeditor.assets;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.halalfoursome.kebabeditor.project.ProjectAssets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AssetServiceTest {
    @TempDir Path dir;
    private AssetService service() {
        return new AssetService(new ProjectAssets(dir, List.of(dir.resolve("assets")), List.of(dir.resolve("level.gltf"))));
    }
    @Test void listsDirectChildrenSortedWithFoldersFirstAndOnlyConfiguredDirectories() throws Exception {
        Files.createDirectories(dir.resolve("assets/Zebra"));
        Files.createDirectory(dir.resolve("assets/apple"));
        Files.createDirectory(dir.resolve("unrelated"));
        Files.writeString(dir.resolve("assets/z.glb"), "");
        Files.writeString(dir.resolve("assets/B.gltf"), "");
        Files.writeString(dir.resolve("assets/a.txt"), "");
        assertEquals(List.of("apple", "Zebra", "a.txt", "B.gltf", "z.glb"),
            service().list(dir.resolve("assets")).stream().map(Object::toString).toList());
        assertEquals(List.of("assets"), service().list(dir).stream().map(Object::toString).toList());
        assertThrows(IOException.class, () -> service().list(dir.resolve("unrelated")));
    }
    @Test void missingAndEmptyDirectoriesAndSymlinkCycleAreSafe() throws Exception {
        assertThrows(IOException.class, () -> service().list(dir.resolve("assets")));
        Files.createDirectory(dir.resolve("assets"));
        assertTrue(service().list(dir.resolve("assets")).isEmpty());
        Files.createSymbolicLink(dir.resolve("assets/cycle"), dir.resolve("assets"));
        assertTrue(service().list(dir.resolve("assets")).isEmpty());
        assertThrows(IOException.class, () -> service().list(dir.resolve("../")));
    }
    @Test void registeredScenesAreAuthoritativeAndOpenActionsAreDispatchedCorrectly() throws Exception {
        Files.createDirectory(dir.resolve("assets"));
        Path scene = Files.writeString(dir.resolve("level.gltf"), "");
        Path model = Files.writeString(dir.resolve("assets/mesh.gltf"), "");
        Path binary = Files.writeString(dir.resolve("assets/model.GLB"), "");
        Path image = Files.writeString(dir.resolve("assets/photo.PNG"), "");
        List<Path> models = new ArrayList<>(), scenes = new ArrayList<>();
        AssetOpenHandler opener = new AssetOpenHandler(models::add, scenes::add);
        for (Path file : List.of(scene, model, binary, image)) opener.open(service(), file);
        assertEquals(List.of(scene), scenes);
        assertEquals(List.of(model, binary), models);
        assertEquals(AssetType.IMAGE, service().type(image));
        assertEquals(AssetType.SCENE, service().type(scene));
        assertEquals(AssetType.MODEL, service().type(model));
        assertThrows(IOException.class, () -> opener.open(service(), dir.resolve("missing.gltf")));
    }
}
