package org.halalfoursome.kebabeditor.scene.error;

import org.halalfoursome.kebabeditor.error.KebabException;

public class ModelLoadException extends KebabException {

    public ModelLoadException(String message) {
        super(message);
    }

    public ModelLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
