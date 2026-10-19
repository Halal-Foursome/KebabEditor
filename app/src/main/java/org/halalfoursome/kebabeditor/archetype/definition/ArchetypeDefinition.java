package org.halalfoursome.kebabeditor.archetype.definition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon;
import org.halalfoursome.kebabeditor.archetype.ParamSpec;
import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor 
@RequiredArgsConstructor 
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ArchetypeDefinition {

    @EqualsAndHashCode.Include
    @Setter(value = AccessLevel.NONE)
    private final String id;
    private String displayName = "<unnamed>";
    private Maybe<ArchetypeIcon> icon = Maybe.none();
    private boolean single = false;
    private Map<String, ParamSpec> params = new LinkedHashMap<>();

    public ArchetypeDefinition duplicateAs(String newId) {
        return new ArchetypeDefinition(
            newId,
            this.displayName,
            this.icon,
            this.single,
            new LinkedHashMap<>(this.params)
        );
    }

    public ArchetypeDefinition withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ArchetypeDefinition withIcon(ArchetypeIcon icon) {
        this.icon = Maybe.some(icon);
        return this;
    }

    public ArchetypeDefinition withSingle() {
        this.single = true;
        return this;
    }

    public ArchetypeDefinition withParam(String name, ParamType type) {
        this.params.put(name, ParamSpec.of(type));
        return this;
    }

    public ArchetypeDefinition withParam(String name, ParamType type, Object defaultValue) {
        this.params.put(name, new ParamSpec(type, defaultValue));
        return this;
    }
}
