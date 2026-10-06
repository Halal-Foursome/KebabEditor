package org.halalfoursome.kebabeditor.scene.error;

import org.halalfoursome.kebabeditor.error.KebabException;

public class SceneException extends KebabException {

    public SceneException(String message) {
        super(message);
    }

    public SceneException(String message, Throwable cause) {
        super(message, cause);
    }
}
