package org.halalfoursome.kebabeditor.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.Test;

/** Recording paths while playing, and the path rays kept afterwards. */
class PathRecordingTest {

    private static final PathRecording EMPTY = new PathRecording(List.of(), List.of(), List.of(), 0);

    @Test
    void samplesGrowPastTheInitialCapacity() {
        PathRecorder recorder = new PathRecorder();
        for (int i = 0; i < 1000; i++) {
            recorder.sample(new Vec3(i, 0, 0), i % 2 == 0);
        }

        PathRecording.Run run = recorder.snapshot(1f).runs().getFirst();

        assertEquals(1000, run.size());
        assertTrue(new Vec3(999, 0, 0).approxEquals(run.position(999), 1e-6f));
        assertFalse(run.isGrounded(999));
    }

    @Test
    void breakingThePathStartsANewRun() {
        PathRecorder recorder = new PathRecorder();
        recorder.sample(Vec3.zero(), true);
        recorder.sample(Vec3.one(), true);
        recorder.breakPath();
        recorder.breakPath(); // nothing new since the last break: no empty run
        recorder.sample(new Vec3(5, 0, 0), false);

        PathRecording recording = recorder.snapshot(2f);

        assertEquals(2, recording.runs().size());
        assertEquals(2, recording.runs().get(0).size());
        assertEquals(1, recording.runs().get(1).size());
        assertEquals(3, recording.sampleCount());
        assertEquals(2f, recording.duration());
    }

    @Test
    void versionChangesWithEveryRecordedThing() {
        PathRecorder recorder = new PathRecorder();
        List<Integer> versions = new ArrayList<>();
        versions.add(recorder.version());

        recorder.sample(Vec3.zero(), true);
        versions.add(recorder.version());
        recorder.addJump(new JumpMetrics(Vec3.zero(), Vec3.one(), 1, 1));
        versions.add(recorder.version());
        recorder.addTriggerHit(Vec3.one());
        versions.add(recorder.version());
        recorder.breakPath();
        versions.add(recorder.version());

        assertEquals(versions.size(), versions.stream().distinct().count());
        assertEquals(1, recorder.jumps().size());
    }

    @Test
    void recordingThatNeverMovedIsEmpty() {
        PathRecorder recorder = new PathRecorder();
        recorder.sample(Vec3.zero(), false);
        recorder.sample(new Vec3(0.005f, 0, 0), true);

        assertTrue(recorder.snapshot(1f).isEmpty());

        recorder.sample(new Vec3(1, 0, 0), true);
        assertFalse(recorder.snapshot(1f).isEmpty());
        assertTrue(EMPTY.isEmpty());
    }

    @Test
    void finishedRecordingCannotBeChangedFromOutside() {
        float[] positions = { 0, 0, 0, 1, 0, 0 };
        boolean[] grounded = { true, true };
        PathRecording.Run run = new PathRecording.Run(positions, grounded);

        positions[3] = 99;
        run.positions()[3] = 99;
        run.grounded()[0] = false;

        assertTrue(new Vec3(1, 0, 0).approxEquals(run.position(1), 1e-6f));
        assertTrue(run.isGrounded(0));
    }

    @Test
    void runNeedsThreeCoordinatesPerSample() {
        assertThrows(IllegalArgumentException.class, () -> new PathRecording.Run(new float[4], new boolean[2]));
    }

    @Test
    void pathRaysKeepOnlyTheLatestRecordings() {
        PathRays rays = new PathRays();
        List<PathRecording> added = new ArrayList<>();

        for (int i = 0; i < PathRays.MAX_RECORDINGS + 3; i++) {
            PathRecording recording = new PathRecording(List.of(), List.of(), List.of(), i);
            added.add(recording);
            rays.add(recording);
        }

        assertEquals(added.subList(3, added.size()), rays.recordings());
    }

    @Test
    void newRecordingShowsHiddenRays() {
        PathRays rays = new PathRays();
        rays.setVisible(false);

        rays.add(EMPTY);

        assertTrue(rays.isVisible());
    }

    @Test
    void listenersHearOnlyRealChanges() {
        PathRays rays = new PathRays();
        int[] calls = { 0 };
        rays.addListener(() -> calls[0]++);

        rays.clear();           // already empty
        rays.setVisible(true);  // already visible
        assertEquals(0, calls[0]);

        rays.add(EMPTY);
        rays.setVisible(false);
        rays.clear();
        assertEquals(3, calls[0]);
        assertTrue(rays.isEmpty());
        assertNotEquals(0, calls[0]);
    }
}
