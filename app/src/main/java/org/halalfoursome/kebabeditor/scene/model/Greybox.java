package org.halalfoursome.kebabeditor.scene.model;

import java.awt.Color;
import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A blockout primitive attached to a node, stored as
 * {@code extras.kebab.greybox = { "shape": "box", "size": [x, y, z], "color": [r, g, b, a] }}.
 *
 * The size is per-axis and applied on top of the node transform, whose scale
 * is uniform, so platforms of any proportion are possible.
 */
public record Greybox(GreyboxShape shape, Vec3 size, Color color) {

    public static final Color DEFAULT_COLOR = new Color(168, 170, 176);

    private static final String KEY = "greybox";
    private static final String SHAPE = "shape";
    private static final String SIZE = "size";
    private static final String COLOR = "color";

    public Greybox {
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(color, "color");
        size = size.copy();
    }

    public static Greybox unit(GreyboxShape shape) {
        return new Greybox(shape, Vec3.one(), DEFAULT_COLOR);
    }

    @Override
    public Vec3 size() {
        return size.copy();
    }

    /** The node's greybox, or none when it has none or the stored data is malformed. */
    public static Maybe<Greybox> of(SceneNode node) {
        JsonNode json = KebabExtras.read(node, KEY);
        if (!json.isObject()) {
            return Maybe.none();
        }

        Maybe<GreyboxShape> shape = GreyboxShape.fromJson(json.path(SHAPE).asText(""));
        float[] size = numbers(json.path(SIZE), 3, new float[] { 1, 1, 1 });
        float[] color = numbers(json.path(COLOR), 4, rgba(DEFAULT_COLOR));

        if (shape.isNone() || size == null || color == null) {
            return Maybe.none();
        }

        return Maybe.some(new Greybox(
            shape.unwrap(),
            new Vec3(size[0], size[1], size[2]),
            new Color(unit(color[0]), unit(color[1]), unit(color[2]), unit(color[3]))
        ));
    }

    public void writeTo(SceneNode node) {
        ObjectNode json = KebabExtras.writable(node).putObject(KEY);
        json.put(SHAPE, shape.jsonName());

        ArrayNode sizeJson = json.putArray(SIZE);
        sizeJson.add(size.getX()).add(size.getY()).add(size.getZ());

        ArrayNode colorJson = json.putArray(COLOR);
        for (float channel : rgba(color)) {
            colorJson.add(channel);
        }
    }

    // A missing value falls back; a present but malformed one is rejected (null).
    private static float[] numbers(JsonNode array, int count, float[] fallback) {
        if (array.isMissingNode()) {
            return fallback;
        }
        if (!array.isArray() || array.size() != count) {
            return null;
        }

        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            if (!array.get(i).isNumber()) {
                return null;
            }
            values[i] = array.get(i).floatValue();
        }
        return values;
    }

    private static float[] rgba(Color color) {
        return color.getRGBComponents(null);
    }

    private static float unit(float value) {
        return Math.clamp(value, 0f, 1f);
    }
}
