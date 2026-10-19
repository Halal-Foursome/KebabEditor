package org.halalfoursome.kebabeditor.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.model.TextureSlot;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public final class ArchetypeIcons {

    public static final String EMPTY_OBJECT = "archetypes/empty.svg";

    private static final int TEXTURE_SIZE = 128;

    private final Map<ArchetypeIcon, Maybe<TextureSlot>> cache = new ConcurrentHashMap<>();

    public Maybe<TextureSlot> textureFor(ArchetypeDefinition definition) {
        return definition.getIcon().andThen(this::textureFor);
    }

    public Maybe<TextureSlot> textureFor(ArchetypeIcon icon) {
        return cache.computeIfAbsent(icon, this::load);
    }

    public Maybe<TextureSlot> emptyObject() {
        return textureFor(new ArchetypeIcon.Bundled(EMPTY_OBJECT));
    }

    private Maybe<TextureSlot> load(ArchetypeIcon icon) {
        return switch (icon) {
            case ArchetypeIcon.Bundled bundled -> vector(new FlatSVGIcon(bundled.resource()), bundled.resource());
            case ArchetypeIcon.Handle handle -> loadFile(handle);
            case ArchetypeIcon.Loaded loaded -> raster(loaded.icon(), "an in-memory image");
        };
    }

    private Maybe<TextureSlot> loadFile(ArchetypeIcon.Handle handle) {
        if (handle.path().toLowerCase().endsWith(".svg")) {
            return vector(new FlatSVGIcon(new File(handle.path())), handle.path());
        }

        return handle.loaded().isSome()
            ? raster(handle.loaded().unwrap().icon(), handle.path())
            : failed(handle.path(), "not a readable image");
    }

    private Maybe<TextureSlot> vector(FlatSVGIcon svg, String source) {
        if (!svg.hasFound()) {
            return failed(source, "file not found");
        }

        FlatSVGIcon icon = svg.derive(TEXTURE_SIZE, TEXTURE_SIZE);

        BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }

        return encode(image, source);
    }

    private Maybe<TextureSlot> raster(BufferedImage image, String source) {
        return encode(image, source);
    }

    private Maybe<TextureSlot> encode(BufferedImage image, String source) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try {
            if (!ImageIO.write(image, "png", bytes)) {
                return failed(source, "cannot be encoded");
            }
        } catch (IOException e) {
            return failed(source, e.getMessage());
        }

        return Maybe.some(TextureSlot.Embedded.of(bytes.toByteArray()));
    }

    private static Maybe<TextureSlot> failed(String source, String reason) {
        LogService.get().warn("Cannot use icon " + source + ": " + reason);

        return Maybe.none();
    }
}
