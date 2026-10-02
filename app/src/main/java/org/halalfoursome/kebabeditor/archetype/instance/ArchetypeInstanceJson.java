package org.halalfoursome.kebabeditor.archetype.instance;

import java.util.LinkedHashMap;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.error.InvalidParamType;
import org.halalfoursome.kebabeditor.archetype.error.ParamDecodeException;
import org.halalfoursome.kebabeditor.archetype.error.UnknownParamException;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record ArchetypeInstanceJson(
    String id,
    Map<String, JsonNode> params
) {
    public static ArchetypeInstanceJson from(ArchetypeInstance instance) {
        Map<String, JsonNode> params = new LinkedHashMap<>();

        for (var entry : instance.getDefinition().getParams().entrySet()) {
            try {
                params.put(
                    entry.getKey(),
                    ParamJson.encode(entry.getValue(), instance.get(entry.getKey()))
                );
            } catch (UnknownParamException e) {
                throw new IllegalStateException("Param vanished from its own definition", e);
            }
        }

        return new ArchetypeInstanceJson(instance.archetypeId(), params);
    }

    public NodeArchetype toInstance(ArchetypeRegistry registry) {        
        switch (registry.find(id)) {
            case Maybe.None<ArchetypeDefinition> _: {
                System.err.println("Unknown archetype `" + id + "`, keeping it unresolved");
                return unresolved();
            }

            case Maybe.Some<ArchetypeDefinition> found: {
                ArchetypeDefinition def = found.value();
                ArchetypeInstance instance = new ArchetypeInstance(def);

                try {
                    for (var entry : def.getParams().entrySet()) {
                        JsonNode raw = params.get(entry.getKey());

                        if (raw != null) {
                            instance.set(entry.getKey(), ParamJson.decode(entry.getValue(), raw));
                        }
                    }
                } catch (ParamDecodeException | InvalidParamType | UnknownParamException e) {
                    System.err.println("Archetype `" + id + "` is unresolved: " + e.getMessage());
                    return unresolved();
                }

                return instance;
            }
        }
    }

    public UnresolvedArchetype unresolved() {
        ObjectNode raw = JsonNodeFactory.instance.objectNode();
        raw.put("id", id);
        raw.set("params", JsonNodeFactory.instance.objectNode().setAll(params));
        
        return new UnresolvedArchetype(id, raw);
    }
}
