package org.halalfoursome.kebabeditor.archetype.instance;

import java.util.HashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.error.InvalidParamType;
import org.halalfoursome.kebabeditor.archetype.error.UnknownParamException;

import lombok.NonNull;

public class ArchetypeInstance {
    
    private ArchetypeDefinition definition;
    private Map<String, Object> values;

    public ArchetypeInstance(ArchetypeDefinition def) {
        definition = def;
        values = new HashMap<>();

        for (var entry : definition.getParams().entrySet()) {
            values.put(entry.getKey(), entry.getValue().defaultValue());
        }        
    }

    public Object get(@NonNull String name) throws UnknownParamException {
        try {
            return values.getOrDefault(
                name, 
                definition.getParams()
                    .get(name)
                    .defaultValue()
            );
        } catch (Exception _) {
            throw new UnknownParamException(name, definition.getParams().keySet());
        }
    }

    public void set(String name, Object value) 
        throws 
            UnknownParamException, 
            InvalidParamType 
    {
        ParamType param = definition.getParams().get(name);

        if (param == null) {
            throw new UnknownParamException(name, definition.getParams().keySet());
        }

        var expected = param.javaClass();

        if (value == null) {
            throw new InvalidParamType(null, expected);
        }

        var found = value.getClass();

        if (!expected.isInstance(value)) {
            throw new InvalidParamType(found, expected);
        }

        values.put(name, value);
    }
}
