package org.halalfoursome.kebabeditor.behavior;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;

public record Drawable(
    MeshData mesh,
    Mat4 world,
    Material material,
    DrawMode mode
) {

    public Drawable {
        Objects.requireNonNull(mesh, "mesh");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(mode, "mode");
        world = world.copy();
    }
}
