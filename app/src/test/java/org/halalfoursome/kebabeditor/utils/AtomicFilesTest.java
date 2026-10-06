package org.halalfoursome.kebabeditor.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFilesTest {

    @TempDir
    Path dir;

    @Test
    void createsAndReplacesFileWithoutLeavingTempFiles() throws IOException {
        Path file = dir.resolve("scene.gltf");

        AtomicFiles.write(file, "first".getBytes(StandardCharsets.UTF_8));
        AtomicFiles.write(file, "second".getBytes(StandardCharsets.UTF_8));

        assertArrayEquals("second".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void replacingKeepsExistingPermissions() throws IOException {
        Path file = dir.resolve("scene.gltf");
        Files.writeString(file, "old");
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-r-----");

        Assumptions.assumeTrue(file.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Files.setPosixFilePermissions(file, permissions);

        AtomicFiles.write(file, "new".getBytes(StandardCharsets.UTF_8));

        assertEquals(permissions, Files.getPosixFilePermissions(file));
    }

    @Test
    void failedWriteKeepsOldContentAndLeavesNoTempFile() throws IOException {
        Path file = dir.resolve("scene.gltf");
        AtomicFiles.write(file, "old".getBytes(StandardCharsets.UTF_8));

        Path blocked = dir.resolve("blocked");
        Files.createDirectory(blocked);
        Files.writeString(blocked.resolve("inner"), "x");

        assertThrows(
            IOException.class,
            () -> AtomicFiles.write(blocked, "new".getBytes(StandardCharsets.UTF_8))
        );

        assertEquals("old", Files.readString(file));
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(2, files.count());
        }
    }
}
