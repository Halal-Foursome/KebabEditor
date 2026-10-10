package org.halalfoursome.kebabeditor.model;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Mat4;

public record ModelPart(
    MeshData mesh, 
    Material material, 
    Mat4 transform
) {

    public ModelPart {
        Objects.requireNonNull(mesh, "mesh");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(transform, "transform");
        transform = transform.copy();
    }
}
