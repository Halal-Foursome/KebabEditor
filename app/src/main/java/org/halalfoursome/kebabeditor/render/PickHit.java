package org.halalfoursome.kebabeditor.render;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

public record PickHit(
    Maybe<SceneNode> node, 
    float distance, 
    Vec3 point, 
    Vec3 normal
) {}
