package org.halalfoursome.kebabeditor.archetype.instance;

import java.util.HashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;

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
}
