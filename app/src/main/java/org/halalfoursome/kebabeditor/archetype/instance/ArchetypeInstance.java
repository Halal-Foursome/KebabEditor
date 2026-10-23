package org.halalfoursome.kebabeditor.archetype.instance;

import java.util.HashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ParamSpec;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.error.InvalidParamType;
import org.halalfoursome.kebabeditor.archetype.error.UnknownParamException;
import org.halalfoursome.kebabeditor.math.Vec3;

import lombok.Getter;
import lombok.NonNull;

public final class ArchetypeInstance implements NodeArchetype {
    
    @Getter
    private ArchetypeDefinition definition;
    private Map<String, Object> values;

    public ArchetypeInstance(ArchetypeDefinition def) {
        definition = def;
        values = new HashMap<>();

        for (var entry : definition.getParams().entrySet()) {
            values.put(entry.getKey(), entry.getValue().defaultValue());
        }        
    }

    @Override 
    public String archetypeId() {
        return definition.getId();
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
        ParamSpec spec = definition.getParams().get(name);

        if (spec == null) {
            throw new UnknownParamException(name, definition.getParams().keySet());
        }

        var expected = spec.type().javaClass();

        if (value == null) {
            throw new InvalidParamType(null, expected);
        }

        var found = value.getClass();

        if (!expected.isInstance(value)) {
            throw new InvalidParamType(found, expected);
        }

        values.put(name, value);
    }

    /** A copy of the current values, to hand back to {@link #restoreValues} later. */
    public Map<String, Object> copyValues() {
        Map<String, Object> copy = new HashMap<>();
        values.forEach((name, value) -> copy.put(name, copyOf(value)));
        return copy;
    }

    /** Puts back values from {@link #copyValues}, skipping any the definition no longer accepts. */
    public void restoreValues(Map<String, Object> saved) {
        values.clear();
        saved.forEach((name, value) -> {
            ParamSpec spec = definition.getParams().get(name);
            if (spec != null && spec.type().javaClass().isInstance(value)) {
                values.put(name, copyOf(value));
            }
        });
    }

    // Vec3 is the only mutable param type
    private static Object copyOf(Object value) {
        return value instanceof Vec3 vec ? vec.copy() : value;
    }
}
