package org.halalfoursome.kebabeditor.model;

import org.halalfoursome.kebabeditor.model.TextureSlot.*;

public sealed interface TextureSlot
    permits
        None,
        Ref,
        Missing
{
    String MISSING_IMAGE = "textures/missing.png";

    TextureSlot NONE = new None();

    public record None() implements TextureSlot {}

    public record Ref(TextureRef texture) implements TextureSlot {}

    public record Missing(String reason) implements TextureSlot {}
}
