package org.halalfoursome.kebabeditor.render;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.TextureSlot;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

public final class IconLayout {

    // Sizes are fractions of the viewport height, so drawing (surface pixels) and picking (component pixels) agree
    static final float ICON_SCREEN_FRACTION = 0.1f;
    static final float GAP_FRACTION = 0.1f;

    public record Offset(float x, float y) {}

    public record Placement(
        SceneNode node,
        TextureSlot texture,
        Vec3 center,
        float worldSize,
        float depth,
        float screenX,
        float screenY,
        float screenSize
    ) {}

    private IconLayout() {}

    public static List<Placement> of(Scene scene, Camera camera, int width, int height, ArchetypeIcons icons) {
        List<Placement> placements = new ArrayList<>();

        if (width < 1 || height < 1) {
            return placements;
        }

        Vec3 eye = camera.getPosition();
        Vec3 forward = camera.forward();
        Vec3 right = camera.right();
        Vec3 up = camera.up();
        float tanHalf = (float) Math.tan(camera.getFovY() / 2);
        float aspect = width / (float) height;
        float iconSize = ICON_SCREEN_FRACTION * height;
        float step = iconSize * (1f + GAP_FRACTION);

        scene.traverse().forEach(node -> {
            List<TextureSlot> textures = texturesOf(node, icons);
            Vec3 anchor = node.worldMatrix().getTranslation();
            Vec3 relative = anchor.sub(eye);
            float depth = relative.dot(forward);

            if (textures.isEmpty() || depth <= camera.getNear()) {
                return;
            }

            float anchorX = (relative.dot(right) / (depth * tanHalf * aspect) + 1f) / 2f * width;
            float anchorY = (1f - relative.dot(up) / (depth * tanHalf)) / 2f * height;
            float worldPerUnit = 2f * depth * tanHalf / height;
            List<Offset> offsets = pyramid(textures.size(), step);

            for (int i = 0; i < textures.size(); i++) {
                Offset offset = offsets.get(i);
                Vec3 center = anchor
                    .add(right.mul(offset.x() * worldPerUnit))
                    .add(up.mul(offset.y() * worldPerUnit));

                placements.add(new Placement(
                    node,
                    textures.get(i),
                    center,
                    iconSize * worldPerUnit,
                    depth,
                    anchorX + offset.x(),
                    anchorY - offset.y(),
                    iconSize
                ));
            }
        });

        return placements;
    }

    public static Maybe<Placement> hit(List<Placement> placements, float x, float y) {
        Placement nearest = null;

        for (Placement placement : placements) {
            float half = placement.screenSize() / 2f;
            boolean inside = Math.abs(x - placement.screenX()) <= half && Math.abs(y - placement.screenY()) <= half;

            if (inside && (nearest == null || placement.depth() < nearest.depth())) {
                nearest = placement;
            }
        }

        return Maybe.ofNullable(nearest);
    }

    public static List<Integer> pyramidRows(int count) {
        int rows = 0;
        while (rows * (rows + 1) / 2 < count) {
            rows++;
        }

        List<Integer> bottomUp = new ArrayList<>();
        int remaining = count;

        for (int capacity = rows; capacity >= 1 && remaining > 0; capacity--) {
            int taken = Math.min(capacity, remaining);
            bottomUp.add(taken);
            remaining -= taken;
        }

        return bottomUp.reversed();
    }

    // Reading order: left to right, top row first. x grows to the right and y upwards, around the centre.
    public static List<Offset> pyramid(int count, float step) {
        List<Integer> rows = pyramidRows(count);
        List<Offset> offsets = new ArrayList<>();

        for (int row = 0; row < rows.size(); row++) {
            int inRow = rows.get(row);
            float y = ((rows.size() - 1) / 2f - row) * step;

            for (int column = 0; column < inRow; column++) {
                offsets.add(new Offset((column - (inRow - 1) / 2f) * step, y));
            }
        }

        return offsets;
    }

    private static List<TextureSlot> texturesOf(SceneNode node, ArchetypeIcons icons) {
        List<TextureSlot> textures = new ArrayList<>();

        for (NodeArchetype archetype : node.getArchetypes()) {
            if (archetype instanceof ArchetypeInstance instance) {
                icons.textureFor(instance.getDefinition()).ifSome(textures::add);
            }
        }

        if (node.getArchetypes().isEmpty() && Greybox.of(node).isNone()) {
            icons.emptyObject().ifSome(textures::add);
        }

        return textures;
    }
}
