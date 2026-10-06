package org.halalfoursome.kebabeditor.archetype.error;

import org.halalfoursome.kebabeditor.error.KebabException;

public class InvalidParamType extends KebabException {

    public InvalidParamType(Class<?> found, Class<?> expected) {
        super(
            "Invalid param type: `"
                + (found == null ? "NULL" : found.getSimpleName())
                + "`, expected: `" + expected.getSimpleName() + "`"
        );
    }
}
