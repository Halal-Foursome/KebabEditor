package org.halalfoursome.kebabeditor.model;

import java.awt.Color;
import java.util.Objects;

import org.halalfoursome.kebabeditor.utils.Maybe;

public record Material(
    Color baseColor,
    Maybe<TextureRef> baseTexture,
    AlphaMode alpha,
    boolean doubleSided
) {

    public static final Material DEFAULT = solid(new Color(180, 180, 185));

    public Material {
        Objects.requireNonNull(baseColor, "baseColor");
        Objects.requireNonNull(baseTexture, "baseTexture");
        Objects.requireNonNull(alpha, "alpha");
    }

    public static Material solid(Color color) {
        AlphaMode alpha = color.getAlpha() < 255 ? AlphaMode.BLEND : AlphaMode.OPAQUE;

        return new Material(color, Maybe.none(), alpha, false);
    }

    public Material withBaseColor(Color color) {
        return new Material(color, baseTexture, alpha, doubleSided);
    }
}
