package org.halalfoursome.kebabeditor.behavior.instance;

import java.awt.Color;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.model.AlphaMode;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.model.TextureSlot;

public final class Placeholders {

    private Placeholders() {}

    public static Drawable brokenCube(Mat4 world, String detail) {
        return new Drawable(
            Meshes.CUBE_TRIANGLES,
            world,
            new Material(Color.WHITE, new TextureSlot.Missing(detail), AlphaMode.OPAQUE, true),
            DrawMode.SOLID
        );
    }

    public static Aabb brokenBounds(Mat4 world) {
        return Meshes.CUBE_TRIANGLES.bounds().transformed(world);
    }
}
