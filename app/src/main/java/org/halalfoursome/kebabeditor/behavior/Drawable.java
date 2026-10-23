package org.halalfoursome.kebabeditor.behavior;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;

public record Drawable(
    MeshData mesh,
    Mat4 world,
    Material material,
    DrawMode mode,
    float lineWidth
) {

    public Drawable(MeshData mesh, Mat4 world, Material material, DrawMode mode) {
        this(mesh, world, material, mode, 1f);
    }

    public Drawable {
        Objects.requireNonNull(mesh, "mesh");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(mode, "mode");
        if (lineWidth < 1f) {
            throw new IllegalArgumentException("lineWidth must be at least 1 pixel, got " + lineWidth);
        }
        world = world.copy();
    }
}
