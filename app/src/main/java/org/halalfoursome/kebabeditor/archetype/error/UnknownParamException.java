package org.halalfoursome.kebabeditor.archetype.error;

import java.util.Set;

public class UnknownParamException extends Exception {
    
    public UnknownParamException(String param, Set<String> defined) {
        super("Unknown param `"+param+"`, defined params are: " + defined.toString());
    }
}
