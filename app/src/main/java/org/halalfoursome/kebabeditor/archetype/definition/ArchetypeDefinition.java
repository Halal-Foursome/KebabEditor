package org.halalfoursome.kebabeditor.archetype.definition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon;
import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Data
@AllArgsConstructor 
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ArchetypeDefinition {

    @EqualsAndHashCode.Include
    @Setter(value = AccessLevel.NONE)
    private String id;
    private String displayName;
    private Maybe<ArchetypeIcon> icon;
    private Map<String, ParamType> params;

    public ArchetypeDefinition duplicateAs(String newId) {
        return new ArchetypeDefinition(
            newId,
            this.displayName,
            this.icon,
            new LinkedHashMap<>(this.params)
        );
    }
}
