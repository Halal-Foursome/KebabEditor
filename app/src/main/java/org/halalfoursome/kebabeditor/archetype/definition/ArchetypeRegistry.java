package org.halalfoursome.kebabeditor.archetype.definition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

public class ArchetypeRegistry {

    @Getter
    @JsonProperty("archetypes")
    private final Set<ArchetypeDefinition> archetypes;

    public ArchetypeRegistry() {
        this.archetypes = new HashSet<>();
    }

    @JsonCreator
    public ArchetypeRegistry(
        @JsonProperty("archetypes")
        Collection<ArchetypeDefinition> archetypes
    ) {
        this.archetypes = new HashSet<>(archetypes);
    }
}
