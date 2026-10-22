package org.halalfoursome.kebabeditor.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.PrimitiveType;
import org.halalfoursome.kebabeditor.playtest.JumpMetrics;
import org.halalfoursome.kebabeditor.playtest.PathRecording;
import org.junit.jupiter.api.Test;

class PathRayMeshesTest {

    // Two ground samples, then two in the air: one ground segment, two air segments.
    private static final PathRecording RECORDING = new PathRecording(
        List.of(new PathRecording.Run(
            new float[] { 0, 0, 0,   1, 0, 0,   2, 1, 0,   3, 0, 0 },
            new boolean[] { true, true, false, false }
        )),
        List.of(new JumpMetrics(new Vec3(1, 0, 0), new Vec3(3, 0, 0), 1, 0.4f)),
        List.of(new Vec3(2, 1, 0)),
        1f
    );

    private static Set<Color> colors(List<Drawable> drawables) {
        return drawables.stream().map(d -> d.material().baseColor()).collect(Collectors.toSet());
    }

    @Test
    void groundAndAirAreSeparateLayers() {
        List<Drawable> drawables = PathRayMeshes.of(RECORDING, false).drawables(true);

        assertEquals(Set.of(PathRayMeshes.Layer.GROUND.color, PathRayMeshes.Layer.AIR.color), colors(drawables));
        Drawable ground = drawables.stream()
            .filter(d -> d.material().baseColor().equals(PathRayMeshes.Layer.GROUND.color))
            .findFirst().orElseThrow();
        Drawable air = drawables.stream()
            .filter(d -> d.material().baseColor().equals(PathRayMeshes.Layer.AIR.color))
            .findFirst().orElseThrow();
        assertEquals(1, ground.mesh().primitiveCount());
        assertEquals(2, air.mesh().primitiveCount());
    }

    @Test
    void finishedRunAlsoShowsJumpsAndTriggerHits() {
        List<Drawable> drawables = PathRayMeshes.of(RECORDING, true).drawables(true);

        assertEquals(PathRayMeshes.Layer.values().length, drawables.size());
        for (Drawable drawable : drawables) {
            assertEquals(PrimitiveType.LINES, drawable.mesh().type());
            assertEquals(DrawMode.SOLID, drawable.mode());
        }
    }

    @Test
    void pathIsLiftedOffTheFloor() {
        Drawable ground = PathRayMeshes.of(RECORDING, false).drawables(true).stream()
            .filter(d -> d.material().baseColor().equals(PathRayMeshes.Layer.GROUND.color))
            .findFirst().orElseThrow();

        assertTrue(ground.mesh().bounds().getMinY() > 0f);
    }

    @Test
    void olderRunsAreDimmed() {
        PathRayMeshes meshes = PathRayMeshes.of(RECORDING, true);

        Set<Color> newest = colors(meshes.drawables(true));
        Set<Color> older = colors(meshes.drawables(false));

        assertEquals(newest.size(), older.size());
        for (Color color : older) {
            assertTrue(!newest.contains(color));
        }
        assertNotEquals(newest, older);
    }

    @Test
    void emptyRecordingDrawsNothing() {
        PathRecording empty = new PathRecording(List.of(), List.of(), List.of(), 0);

        assertTrue(PathRayMeshes.of(empty, true).drawables(true).isEmpty());
    }
}
