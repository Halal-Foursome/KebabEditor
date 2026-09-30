package org.halalfoursome.kebabeditor.archetype;

import java.awt.image.BufferedImage;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon.*;

public sealed interface ArchetypeIcon 
    permits
        Handle,
        Loaded
{
    public record Handle(String path) implements ArchetypeIcon {}

    public record Loaded(BufferedImage icon) implements ArchetypeIcon {}
}
