package org.halalfoursome.kebabeditor.scene.resolve;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.AlphaMode;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.model.TextureRef;
import org.halalfoursome.kebabeditor.model.TextureSlot;
import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;

public final class GltfMeshExtractor implements ModelExtractor {

    private static final int MODE_POINTS = 0;
    private static final int MODE_LINES = 1;
    private static final int MODE_LINE_LOOP = 2;
    private static final int MODE_LINE_STRIP = 3;
    private static final int MODE_TRIANGLES = 4;
    private static final int MODE_TRIANGLE_STRIP = 5;
    private static final int MODE_TRIANGLE_FAN = 6;

    @Override
    public boolean supports(Path file) {
        String name = file.getFileName().toString().toLowerCase();

        return name.endsWith(".gltf") || name.endsWith(".glb");
    }

    public List<ModelPart> extract(Path file) throws ModelLoadException {
        return extract(file, file.toAbsolutePath().getParent());
    }

    @Override
    public List<ModelPart> extract(Path file, Path assetRoot) throws ModelLoadException {
        return extract(GltfDocument.read(file, assetRoot));
    }

    public List<ModelPart> extract(
        JsonNode document, 
        Path directory
    ) throws ModelLoadException {
        return extract(document, directory, directory);
    }

    public List<ModelPart> extract(
        JsonNode document, 
        Path directory, 
        Path assetRoot
    ) throws ModelLoadException {
        return extract(new GltfDocument(
            document,
            directory.toAbsolutePath().normalize(),
            assetRoot.toAbsolutePath().normalize(),
            Maybe.none()
        ));
    }

    public List<ModelPart> extract(GltfDocument document) throws ModelLoadException {
        if (!document.json().isObject()) {
            throw new ModelLoadException("A glTF document must be a JSON object");
        }

        return new Extraction(document).run();
    }

    private static final class Extraction {

        private final GltfDocument document;
        private final JsonNode json;
        private final GltfAccessors accessors;
        private final List<ModelPart> parts = new ArrayList<>();
        private final Map<Integer, List<Primitive>> meshes = new HashMap<>();
        private final Map<Integer, Material> materials = new HashMap<>();
        private final Set<Integer> activeNodes = new HashSet<>();

        Extraction(GltfDocument document) {
            this.document = document;
            this.json = document.json();
            this.accessors = new GltfAccessors(document);
        }

        List<ModelPart> run() throws ModelLoadException {
            if (!json.path("nodes").isArray()) {
                addAllMeshesAtOrigin();

                return parts;
            }

            for (int root : rootNodes()) {
                visit(root, Mat4.identity());
            }

            return parts;
        }

        // A file without nodes still has drawable meshes, so show them untransformed
        private void addAllMeshesAtOrigin() throws ModelLoadException {
            for (int mesh = 0; mesh < json.path("meshes").size(); mesh++) {
                addMesh(mesh, Mat4.identity());
            }
        }

        private List<Integer> rootNodes() throws ModelLoadException {
            JsonNode scenes = json.path("scenes");

            if (scenes.isArray() && !scenes.isEmpty()) {
                JsonNode scene = scenes.get(json.path("scene").asInt(0));

                if (scene == null) {
                    throw new ModelLoadException("Default scene does not exist");
                }

                List<Integer> roots = new ArrayList<>();
                for (JsonNode index : scene.path("nodes")) {
                    roots.add(index.asInt(-1));
                }

                return roots;
            }

            Set<Integer> children = new HashSet<>();
            for (JsonNode node : json.path("nodes")) {
                for (JsonNode child : node.path("children")) {
                    children.add(child.asInt(-1));
                }
            }

            List<Integer> roots = new ArrayList<>();
            for (int node = 0; node < json.path("nodes").size(); node++) {
                if (!children.contains(node)) {
                    roots.add(node);
                }
            }

            return roots;
        }

        private void visit(int index, Mat4 parentWorld) throws ModelLoadException {
            JsonNode node = element("nodes", index);

            if (!activeNodes.add(index)) {
                throw new ModelLoadException("Node " + index + " is its own ancestor");
            }

            Mat4 world = parentWorld.mul(localMatrix(node));

            if (node.has("mesh")) {
                addMesh(node.path("mesh").asInt(-1), world);
            }

            for (JsonNode child : node.path("children")) {
                visit(child.asInt(-1), world);
            }

            activeNodes.remove(index);
        }

        private void addMesh(int meshIndex, Mat4 world) throws ModelLoadException {
            if (!meshes.containsKey(meshIndex)) {
                meshes.put(meshIndex, buildMesh(meshIndex));
            }

            for (Primitive primitive : meshes.get(meshIndex)) {
                parts.add(new ModelPart(primitive.mesh, primitive.material, world));
            }
        }

