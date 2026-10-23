package org.halalfoursome.kebabeditor.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.playtest.JumpMetrics;
import org.halalfoursome.kebabeditor.playtest.PathRecording;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Line meshes for one recorded path: the path coloured by ground or air, jump
 * spans, and crosses at take-offs, landings and trigger hits.
 */
final class PathRayMeshes {

    /** Lifted off the floor so the path does not sink into the surface it runs on. */
    private static final float LIFT = 0.05f;
    private static final float MARKER_SIZE = 0.2f;
    /** Older runs are drawn this much darker than the newest. */
    private static final float OLDER_BRIGHTNESS = 0.5f;

    enum Layer {
        GROUND(new Color(70, 190, 255)),
        // Violet, so it does not read as a trigger volume (orange).
        AIR(new Color(190, 120, 255)),
        SPAN(new Color(235, 235, 235)),
        TAKEOFF(new Color(110, 230, 110)),
        LANDING(new Color(240, 90, 90)),
        TRIGGER(new Color(255, 220, 70));

        final Color color;

        Layer(Color color) {
            this.color = color;
        }
    }

    private final Map<Layer, MeshData> meshes;

    private PathRayMeshes(Map<Layer, MeshData> meshes) {
        this.meshes = meshes;
    }

    /**
     * @param markers include jump spans and markers; a live trail leaves them out
     */
    static PathRayMeshes of(PathRecording recording, boolean markers) {
        Map<Layer, Lines> lines = new EnumMap<>(Layer.class);
        for (Layer layer : Layer.values()) {
            lines.put(layer, new Lines());
        }

        for (PathRecording.Run run : recording.runs()) {
            for (int i = 0; i + 1 < run.size(); i++) {
                Layer layer = run.isGrounded(i) && run.isGrounded(i + 1) ? Layer.GROUND : Layer.AIR;
                lines.get(layer).segment(lifted(run.position(i)), lifted(run.position(i + 1)));
            }
        }

        if (markers) {
            for (JumpMetrics jump : recording.jumps()) {
                lines.get(Layer.SPAN).segment(lifted(jump.takeoff()), lifted(jump.landing()));
                lines.get(Layer.TAKEOFF).cross(lifted(jump.takeoff()));
                lines.get(Layer.LANDING).cross(lifted(jump.landing()));
            }
            for (Vec3 hit : recording.triggerHits()) {
                lines.get(Layer.TRIGGER).cross(lifted(hit));
            }
        }

        Map<Layer, MeshData> meshes = new EnumMap<>(Layer.class);
        lines.forEach((layer, builder) -> builder.build().ifSome(mesh -> meshes.put(layer, mesh)));
        return new PathRayMeshes(meshes);
    }

    /** @param newest the latest run is bright, older ones are dimmed */
    List<Drawable> drawables(boolean newest) {
        List<Drawable> drawables = new ArrayList<>();
        Mat4 identity = Mat4.identity();

        meshes.forEach((layer, mesh) -> drawables.add(new Drawable(
            mesh,
            identity,
            Material.solid(newest ? layer.color : dim(layer.color)),
            DrawMode.SOLID
        )));

        return drawables;
    }

    void release(DrawableRenderer renderer) {
        meshes.values().forEach(renderer::release);
    }

    private static Vec3 lifted(Vec3 feet) {
        return feet.add(new Vec3(0, LIFT, 0));
    }

    private static Color dim(Color color) {
        return new Color(
            (int) (color.getRed() * OLDER_BRIGHTNESS),
            (int) (color.getGreen() * OLDER_BRIGHTNESS),
            (int) (color.getBlue() * OLDER_BRIGHTNESS)
        );
    }

    private static final class Lines {

        private float[] positions = new float[3 * 64];
        private int vertices;

        void segment(Vec3 a, Vec3 b) {
            add(a);
            add(b);
        }

        void cross(Vec3 at) {
            float h = MARKER_SIZE;
            segment(at.add(new Vec3(-h, 0, 0)), at.add(new Vec3(h, 0, 0)));
            segment(at.add(new Vec3(0, -h, 0)), at.add(new Vec3(0, h, 0)));
            segment(at.add(new Vec3(0, 0, -h)), at.add(new Vec3(0, 0, h)));
        }

        // Every pair of vertices is its own segment, so indices just count up.
        Maybe<MeshData> build() {
            if (vertices == 0) {
                return Maybe.none();
            }

            int[] indices = new int[vertices];
            for (int i = 0; i < vertices; i++) {
                indices[i] = i;
            }
            return Maybe.some(
                MeshData.lines(Arrays.copyOf(positions, vertices * 3), indices)
            );
        }

        private void add(Vec3 p) {
            if ((vertices + 1) * 3 > positions.length) {
                positions = Arrays.copyOf(positions, positions.length * 2);
            }
            positions[vertices * 3] = p.getX();
            positions[vertices * 3 + 1] = p.getY();
            positions[vertices * 3 + 2] = p.getZ();
            vertices++;
        }
    }
}
