package org.halalfoursome.kebabeditor.archetype.definition;

import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon;
import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.jspecify.annotations.Nullable;

public record ArchetypeDefinitionJson(
    String id,
    String displayName,
    @Nullable String icon,
    Map<String, ParamType> params
) {
    public ArchetypeDefinition toDefinition() {
        return new ArchetypeDefinition(
            id,
            displayName,
            icon == null
                ? Maybe.none()
                : Maybe.some(new ArchetypeIcon.Handle(icon)),
            params
        );
    }
}
