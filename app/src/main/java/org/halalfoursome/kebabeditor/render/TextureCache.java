package org.halalfoursome.kebabeditor.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.model.TextureSlot;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;

/**
 * One mipmapped GL texture per image. A {@link TextureSlot.Ref} loads its file,
 * a {@link TextureSlot.Missing} shows the bundled "missing" image so the problem
 * stays visible. Images are decoded with ImageIO (PNG, JPEG, BMP, GIF); one that
 * fails is reported once and the surface draws untextured.
 */
final class TextureCache {

    private final Map<String, Integer> textures = new HashMap<>();
    private final Set<String> failed = new HashSet<>();

    private static final String MISSING_KEY = "missing:" + TextureSlot.MISSING_IMAGE;

    Maybe<Integer> get(GL3 gl, TextureSlot slot) {
        return switch (slot) {
            case TextureSlot.None _ -> Maybe.none();
            case TextureSlot.Ref ref -> get(gl, ref.texture().path().toString(), () -> ImageIO.read(ref.texture().path().toFile()));
            case TextureSlot.Embedded embedded -> get(gl, embedded.key(), () -> ImageIO.read(new ByteArrayInputStream(embedded.data())));
            case TextureSlot.Missing _ -> get(gl, MISSING_KEY, TextureCache::missingImage);
        };
    }

    private interface ImageSource {
        BufferedImage read() throws IOException;
    }

    private Maybe<Integer> get(GL3 gl, String key, ImageSource source) {
        Integer cached = textures.get(key);
        if (cached != null) {
            return Maybe.some(cached);
        }
        if (failed.contains(key)) {
            return Maybe.none();
        }

        try {
            BufferedImage image = source.read();
            if (image == null) {
                throw new IOException("unsupported image format");
            }
            int id = upload(gl, image);
            textures.put(key, id);
            return Maybe.some(id);
        } catch (IOException e) {
            failed.add(key);
            LogService.get().warn("Cannot load texture " + key + ": " + e.getMessage());
            return Maybe.none();
        }
    }

    private static BufferedImage missingImage() throws IOException {
        try (InputStream stream = TextureCache.class.getResourceAsStream("/" + TextureSlot.MISSING_IMAGE)) {
            if (stream == null) {
                throw new IOException("bundled image " + TextureSlot.MISSING_IMAGE + " not found");
            }
            return ImageIO.read(stream);
        }
    }

    // Rows go up top-first, which matches glTF's top-left UV origin without flipping.
    private static int upload(GL3 gl, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);

        ByteBuffer rgba = Buffers.newDirectByteBuffer(width * height * 4);
        for (int pixel : argb) {
            rgba.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel).put((byte) (pixel >>> 24));
        }
        rgba.flip();

        int[] id = new int[1];
        gl.glGenTextures(1, id, 0);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, id[0]);
        gl.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA8, width, height, 0, GL3.GL_RGBA, GL3.GL_UNSIGNED_BYTE, rgba);
        gl.glGenerateMipmap(GL3.GL_TEXTURE_2D);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);

        return id[0];
    }

    void dispose(GL3 gl) {
        int[] ids = textures.values().stream().mapToInt(Integer::intValue).toArray();
        if (ids.length > 0) {
            gl.glDeleteTextures(ids.length, ids, 0);
        }
        textures.clear();
        failed.clear();
    }
}
