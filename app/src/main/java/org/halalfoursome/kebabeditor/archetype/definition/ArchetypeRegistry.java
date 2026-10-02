package org.halalfoursome.kebabeditor.archetype.definition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;

public class ArchetypeRegistry {

    @Getter
    private final Set<ArchetypeDefinition> archetypes;

    public ArchetypeRegistry() {
        this.archetypes = new HashSet<>();
    }

    public ArchetypeRegistry(Collection<ArchetypeDefinition> archetypes) {
        this.archetypes = new HashSet<>(archetypes);
    }

    // Definitions are equal by id, so a plain Set.add would silently drop a duplicate
    public void add(ArchetypeDefinition definition) {
        if (!archetypes.add(definition)) {
            throw new IllegalArgumentException(
                "Archetype '" + definition.getId() + "' already exists"
            );
        }
    }

    public void remove(ArchetypeDefinition definition) {
        archetypes.remove(definition);
    }
}
