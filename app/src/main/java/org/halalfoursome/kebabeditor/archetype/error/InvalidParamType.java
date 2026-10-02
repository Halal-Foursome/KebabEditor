package org.halalfoursome.kebabeditor.archetype.error;

public class InvalidParamType extends Exception {

    public InvalidParamType(Class<?> found, Class<?> expected) {
        super(
            "Invalid param type: `"
                + found == null ? "NULL" : found.getSimpleName()
                + "`, expected: `"+expected.getSimpleName()+"`"
        );
    }
}
