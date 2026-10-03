package org.halalfoursome.kebabeditor.archetype.error;

import org.halalfoursome.kebabeditor.archetype.ParamType;

import com.fasterxml.jackson.databind.JsonNode;

public class ParamDecodeException extends Exception {

    public ParamDecodeException(ParamType type, JsonNode node) {
        super("Cannot decode `" + node + "` as " + type.displayName());
    }
}