        private List<Primitive> buildMesh(int meshIndex) throws ModelLoadException {
            List<Primitive> primitives = new ArrayList<>();

            for (JsonNode primitive : element("meshes", meshIndex).path("primitives")) {
                buildPrimitive(primitive).ifSome(primitives::add);
            }

            return primitives;
        }

        private Maybe<Primitive> buildPrimitive(JsonNode primitive) throws ModelLoadException {
            int mode = primitive.path("mode").asInt(MODE_TRIANGLES);
            JsonNode attributes = primitive.path("attributes");

            if (mode == MODE_POINTS || !attributes.has("POSITION")) {
                return Maybe.none();
            }

            float[] positions = accessors.floats(attributes.path("POSITION").asInt(-1), 3);
            float[] normals = attributes.has("NORMAL")
                ? accessors.floats(attributes.path("NORMAL").asInt(-1), 3)
                : new float[0];

            float[] uvs = attributes.has("TEXCOORD_0")
                ? accessors.floats(attributes.path("TEXCOORD_0").asInt(-1), 2)
                : new float[0];

            int[] indices = primitive.has("indices")
                ? accessors.indices(primitive.path("indices").asInt(-1))
                : sequence(positions.length / 3);

            requireIndicesInRange(indices, positions.length / 3);

            Material material = primitive.has("material")
                ? material(primitive.path("material").asInt(-1))
                : Material.DEFAULT;

            try {
                if (mode == MODE_LINES || mode == MODE_LINE_STRIP || mode == MODE_LINE_LOOP) {
                    int[] lines = switch (mode) {
                        case MODE_LINES -> indices;
                        case MODE_LINE_STRIP -> lineStrip(indices, false);
                        default -> lineStrip(indices, true);
                    };

                    return Maybe.some(new Primitive(MeshData.lines(positions, lines), material));
                }

                int[] triangles = switch (mode) {
                    case MODE_TRIANGLES -> indices;
                    case MODE_TRIANGLE_STRIP -> triangleStrip(indices);
                    case MODE_TRIANGLE_FAN -> triangleFan(indices);
                    default -> throw new ModelLoadException("Unsupported primitive mode " + mode);
                };

                if (normals.length == 0) {
                    normals = smoothNormals(positions, triangles);
                }

                return Maybe.some(new Primitive(
                    MeshData.triangles(positions, normals, uvs, triangles),
                    material
                ));
            } catch (IllegalArgumentException e) {
                throw new ModelLoadException("Invalid mesh data: " + e.getMessage(), e);
            }
        }

        private Material material(int index) throws ModelLoadException {
            Material cached = materials.get(index);

            if (cached != null) {
                return cached;
            }

            JsonNode definition = element("materials", index);
            JsonNode pbr = definition.path("pbrMetallicRoughness");
            AlphaMode alpha = "BLEND".equals(definition.path("alphaMode").asText("OPAQUE"))
                ? AlphaMode.BLEND
                : AlphaMode.OPAQUE;
                
            float[] factor = pbr.has("baseColorFactor")
                ? floatArray(pbr.path("baseColorFactor"), 4, "baseColorFactor")
                : new float[] { 1f, 1f, 1f, 1f };

            float opacity = alpha == AlphaMode.OPAQUE ? 1f : clamp(factor[3]);

            Material material = new Material(
                new Color(clamp(factor[0]), clamp(factor[1]), clamp(factor[2]), opacity),
                baseColorTexture(pbr.path("baseColorTexture")),
                alpha,
                definition.path("doubleSided").asBoolean(false)
            );
            materials.put(index, material);

            return material;
        }

        // A texture the editor can't show becomes Missing, not an error, so the model still loads
        private TextureSlot baseColorTexture(JsonNode reference) throws ModelLoadException {
            if (!reference.has("index")) {
                return TextureSlot.NONE;
            }

            JsonNode texture = element("textures", reference.path("index").asInt(-1));

            if (!texture.has("source")) {
                return new TextureSlot.Missing("texture has no image source");
            }

            String uri = element("images", texture.path("source").asInt(-1)).path("uri").asText(null);

            if (uri == null || uri.startsWith("data:")) {
                return new TextureSlot.Missing("embedded images are not supported");
            }

            Path file = document.resolveInside(uri).unwrapOrNull();

            if (file == null) {
                return new TextureSlot.Missing("'" + uri + "' is outside the project or not a file path");
            }
            if (!Files.isRegularFile(file)) {
                return new TextureSlot.Missing("file not found: " + file);
            }

            return new TextureSlot.Ref(new TextureRef(file));
        }

