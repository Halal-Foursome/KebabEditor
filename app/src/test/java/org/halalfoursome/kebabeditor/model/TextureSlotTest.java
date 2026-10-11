package org.halalfoursome.kebabeditor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class TextureSlotTest {

    private static final int MAGENTA = 0xFFFF00FF;
    private static final int BLACK = 0xFF000000;

    private BufferedImage loadMissingImage() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/" + TextureSlot.MISSING_IMAGE)) {
            assertNotNull(stream, "missing-texture resource is not on the classpath");

            return ImageIO.read(stream);
        }
    }

    @Test
    void missingTextureImageIsA64PixelMagentaAndBlackChecker() throws Exception {
        BufferedImage image = loadMissingImage();

        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
        assertEquals(MAGENTA, image.getRGB(0, 0));
        assertEquals(BLACK, image.getRGB(8, 0));
        assertEquals(BLACK, image.getRGB(0, 8));
        assertEquals(MAGENTA, image.getRGB(8, 8));
        assertEquals(MAGENTA, image.getRGB(63, 63));
    }

}
