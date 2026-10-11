package org.halalfoursome.kebabeditor.scene.resolve;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;

import com.fasterxml.jackson.databind.JsonNode;

// Reads typed arrays out of a glTF's buffers, 
// following accessor -> bufferView -> buffer
public final class GltfAccessors {

    private static final int BYTE = 5120;
    private static final int UNSIGNED_BYTE = 5121;
    private static final int SHORT = 5122;
    private static final int UNSIGNED_SHORT = 5123;
    private static final int UNSIGNED_INT = 5125;
    private static final int FLOAT = 5126;
    private static final String BASE64_MARKER = ";base64";

    private final GltfDocument document;
    private final Map<Integer, byte[]> buffers = new HashMap<>();

    public GltfAccessors(GltfDocument document) {
        this.document = document;
    }

    public float[] floats(int accessorIndex, int components) throws ModelLoadException {
        Accessor accessor = accessor(accessorIndex);

        if (accessor.components != components) {
            throw new ModelLoadException(
                "Accessor " + accessorIndex + " has " + accessor.components
                    + " components, expected " + components
            );
        }

        float[] values = new float[accessor.count * components];

        for (int element = 0; element < accessor.count; element++) {
            for (int component = 0; component < components; component++) {
                values[element * components + component] = accessor.floatAt(element, component);
            }
        }

        return values;
    }

    public int[] indices(int accessorIndex) throws ModelLoadException {
        Accessor accessor = accessor(accessorIndex);

        if (accessor.components != 1) {
            throw new ModelLoadException("Index accessor " + accessorIndex + " must be SCALAR");
        }

        int[] values = new int[accessor.count];

        for (int element = 0; element < accessor.count; element++) {
            values[element] = accessor.indexAt(element);
        }

        return values;
    }

    private Accessor accessor(int index) throws ModelLoadException {
        JsonNode json = document.json().path("accessors").get(index);

        if (json == null) {
            throw new ModelLoadException("Accessor " + index + " does not exist");
        }
        if (json.has("sparse")) {
            throw new ModelLoadException("Sparse accessors are not supported (accessor " + index + ")");
        }

        int componentType = json.path("componentType").asInt(-1);
        int componentSize = componentSize(componentType);
        int components = components(json.path("type").asText(""));
        int count = json.path("count").asInt(-1);

        if (count < 0) {
            throw new ModelLoadException("Accessor " + index + " has no valid count");
        }

        int elementSize = componentSize * components;

        if (!json.has("bufferView")) {
            // Per spec an accessor without a bufferView is all zeros
            ByteBuffer zeros = ByteBuffer.allocate(count * elementSize).order(ByteOrder.LITTLE_ENDIAN);

            return new Accessor(zeros, 0, elementSize, componentType, false, count, components);
        }

        JsonNode view = document.json().path("bufferViews").get(json.path("bufferView").asInt(-1));

        if (view == null) {
            throw new ModelLoadException("Accessor " + index + " points at a missing bufferView");
        }

        byte[] bytes = buffer(view.path("buffer").asInt(-1));
        int stride = view.path("byteStride").asInt(0);
        int start = view.path("byteOffset").asInt(0) + json.path("byteOffset").asInt(0);

        if (stride == 0) {
            stride = elementSize;
        }
        if (stride < elementSize || start < 0) {
            throw new ModelLoadException("Accessor " + index + " has an invalid stride or offset");
        }
        if (count > 0 && (long) start + (long) (count - 1) * stride + elementSize > bytes.length) {
            throw new ModelLoadException("Accessor " + index + " reads past the end of its buffer");
        }

        return new Accessor(
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN),
            start,
            stride,
            componentType,
            json.path("normalized").asBoolean(false),
            count,
            components
        );
    }

    private byte[] buffer(int index) throws ModelLoadException {
        byte[] cached = buffers.get(index);

        if (cached != null) {
            return cached;
        }

        JsonNode json = document.json().path("buffers").get(index);

        if (json == null) {
            throw new ModelLoadException("Buffer " + index + " does not exist");
        }

        byte[] loaded = load(index, json.path("uri").asText(null));
        buffers.put(index, loaded);

        return loaded;
    }

    private byte[] load(int index, String uri) throws ModelLoadException {
        if (uri == null) {
            if (index == 0 && document.binaryChunk().isSome()) {
                return document.binaryChunk().unwrap();
            }

            throw new ModelLoadException("Buffer " + index + " has no uri and there is no GLB chunk");
        }

        if (uri.startsWith("data:")) {
            return decodeDataUri(uri);
        }

        Path file = document.resolveInside(uri).unwrapOrNull();

        if (file == null) {
            throw new ModelLoadException("Buffer uri '" + uri + "' points outside the model directory");
        }

        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ModelLoadException("Cannot read buffer " + file, e);
        }
    }

    private static byte[] decodeDataUri(String uri) throws ModelLoadException {
        int comma = uri.indexOf(',');

        if (comma < 0 || !uri.substring(0, comma).endsWith(BASE64_MARKER)) {
            throw new ModelLoadException("Only base64 data uris are supported");
        }

        try {
            return Base64.getDecoder().decode(uri.substring(comma + 1));
        } catch (IllegalArgumentException e) {
            throw new ModelLoadException("Invalid base64 buffer", e);
        }
    }

    private static int componentSize(int componentType) throws ModelLoadException {
        return switch (componentType) {
            case BYTE, UNSIGNED_BYTE -> 1;
            case SHORT, UNSIGNED_SHORT -> 2;
            case UNSIGNED_INT, FLOAT -> 4;
            default -> throw new ModelLoadException("Unsupported component type " + componentType);
        };
    }

    private static int components(String type) throws ModelLoadException {
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            default -> throw new ModelLoadException("Unsupported accessor type '" + type + "'");
        };
    }

    private record Accessor(
        ByteBuffer data,
        int start,
        int stride,
        int componentType,
        boolean normalized,
        int count,
        int components
    ) {

        float floatAt(int element, int component) {
            int position = start + element * stride + component * componentSize();

            return switch (componentType) {
                case FLOAT -> data.getFloat(position);
                case BYTE -> scale(data.get(position), 127f);
                case UNSIGNED_BYTE -> scale(data.get(position) & 0xFF, 255f);
                case SHORT -> scale(data.getShort(position), 32767f);
                case UNSIGNED_SHORT -> scale(data.getShort(position) & 0xFFFF, 65535f);
                default -> scale(data.getInt(position) & 0xFFFFFFFFL, 4294967295f);
            };
        }

        int indexAt(int element) throws ModelLoadException {
            int position = start + element * stride;

            return switch (componentType) {
                case UNSIGNED_BYTE -> data.get(position) & 0xFF;
                case UNSIGNED_SHORT -> data.getShort(position) & 0xFFFF;
                case UNSIGNED_INT -> toInt(data.getInt(position) & 0xFFFFFFFFL);
                default -> throw new ModelLoadException("Indices must be unsigned byte, short or int");
            };
        }

        private int componentSize() {
            return switch (componentType) {
                case BYTE, UNSIGNED_BYTE -> 1;
                case SHORT, UNSIGNED_SHORT -> 2;
                default -> 4;
            };
        }

        // Signed normalized values clamp at -1, 
        // matching the glTF conversion rule
        private float scale(double value, float max) {
            return normalized ? (float) Math.max(value / max, -1.0) : (float) value;
        }

        private static int toInt(long value) throws ModelLoadException {
            if (value > Integer.MAX_VALUE) {
                throw new ModelLoadException("Index " + value + " is too large");
            }

            return (int) value;
        }
    }
}
