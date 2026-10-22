package org.halalfoursome.kebabeditor.playtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Vec3;

/** Collects a {@link PathRecording} while a playtest runs. */
public final class PathRecorder {

    private final List<PathRecording.Run> finished = new ArrayList<>();
    private final List<JumpMetrics> jumps = new ArrayList<>();
    private final List<Vec3> triggerHits = new ArrayList<>();

    private float[] positions = new float[3 * 256];
    private boolean[] grounded = new boolean[256];
    private int size;
    private int version;

    public void sample(Vec3 feet, boolean onGround) {
        if (size == grounded.length) {
            positions = Arrays.copyOf(positions, positions.length * 2);
            grounded = Arrays.copyOf(grounded, grounded.length * 2);
        }

        positions[size * 3] = feet.getX();
        positions[size * 3 + 1] = feet.getY();
        positions[size * 3 + 2] = feet.getZ();
        grounded[size] = onGround;
        size++;
        version++;
    }

    /** End the current stretch, e.g. after a respawn, so it is not joined to the next. */
    public void breakPath() {
        if (size > 0) {
            finished.add(currentRun());
            size = 0;
            version++;
        }
    }

    public void addJump(JumpMetrics jump) {
        jumps.add(jump);
        version++;
    }

    public void addTriggerHit(Vec3 at) {
        triggerHits.add(at.copy());
        version++;
    }

    public List<JumpMetrics> jumps() {
        return List.copyOf(jumps);
    }

    /** Changes whenever something is recorded, so a renderer knows when to rebuild. */
    public int version() {
        return version;
    }

    public PathRecording snapshot(float duration) {
        List<PathRecording.Run> runs = new ArrayList<>(finished);
        if (size > 0) {
            runs.add(currentRun());
        }
        return new PathRecording(runs, jumps, triggerHits, duration);
    }

    private PathRecording.Run currentRun() {
        return new PathRecording.Run(Arrays.copyOf(positions, size * 3), Arrays.copyOf(grounded, size));
    }
}
