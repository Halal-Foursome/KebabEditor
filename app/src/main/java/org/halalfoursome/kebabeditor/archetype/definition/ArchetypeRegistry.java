package org.halalfoursome.kebabeditor.archetype.definition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.halalfoursome.kebabeditor.utils.Maybe;

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

    public Maybe<ArchetypeDefinition> find(String id) {
        return Maybe.fromOptional(
            archetypes.stream()
                .filter(def -> def.getId().equals(id))
                .findAny()
        );
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
