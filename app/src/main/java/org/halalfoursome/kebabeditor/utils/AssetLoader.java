package org.halalfoursome.kebabeditor.utils;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public class AssetLoader {
    private static AssetLoader instance;
    private static final Path[] DEV_RESOURCE_ROOTS = new Path[] {
        Paths.get("src", "main", "resources"),
    };

    private AssetLoader() {}

    public static synchronized AssetLoader getInstance() {
        if (instance == null) {
            instance = new AssetLoader();
        }
        return instance;
    }

    private InputStream openResourceStream(String path) throws IOException {
        String normalized = path.startsWith("/") ? path.substring(1) : path;

        InputStream stream = getClass().getResourceAsStream("/" + normalized);
        if (stream != null) {
            return stream;
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            stream = contextClassLoader.getResourceAsStream(normalized);
            if (stream != null) {
                return stream;
            }
        }

        for (Path root : DEV_RESOURCE_ROOTS) {
            Path candidate = root.resolve(normalized);
            if (Files.isRegularFile(candidate)) {
                return Files.newInputStream(candidate);
            }
        }

        return null;
    }

    public synchronized Font loadFont(String path, float size, boolean ligatures) {
        try (InputStream is = openResourceStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Font resource not found: " + path);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);

            if (ligatures) {
                Map<TextAttribute, Object> attrs = new HashMap<>(font.getAttributes());
                attrs.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
                font = font.deriveFont(attrs);
            }

            return font;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load font: " + path, e);
        }
    }

    public synchronized FavIcon loadIcon(String path) {
        FlatSVGIcon svgIcon = new FlatSVGIcon(
            path,
            KebabStyle.ICON_WIDTH,
            KebabStyle.ICON_HEIGHT
        );
        BufferedImage img = new BufferedImage(
            svgIcon.getIconWidth(),
            svgIcon.getIconHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = img.createGraphics();
        svgIcon.paintIcon(null, g, 0, 0);
        g.dispose();

        return new FavIcon(img);
    }
}
