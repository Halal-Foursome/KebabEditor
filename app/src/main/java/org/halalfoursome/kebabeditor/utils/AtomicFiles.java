package org.halalfoursome.kebabeditor.utils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class AtomicFiles {

    private AtomicFiles() {}

    public static void write(Path target, byte[] data) throws IOException {
        Path temp = target.toAbsolutePath().resolveSibling(
            target.getFileName() + "." + UUID.randomUUID() + ".tmp"
        );
        Files.createFile(temp);

        try {
            copyPermissions(target, temp);
            Files.write(temp, data);
            move(temp, target);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    private static void copyPermissions(Path from, Path to) throws IOException {
        if (!Files.exists(from)) {
            return;
        }

        try {
            Files.setPosixFilePermissions(to, Files.getPosixFilePermissions(from));
        } catch (UnsupportedOperationException e) {
            // not a POSIX file system, so there is nothing to copy
        }
    }

    private static void move(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
