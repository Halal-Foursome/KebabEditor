package org.halalfoursome.kebabeditor.archetype.error;

import java.util.Set;

import org.halalfoursome.kebabeditor.error.KebabException;

public class UnknownParamException extends KebabException {
    
    public UnknownParamException(String param, Set<String> defined) {
        super("Unknown param `"+param+"`, defined params are: " + defined.toString());
    }
}
