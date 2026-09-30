package org.halalfoursome.kebabeditor.archetype.definition;

import java.util.Map;
import java.util.Optional;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon;
import org.halalfoursome.kebabeditor.archetype.ParamType;

import lombok.Data;

@Data 
public class ArchetypeDefinition {
    
    private String id;
    private String displayName;
    private Optional<ArchetypeIcon> icon;
    private Map<String, ParamType> params;
}
