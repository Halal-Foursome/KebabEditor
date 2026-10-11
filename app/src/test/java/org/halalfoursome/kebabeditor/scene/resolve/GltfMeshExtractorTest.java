package org.halalfoursome.kebabeditor.scene.resolve;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.AlphaMode;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.model.PrimitiveType;
import org.halalfoursome.kebabeditor.model.TextureSlot;
import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class GltfMeshExtractorTest {

    private static final float EPS = 1e-4f;
    private static final float[] TRIANGLE = { 0, 0, 0, 1, 0, 0, 0, 1, 0 };
    private static final float[] QUAD = { 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0 };

    private final GltfMeshExtractor extractor = new GltfMeshExtractor();

    @TempDir
    Path directory;

    // Builds a one-mesh glTF whose buffer holds positions and, optionally, ushort indices
    private static final class Model {

        float[] positions = TRIANGLE;
        int[] indices = { 0, 1, 2 };
        String primitiveExtras = "";
        String nodes = "[{\"mesh\":0}]";
        String sceneNodes = "[0]";
        String extra = "";

        Model positions(float[] positions) {
            this.positions = positions;
            return this;
        }

        Model indices(int... indices) {
            this.indices = indices;
            return this;
        }

        Model primitive(String extras) {
            this.primitiveExtras = extras;
            return this;
        }

        Model nodes(String nodes, String sceneNodes) {
            this.nodes = nodes;
            this.sceneNodes = sceneNodes;
            return this;
        }

        Model extra(String extra) {
            this.extra = extra;
            return this;
        }

        byte[] bytes() {
            int indexBytes = indices == null ? 0 : (indices.length * 2 + 3) / 4 * 4;
            ByteBuffer buffer = ByteBuffer
                .allocate(positions.length * 4 + indexBytes)
                .order(ByteOrder.LITTLE_ENDIAN);

            for (float value : positions) {
                buffer.putFloat(value);
            }
            if (indices != null) {
                for (int index : indices) {
                    buffer.putShort((short) index);
                }
            }

            return buffer.array();
        }

        String json(String bufferMember) {
            int vertices = positions.length / 3;
            int positionBytes = positions.length * 4;
            String indexParts = indices == null
                ? ""
                : ",{\"buffer\":0,\"byteOffset\":" + positionBytes + ",\"byteLength\":" + indices.length * 2 + "}";
            String indexAccessor = indices == null
                ? ""
                : ",{\"bufferView\":1,\"componentType\":5123,\"count\":" + indices.length + ",\"type\":\"SCALAR\"}";

            return "{\"asset\":{\"version\":\"2.0\"},"
                + "\"scene\":0,\"scenes\":[{\"nodes\":" + sceneNodes + "}],"
                + "\"nodes\":" + nodes + ","
                + "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0}"
                + (indices == null ? "" : ",\"indices\":1") + primitiveExtras + "}]}],"
                + "\"buffers\":[{" + bufferMember + "\"byteLength\":" + bytes().length + "}],"
                + "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":" + positionBytes + "}" + indexParts + "],"
                + "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":" + vertices + ",\"type\":\"VEC3\"}"
                + indexAccessor + "]"
                + extra + "}";
        }

        String inlineJson() {
            return json("\"uri\":\"" + dataUri(bytes()) + "\",");
        }
    }

    private static String dataUri(byte[] bytes) {
        return "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private List<ModelPart> extract(String json) throws Exception {
        return extractor.extract(new ObjectMapper().readTree(json), directory);
    }

    private List<ModelPart> extract(Model model) throws Exception {
        return extract(model.inlineJson());
    }

    private static Vec3 origin(ModelPart part) {
        return part.transform().transformPoint(Vec3.zero());
    }

    @Test
    void triangleBecomesOnePartWithGeneratedNormals() throws Exception {
        List<ModelPart> parts = extract(new Model());

        assertEquals(1, parts.size());
        MeshData mesh = parts.get(0).mesh();
        assertEquals(PrimitiveType.TRIANGLES, mesh.type());
        assertEquals(3, mesh.vertexCount());
        assertArrayEquals(new int[] { 0, 1, 2 }, mesh.indices());
        assertTrue(mesh.hasNormals());
        assertArrayEquals(new float[] { 0, 0, 1, 0, 0, 1, 0, 0, 1 }, mesh.normals(), EPS);
        assertEquals(Material.DEFAULT, parts.get(0).material());
        assertTrue(origin(parts.get(0)).approxEquals(Vec3.zero(), EPS));
    }

    @Test
    void nodeTransformsComposeDownTheHierarchy() throws Exception {
        Model model = new Model().nodes(
            "[{\"translation\":[10,0,0],\"scale\":[2,2,2],\"children\":[1]},"
                + "{\"translation\":[1,0,0],\"mesh\":0}]",
            "[0]"
        );

        List<ModelPart> parts = extract(model);

        // The parent's scale of 2 applies to the child's offset of 1
        assertTrue(origin(parts.get(0)).approxEquals(new Vec3(12, 0, 0), EPS));
    }

    @Test
    void matrixNodesAreReadColumnMajor() throws Exception {
        Model model = new Model().nodes(
            "[{\"mesh\":0,\"matrix\":[1,0,0,0, 0,1,0,0, 0,0,1,0, 5,6,7,1]}]",
            "[0]"
        );

        assertTrue(origin(extract(model).get(0)).approxEquals(new Vec3(5, 6, 7), EPS));
    }

    @Test
    void rotationQuaternionIsApplied() throws Exception {
        double half = Math.sqrt(0.5);
        Model model = new Model().nodes(
            "[{\"mesh\":0,\"rotation\":[0,0," + half + "," + half + "]}]",
            "[0]"
        );

        Vec3 rotated = extract(model).get(0).transform().transformPoint(new Vec3(1, 0, 0));

        assertTrue(rotated.approxEquals(new Vec3(0, 1, 0), EPS), rotated.toString());
    }

    @Test
    void instancesOfOneMeshShareTheSameMeshData() throws Exception {
        Model model = new Model().nodes(
            "[{\"mesh\":0,\"translation\":[1,0,0]},{\"mesh\":0,\"translation\":[2,0,0]}]",
            "[0,1]"
        );

        List<ModelPart> parts = extract(model);

        assertEquals(2, parts.size());
        assertSame(parts.get(0).mesh(), parts.get(1).mesh());
    }

    @Test
    void nonIndexedPrimitiveGetsSequentialIndices() throws Exception {
        List<ModelPart> parts = extract(new Model().indices((int[]) null));

        assertArrayEquals(new int[] { 0, 1, 2 }, parts.get(0).mesh().indices());
    }

    @Test
    void triangleStripKeepsConsistentWinding() throws Exception {
        List<ModelPart> parts = extract(
            new Model().positions(QUAD).indices(0, 1, 2, 3).primitive(",\"mode\":5")
        );

        MeshData mesh = parts.get(0).mesh();
        assertArrayEquals(new int[] { 0, 1, 2, 2, 1, 3 }, mesh.indices());
        float[] normals = mesh.normals();
        for (int vertex = 0; vertex < 4; vertex++) {
            assertEquals(1f, normals[vertex * 3 + 2], EPS);
        }
    }

    @Test
    void triangleFanFansAroundTheFirstVertex() throws Exception {
        List<ModelPart> parts = extract(
            new Model().positions(QUAD).indices(0, 1, 2, 3).primitive(",\"mode\":6")
        );

        assertArrayEquals(new int[] { 0, 1, 2, 0, 2, 3 }, parts.get(0).mesh().indices());
    }

    @Test
    void lineStripBecomesLineSegments() throws Exception {
        List<ModelPart> parts = extract(new Model().primitive(",\"mode\":3"));

        MeshData mesh = parts.get(0).mesh();
        assertEquals(PrimitiveType.LINES, mesh.type());
        assertArrayEquals(new int[] { 0, 1, 1, 2 }, mesh.indices());
        assertTrue(!mesh.hasNormals());
    }

    @Test
    void pointPrimitivesAreSkipped() throws Exception {
        assertTrue(extract(new Model().primitive(",\"mode\":0")).isEmpty());
    }

    @Test
    void meshesWithoutNodesAreDrawnAtTheOrigin() throws Exception {
        Model model = new Model();
        String json = model.inlineJson()
            .replace("\"scene\":0,\"scenes\":[{\"nodes\":[0]}],", "")
            .replace("\"nodes\":[{\"mesh\":0}],", "");

        List<ModelPart> parts = extract(json);

        assertEquals(1, parts.size());
        assertTrue(origin(parts.get(0)).approxEquals(Vec3.zero(), EPS));
    }

    @Test
    void materialColorAlphaAndSidednessAreRead() throws Exception {
        Model model = new Model()
            .primitive(",\"material\":0")
            .extra(",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorFactor\":[1,0,0,0.5]},"
                + "\"alphaMode\":\"BLEND\",\"doubleSided\":true}]");

        Material material = extract(model).get(0).material();

        assertEquals(AlphaMode.BLEND, material.alpha());
        assertTrue(material.doubleSided());
        assertEquals(new Color(255, 0, 0, 128), material.baseColor());
    }

    @Test
    void opaqueMaterialsIgnoreFactorAlpha() throws Exception {
        Model model = new Model()
            .primitive(",\"material\":0")
            .extra(",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorFactor\":[0,1,0,0.2]}}]");

        Material material = extract(model).get(0).material();

        assertEquals(AlphaMode.OPAQUE, material.alpha());
        assertEquals(255, material.baseColor().getAlpha());
    }

    @Test
    void textureResolvesInsideTheModelDirectoryOnly() throws Exception {
        touch("tex/a.png");

        assertEquals(directory.resolve("tex/a.png"), pathOf(textureFor("tex/a.png")));
        assertMissing(textureFor("../evil.png"));
        assertMissing(textureFor("/etc/passwd"));
        assertMissing(textureFor("http://example.com/a.png"));
    }

    @Test
    void uriSpellingsFromDifferentExportersAreUnderstood() throws Exception {
        touch("my tex.png");
        touch("100%.png");
        touch("a+b.png");

        assertEquals(directory.resolve("my tex.png"), pathOf(textureFor("my tex.png")));
        assertEquals(directory.resolve("my tex.png"), pathOf(textureFor("my%20tex.png")));
        assertEquals(directory.resolve("100%.png"), pathOf(textureFor("100%.png")));
        assertEquals(directory.resolve("a+b.png"), pathOf(textureFor("a+b.png")));
    }

    @Test
    void malformedUrlsAndDriveLettersAreRejectedNotTreatedAsFiles() throws Exception {
        assertMissing(textureFor("http://exa mple.com/a b.png"));
        assertMissing(textureFor("file:///etc/passwd"));
        assertMissing(textureFor("C:/x.png"));
        assertMissing(textureFor("\\\\server\\share\\a.png"));
    }

    @Test
    void textureFileThatDoesNotExistIsMissingWithAReason() throws Exception {
        TextureSlot.Missing missing = assertMissing(textureFor("nope.png"));

        assertTrue(missing.reason().contains("not found"), missing.reason());
    }

    @Test
    void embeddedImageIsMissingNotSilentlyDropped() throws Exception {
        TextureSlot.Missing missing = assertMissing(textureFor("data:image/png;base64,AAAA"));

        assertTrue(missing.reason().contains("embedded"), missing.reason());
    }

    @Test
    void materialWithoutATextureHasNoTextureSlot() throws Exception {
        Model model = new Model()
            .primitive(",\"material\":0")
            .extra(",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorFactor\":[1,1,1,1]}}]");

        assertSame(TextureSlot.NONE, extract(model).get(0).material().baseTexture());
        assertSame(TextureSlot.NONE, Material.DEFAULT.baseTexture());
    }

    private void touch(String relative) throws IOException {
        Path file = directory.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] { 1 });
    }

    private static Path pathOf(TextureSlot slot) {
        return assertInstanceOf(TextureSlot.Ref.class, slot).texture().path();
    }

    private static TextureSlot.Missing assertMissing(TextureSlot slot) {
        return assertInstanceOf(TextureSlot.Missing.class, slot);
    }

    private TextureSlot textureFor(String uri) throws Exception {
        Model model = new Model()
            .primitive(",\"material\":0")
            .extra(",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorTexture\":{\"index\":0}}}],"
                + "\"textures\":[{\"source\":0}],\"images\":[{\"uri\":\"" + uri.replace("\\", "\\\\") + "\"}]");

        return extract(model).get(0).material().baseTexture();
    }

    @Test
    void normalizedUnsignedShortUvsAreScaledToUnitRange() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(36 + 12).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : TRIANGLE) {
            buffer.putFloat(value);
        }
        for (int uv : new int[] { 0, 0, 65535, 0, 0, 65535 }) {
            buffer.putShort((short) uv);
        }
        String json = "{\"asset\":{\"version\":\"2.0\"},"
            + "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0,\"TEXCOORD_0\":1}}]}],"
            + "\"buffers\":[{\"uri\":\"" + dataUri(buffer.array()) + "\",\"byteLength\":48}],"
            + "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":36},"
            + "{\"buffer\":0,\"byteOffset\":36,\"byteLength\":12}],"
            + "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"},"
            + "{\"bufferView\":1,\"componentType\":5123,\"normalized\":true,\"count\":3,\"type\":\"VEC2\"}]}";

        float[] uvs = extract(json).get(0).mesh().uvs();

        assertArrayEquals(new float[] { 0, 0, 1, 0, 0, 1 }, uvs, EPS);
    }

    @Test
    void glbFileWithEmbeddedBinaryChunkIsRead() throws Exception {
        Model model = new Model();
        Path file = directory.resolve("model.glb");
        Files.write(file, glb(model.json(""), model.bytes()));

        List<ModelPart> parts = extractor.extract(file);

        assertEquals(1, parts.size());
        assertEquals(3, parts.get(0).mesh().vertexCount());
    }

    @Test
    void gltfFileWithExternalBufferIsRead() throws Exception {
        Model model = new Model();
        Files.write(directory.resolve("data.bin"), model.bytes());
        Path file = directory.resolve("model.gltf");
        Files.writeString(file, model.json("\"uri\":\"data.bin\","));

        assertEquals(1, extractor.extract(file).size());
    }

    @Test
    void bufferOutsideTheModelDirectoryIsRejected() throws Exception {
        Model model = new Model();
        Path nested = Files.createDirectory(directory.resolve("nested"));
        Files.write(directory.resolve("data.bin"), model.bytes());
        Path file = nested.resolve("model.gltf");
        Files.writeString(file, model.json("\"uri\":\"../data.bin\","));

        assertThrows(ModelLoadException.class, () -> extractor.extract(file));
    }

    @Test
    void bufferMayClimbOutOfTheModelFolderInsideTheAssetRoot() throws Exception {
        Model model = new Model();
        Path nested = Files.createDirectory(directory.resolve("models"));
        Files.write(directory.resolve("data.bin"), model.bytes());
        Path file = nested.resolve("model.gltf");
        Files.writeString(file, model.json("\"uri\":\"../data.bin\","));

        assertEquals(1, extractor.extract(file, directory).size());
    }

    @Test
    void climbingOutOfTheAssetRootIsRejected() throws Exception {
        Model model = new Model();
        Path project = Files.createDirectories(directory.resolve("project/models"));
        Files.write(directory.resolve("secret.bin"), model.bytes());
        Path file = project.resolve("model.gltf");
        Files.writeString(file, model.json("\"uri\":\"../../secret.bin\","));

        assertThrows(
            ModelLoadException.class,
            () -> extractor.extract(file, directory.resolve("project"))
        );
    }

    @Test
    void texturesMayClimbInsideTheAssetRootButNotOutOfIt() throws Exception {
        Path models = Files.createDirectories(directory.resolve("project/models"));
        Path root = directory.resolve("project");
        Model model = new Model()
            .primitive(",\"material\":0")
            .extra(",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorTexture\":{\"index\":0}}}],"
                + "\"textures\":[{\"source\":0}],\"images\":[{\"uri\":\"../textures/a.png\"}]");
        Path file = models.resolve("model.gltf");

        Files.createDirectories(root.resolve("textures"));
        Files.write(root.resolve("textures/a.png"), new byte[] { 1 });

        Files.writeString(file, model.json("\"uri\":\"" + dataUri(model.bytes()) + "\","));
        assertEquals(
            root.resolve("textures/a.png"),
            pathOf(extractor.extract(file, root).get(0).material().baseTexture())
        );

        Files.writeString(
            file,
            model.json("\"uri\":\"" + dataUri(model.bytes()) + "\",").replace("../textures/a.png", "../../a.png")
        );
        assertMissing(extractor.extract(file, root).get(0).material().baseTexture());
    }

    @Test
    void symlinkLeadingOutOfTheAssetRootIsRejected() throws Exception {
        Path root = Files.createDirectories(directory.resolve("project"));
        Path outside = Files.createDirectories(directory.resolve("outside"));
        Files.write(outside.resolve("a.png"), new byte[] { 1 });

        try {
            Files.createSymbolicLink(root.resolve("link"), outside);
        } catch (UnsupportedOperationException | IOException e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "symlinks are not available");
        }

        Model model = new Model()
            .primitive(",\"material\":0")
            .extra(",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorTexture\":{\"index\":0}}}],"
                + "\"textures\":[{\"source\":0}],\"images\":[{\"uri\":\"link/a.png\"}]");
        Path file = root.resolve("model.gltf");
        Files.writeString(file, model.json("\"uri\":\"" + dataUri(model.bytes()) + "\","));

        assertMissing(extractor.extract(file, root).get(0).material().baseTexture());
    }

    @Test
    void modelOutsideTheAssetRootIsRejected() throws Exception {
        Path root = Files.createDirectories(directory.resolve("project"));
        Model model = new Model();
        Path file = directory.resolve("model.gltf");
        Files.writeString(file, model.inlineJson());

        assertThrows(ModelLoadException.class, () -> extractor.extract(file, root));
    }

    @Test
    void missingFileIsReported() {
        assertThrows(
            ModelLoadException.class,
            () -> extractor.extract(directory.resolve("nope.gltf"))
        );
    }

    @Test
    void accessorReadingPastItsBufferIsRejected() {
        String json = new Model().inlineJson().replace("\"count\":3,\"type\":\"VEC3\"", "\"count\":99,\"type\":\"VEC3\"");

        assertThrows(ModelLoadException.class, () -> extract(json));
    }

    @Test
    void indexOutsideTheVertexRangeIsRejected() {
        Model model = new Model().indices(0, 1, 7);

        assertThrows(ModelLoadException.class, () -> extract(model));
    }

    @Test
    void nodeThatIsItsOwnAncestorIsRejected() {
        Model model = new Model().nodes("[{\"mesh\":0,\"children\":[0]}]", "[0]");

        assertThrows(ModelLoadException.class, () -> extract(model));
    }

    @Test
    void sparseAccessorsAreRejected() {
        String json = new Model().inlineJson().replace("\"type\":\"VEC3\"", "\"type\":\"VEC3\",\"sparse\":{}");

        assertThrows(ModelLoadException.class, () -> extract(json));
    }

    @Test
    void nonObjectDocumentIsRejected() throws IOException {
        JsonNode array = new ObjectMapper().readTree("[]");

        assertThrows(ModelLoadException.class, () -> extractor.extract(array, directory));
    }

    private static byte[] glb(String json, byte[] binary) {
        byte[] jsonBytes = pad(json.getBytes(StandardCharsets.UTF_8), (byte) ' ');
        byte[] binBytes = pad(binary, (byte) 0);
        int total = 12 + 8 + jsonBytes.length + 8 + binBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(0x46546C67).putInt(2).putInt(total);
        buffer.putInt(jsonBytes.length).putInt(0x4E4F534A).put(jsonBytes);
        buffer.putInt(binBytes.length).putInt(0x004E4942).put(binBytes);

        return buffer.array();
    }

    private static byte[] pad(byte[] bytes, byte fill) {
        byte[] padded = java.util.Arrays.copyOf(bytes, (bytes.length + 3) / 4 * 4);
        java.util.Arrays.fill(padded, bytes.length, padded.length, fill);

        return padded;
    }
}
