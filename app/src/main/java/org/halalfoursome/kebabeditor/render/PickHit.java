package org.halalfoursome.kebabeditor.render;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Where a ray first meets the scene.
 *
 * @param node     the node that was hit, or none for the ground plane
 * @param distance along the ray, in world units
 * @param point    world-space hit point
 * @param normal   unit world-space surface normal, facing the ray
 */
public record PickHit(Maybe<SceneNode> node, float distance, Vec3 point, Vec3 normal) {}