        private Mat4 localMatrix(JsonNode node) throws ModelLoadException {
            if (node.has("matrix")) {
                return Mat4.fromArray(floatArray(node.path("matrix"), 16, "matrix"));
            }

            Vec3 translation = node.has("translation")
                ? vec3(floatArray(node.path("translation"), 3, "translation"))
                : Vec3.zero();
            Vec3 scale = node.has("scale")
                ? vec3(floatArray(node.path("scale"), 3, "scale"))
                : Vec3.one();
            Quat rotation = Quat.identity();

            if (node.has("rotation")) {
                float[] q = floatArray(node.path("rotation"), 4, "rotation");
                rotation = Quat.identity().set(q[0], q[1], q[2], q[3]).normalizeLocal();
            }

            return Mat4.compose(translation, rotation, scale);
        }

        private JsonNode element(String array, int index) throws ModelLoadException {
            JsonNode element = json.path(array).get(index);

            if (element == null) {
                throw new ModelLoadException("glTF " + array + "[" + index + "] does not exist");
            }

            return element;
        }
    }

    private record Primitive(MeshData mesh, Material material) {}

    private static void requireIndicesInRange(int[] indices, int vertexCount) throws ModelLoadException {
        for (int index : indices) {
            if (index < 0 || index >= vertexCount) {
                throw new ModelLoadException("Index " + index + " is outside " + vertexCount + " vertices");
            }
        }
    }

    private static Vec3 vec3(float[] values) {
        return new Vec3(values[0], values[1], values[2]);
    }

    private static float[] floatArray(JsonNode node, int length, String name) throws ModelLoadException {
        if (!node.isArray() || node.size() != length) {
            throw new ModelLoadException("'" + name + "' must be an array of " + length + " numbers");
        }

        float[] values = new float[length];
        for (int i = 0; i < length; i++) {
            values[i] = (float) node.get(i).asDouble();
        }

        return values;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static int[] sequence(int count) {
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            indices[i] = i;
        }

        return indices;
    }

    // Every other triangle is flipped so all of them keep the same winding
    private static int[] triangleStrip(int[] indices) {
        int triangles = Math.max(0, indices.length - 2);
        int[] result = new int[triangles * 3];

        for (int i = 0; i < triangles; i++) {
            boolean odd = i % 2 == 1;
            result[i * 3] = indices[odd ? i + 1 : i];
            result[i * 3 + 1] = indices[odd ? i : i + 1];
            result[i * 3 + 2] = indices[i + 2];
        }

        return result;
    }

    private static int[] triangleFan(int[] indices) {
        int triangles = Math.max(0, indices.length - 2);
        int[] result = new int[triangles * 3];

        for (int i = 0; i < triangles; i++) {
            result[i * 3] = indices[0];
            result[i * 3 + 1] = indices[i + 1];
            result[i * 3 + 2] = indices[i + 2];
        }

        return result;
    }

    private static int[] lineStrip(int[] indices, boolean closed) {
        int segments = indices.length < 2 ? 0 : indices.length - 1 + (closed ? 1 : 0);
        int[] result = new int[segments * 2];

        for (int i = 0; i < segments; i++) {
            result[i * 2] = indices[i];
            result[i * 2 + 1] = indices[(i + 1) % indices.length];
        }

        return result;
    }

    // Area-weighted vertex normals, for files that ship without them
    private static float[] smoothNormals(float[] positions, int[] triangles) {
        float[] normals = new float[positions.length];

        for (int i = 0; i + 2 < triangles.length; i += 3) {
            int a = triangles[i] * 3;
            int b = triangles[i + 1] * 3;
            int c = triangles[i + 2] * 3;
            float abx = positions[b] - positions[a];
            float aby = positions[b + 1] - positions[a + 1];
            float abz = positions[b + 2] - positions[a + 2];
            float acx = positions[c] - positions[a];
            float acy = positions[c + 1] - positions[a + 1];
            float acz = positions[c + 2] - positions[a + 2];
            float nx = aby * acz - abz * acy;
            float ny = abz * acx - abx * acz;
            float nz = abx * acy - aby * acx;

            for (int vertex : new int[] { a, b, c }) {
                normals[vertex] += nx;
                normals[vertex + 1] += ny;
                normals[vertex + 2] += nz;
            }
        }

        for (int i = 0; i < normals.length; i += 3) {
            float length = (float) Math.sqrt(
                normals[i] * normals[i] + normals[i + 1] * normals[i + 1] + normals[i + 2] * normals[i + 2]
            );

            if (length > 0f) {
                normals[i] /= length;
                normals[i + 1] /= length;
                normals[i + 2] /= length;
            } else {
                // Unused or degenerate vertices still need a valid unit normal
                normals[i + 1] = 1f;
            }
        }

        return normals;
    }
}
