package org.halalfoursome.kebabeditor.archetype;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon.*;

public sealed interface ArchetypeIcon 
    permits
        Handle,
        Loaded
{
    record Handle(String path) implements ArchetypeIcon {
        public Optional<Loaded> loaded() {
            try {
                BufferedImage image = ImageIO.read(new File(path));

                return Optional.ofNullable(image).map(Loaded::new);
            } catch (IOException e) {
                return Optional.empty();
            }
        }
    }

    public record Loaded(BufferedImage icon) implements ArchetypeIcon {}
}
