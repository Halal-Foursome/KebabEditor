package org.halalfoursome.kebabeditor.scene.resolve;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;
import org.halalfoursome.kebabeditor.utils.AssetPaths;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record GltfDocument(
    JsonNode json, 
    Path directory, 
    Path assetRoot,
    Maybe<byte[]> binaryChunk
) {

    private static final int GLB_MAGIC = 0x46546C67;
    private static final int GLB_VERSION = 2;
    private static final int CHUNK_JSON = 0x4E4F534A;
    private static final int CHUNK_BIN = 0x004E4942;
    private static final int GLB_HEADER_SIZE = 12;
    private static final int CHUNK_HEADER_SIZE = 8;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static GltfDocument read(Path file, Path assetRoot) throws ModelLoadException {
        byte[] bytes;

        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ModelLoadException("Cannot read model " + file, e);
        }

        Path directory = file.toAbsolutePath().normalize().getParent();
        Path root = assetRoot.toAbsolutePath().normalize();

        if (!directory.startsWith(root)) {
            throw new ModelLoadException("Model " + file + " is outside the asset root " + root);
        }

        try {
            return isGlb(bytes)
                ? readGlb(bytes, directory, root)
                : new GltfDocument(MAPPER.readTree(bytes), directory, root, Maybe.none());
        } catch (IOException e) {
            throw new ModelLoadException("Invalid glTF in " + file, e);
        }
    }

    public Maybe<Path> resolveInside(String uri) {
        return AssetPaths.resolveInside(directory, assetRoot, uri);
    }

    private static boolean isGlb(byte[] bytes) {
        return bytes.length >= GLB_HEADER_SIZE
            && ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(0) == GLB_MAGIC;
    }

    private static GltfDocument readGlb(byte[] bytes, Path directory, Path assetRoot) throws IOException, ModelLoadException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        if (buffer.getInt(4) != GLB_VERSION) {
            throw new ModelLoadException("Unsupported GLB version " + buffer.getInt(4));
        }

        int total = buffer.getInt(8);

        if (total < GLB_HEADER_SIZE || total > bytes.length) {
            throw new ModelLoadException("GLB length does not match the file");
        }

        JsonNode json = null;
        byte[] binary = null;
        int position = GLB_HEADER_SIZE;

        while (position + CHUNK_HEADER_SIZE <= total) {
            int length = buffer.getInt(position);
            int type = buffer.getInt(position + 4);
            int start = position + CHUNK_HEADER_SIZE;

            if (length < 0 || (long) start + length > total) {
                throw new ModelLoadException("GLB chunk exceeds the file");
            }

            if (type == CHUNK_JSON && json == null) {
                json = MAPPER.readTree(bytes, start, length);
            } else if (type == CHUNK_BIN && binary == null) {
                binary = Arrays.copyOfRange(bytes, start, start + length);
            }

            position = start + length;
        }

        if (json == null) {
            throw new ModelLoadException("GLB has no JSON chunk");
        }

        return new GltfDocument(json, directory, assetRoot, Maybe.ofNullable(binary));
    }
}
