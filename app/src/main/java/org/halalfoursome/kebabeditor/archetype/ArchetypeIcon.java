package org.halalfoursome.kebabeditor.archetype;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon.*;
import org.halalfoursome.kebabeditor.utils.Maybe;

public sealed interface ArchetypeIcon 
    permits
        Handle,
        Loaded
{
    record Handle(String path) implements ArchetypeIcon {
        public Maybe<Loaded> loaded() {
            try {
                BufferedImage image = ImageIO.read(new File(path));

                return Maybe.ofNullable(image).map(Loaded::new);
            } catch (IOException e) {
                return Maybe.none();
            }
        }
    }

    public record Loaded(BufferedImage icon) implements ArchetypeIcon {}
}
