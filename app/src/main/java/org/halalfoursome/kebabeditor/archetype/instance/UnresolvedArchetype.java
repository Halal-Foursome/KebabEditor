package org.halalfoursome.kebabeditor.archetype.instance;

import com.fasterxml.jackson.databind.JsonNode;

public record UnresolvedArchetype(String id, JsonNode rawJson) implements NodeArchetype {
    
    @Override 
    public String archetypeId() {
        return id;
    }
}
