package org.halalfoursome.kebabeditor.playtest;

import java.util.List;

import org.halalfoursome.kebabeditor.math.Vec3;

/**
 * Where the actor went during one playtest: the path rays drawn after it.
 *
 * @param runs        unbroken stretches of the path; a respawn starts a new one
 * @param jumps       every jump that ended in a landing
 * @param triggerHits where the actor entered a trigger
 * @param duration    simulated seconds
 */
public record PathRecording(List<Run> runs, List<JumpMetrics> jumps, List<Vec3> triggerHits, float duration) {

    /**
     * Samples of the feet position, three floats each, and whether the actor
     * stood on the ground at each sample.
     */
    public record Run(float[] positions, boolean[] grounded) {

        public Run {
            if (positions.length != grounded.length * 3) {
                throw new IllegalArgumentException("need three coordinates per sample");
            }
            positions = positions.clone();
            grounded = grounded.clone();
        }

        // Copies, so a finished recording cannot be changed through them.
        @Override
        public float[] positions() {
            return positions.clone();
        }

        @Override
        public boolean[] grounded() {
            return grounded.clone();
        }

        public int size() {
            return grounded.length;
        }

        public Vec3 position(int sample) {
            return new Vec3(positions[sample * 3], positions[sample * 3 + 1], positions[sample * 3 + 2]);
        }

        public boolean isGrounded(int sample) {
            return grounded[sample];
        }
    }

    public PathRecording {
        runs = List.copyOf(runs);
        jumps = List.copyOf(jumps);
        triggerHits = triggerHits.stream().map(Vec3::copy).toList();
    }

    /** Below this the actor is taken to have stood still. */
    private static final float MOVED = 0.01f;

    /** Nothing to draw: the actor never moved, e.g. the playtest was stopped right away. */
    public boolean isEmpty() {
        for (Run run : runs) {
            for (int i = 1; i < run.size(); i++) {
                if (run.position(i).distance(run.position(0)) > MOVED) {
                    return false;
                }
            }
        }
        return true;
    }

    public int sampleCount() {
        return runs.stream().mapToInt(Run::size).sum();
    }
}
