package org.halalfoursome.kebabeditor.archetype.error;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.error.KebabException;

import com.fasterxml.jackson.databind.JsonNode;

public class ParamDecodeException extends KebabException {

    public ParamDecodeException(ParamType type, JsonNode node) {
        super("Cannot decode `" + node + "` as " + type.displayName());
    }
}
