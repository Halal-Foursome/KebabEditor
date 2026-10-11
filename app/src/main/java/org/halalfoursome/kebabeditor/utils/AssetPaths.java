package org.halalfoursome.kebabeditor.utils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class AssetPaths {

    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

    private AssetPaths() {}

    public static Maybe<Path> resolveInside(Path directory, Path assetRoot, String reference) {
        if (SCHEME.matcher(reference).find() || reference.startsWith("/") || reference.startsWith("\\")) {
            return Maybe.none();
        }

        try {
            Path resolved = directory.resolve(decode(reference)).normalize();

            return isInsideRoot(resolved, assetRoot) ? Maybe.some(resolved) : Maybe.none();
        } catch (InvalidPathException e) {
            return Maybe.none();
        }
    }

    private static String decode(String reference) {
        try {
            // A '+' is a literal plus in a path, unlike in form encoding
            return URLDecoder.decode(reference.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Not valid percent-encoding (a raw "100%.png", say), so use it as written
            return reference;
        }
    }

    private static boolean isInsideRoot(Path resolved, Path assetRoot) {
        if (!resolved.startsWith(assetRoot)) {
            return false;
        }

        if (!Files.exists(resolved)) {
            return true;
        }

        // A symlink inside the project must not lead outside it
        try {
            return resolved.toRealPath().startsWith(assetRoot.toRealPath());
        } catch (IOException e) {
            return false;
        }
    }
}
