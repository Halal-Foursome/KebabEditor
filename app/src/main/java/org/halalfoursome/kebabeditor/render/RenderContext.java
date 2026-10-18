package org.halalfoursome.kebabeditor.render;

import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/** Everything a pass may read while drawing one frame. Built fresh every frame. */
public record RenderContext(
    Camera camera,
    Mat4 view,
    Mat4 projection,
    Mat4 viewProjection,
    int width,
    int height,
    Maybe<Scene> scene,
    Maybe<Path> sceneFile,
    Maybe<BehaviorContext> behaviors,
    List<SceneNode> selection,
    Maybe<PlacementPreview> placement
) {

    public static RenderContext of(
        Camera camera,
        int width,
        int height,
        Maybe<Scene> scene,
        Maybe<Path> sceneFile,
        Maybe<BehaviorContext> behaviors,
        List<SceneNode> selection,
        Maybe<PlacementPreview> placement
    ) {
        Mat4 view = camera.view();
        Mat4 projection = camera.projection(width / (float) Math.max(height, 1));

        return new RenderContext(
            camera,
            view,
            projection,
            projection.mul(view),
            width,
            height,
            scene,
            sceneFile,
            behaviors,
            List.copyOf(selection),
            placement
        );
    }

    public boolean isSelected(SceneNode node) {
        return selection.contains(node);
    }

    public Vec3 cameraPosition() {
        return camera.getPosition().copy();
    }

    // Folder that relative asset paths in the scene are resolved against.
    public Maybe<Path> sceneDirectory() {
        return sceneFile.map(file -> file.toAbsolutePath().getParent());
    }
}
