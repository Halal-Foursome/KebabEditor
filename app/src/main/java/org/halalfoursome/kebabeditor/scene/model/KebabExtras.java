package org.halalfoursome.kebabeditor.scene.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Access to editor-owned data under a node's {@code extras.kebab}. The scene
 * writer keeps unknown keys there as they are, so components stored here
 * round-trip through save and load untouched.
 */
final class KebabExtras {

    private static final String EXTRAS = "extras";
    private static final String KEBAB = "kebab";

    private KebabExtras() {}

    static JsonNode read(SceneNode node, String key) {
        JsonNode raw = node.getRawJson();
        return raw == null ? MissingNode.getInstance() : raw.path(EXTRAS).path(KEBAB).path(key);
    }

    static ObjectNode writable(SceneNode node) {
        ObjectNode raw;
        if (node.getRawJson() instanceof ObjectNode existing) {
            raw = existing;
        } else {
            raw = JsonNodeFactory.instance.objectNode();
            node.setRawJson(raw);
        }

        ObjectNode extras = raw.get(EXTRAS) instanceof ObjectNode e ? e : raw.putObject(EXTRAS);
        return extras.get(KEBAB) instanceof ObjectNode k ? k : extras.putObject(KEBAB);
    }
}
