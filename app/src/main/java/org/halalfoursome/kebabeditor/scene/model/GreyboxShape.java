package org.halalfoursome.kebabeditor.scene.model;

import java.util.Locale;

import org.halalfoursome.kebabeditor.utils.Maybe;

public enum GreyboxShape {
    BOX("Box"),
    SPHERE("Sphere");

    private final String displayName;

    GreyboxShape(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String jsonName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Maybe<GreyboxShape> fromJson(String value) {
        for (GreyboxShape shape : values()) {
            if (shape.jsonName().equals(value)) {
                return Maybe.some(shape);
            }
        }
        return Maybe.none();
    }
}
