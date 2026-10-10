package org.halalfoursome.kebabeditor.archetype;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
public class ArchetypeCatalog {

    private final ArchetypeRegistry registry;
    
    public Maybe<ArchetypeDefinition> find(String id) {
        return BuiltinArchetypes.find(id)
            .orElse(() -> registry.find(id));
    }
}
