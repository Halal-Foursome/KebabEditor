package org.halalfoursome.kebabeditor.archetype;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
public class ArchetypeCatalog {

    @Getter 
    private final ArchetypeRegistry registry;
    
    public Maybe<ArchetypeDefinition> find(String id) {
        return BuiltinArchetypes.find(id)
            .orElse(() -> registry.find(id));
    }

    public boolean isBuiltin(String id) {
        return BuiltinArchetypes.find(id).isSome();
    }

    public Collection<ArchetypeDefinition> all() {
        Set<ArchetypeDefinition> all = new LinkedHashSet<>(BuiltinArchetypes.all());
        all.addAll(registry.getArchetypes());
        return all;
    }
}
