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
    public static ArchetypeDefinitionJson from(ArchetypeDefinition def) {
        // only a handle has a path that can be stored
        String icon = null;
        if (def.getIcon() instanceof Maybe.Some<ArchetypeIcon>(var value)
            && value instanceof ArchetypeIcon.Handle handle
        ) {
            icon = handle.path();
        }

        return new ArchetypeDefinitionJson(def.getId(), def.getDisplayName(), icon, def.getParams());
    }

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
