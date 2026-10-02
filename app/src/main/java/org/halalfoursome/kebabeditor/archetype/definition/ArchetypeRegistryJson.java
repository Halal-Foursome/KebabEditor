package org.halalfoursome.kebabeditor.archetype.definition;

import java.util.Comparator;
import java.util.List;

public record ArchetypeRegistryJson(List<ArchetypeDefinitionJson> archetypes) {

    public ArchetypeRegistry toRegistry() {
        return new ArchetypeRegistry(
            archetypes.stream().map(ArchetypeDefinitionJson::toDefinition).toList()
        );
    }

    public static ArchetypeRegistryJson from(ArchetypeRegistry registry) {
        return new ArchetypeRegistryJson(
            registry.getArchetypes().stream()
                .sorted(Comparator.comparing(ArchetypeDefinition::getId))
                .map(ArchetypeDefinitionJson::from)
                .toList()
        );
    }
}
