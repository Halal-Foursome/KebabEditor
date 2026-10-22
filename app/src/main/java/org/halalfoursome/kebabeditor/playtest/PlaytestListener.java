package org.halalfoursome.kebabeditor.playtest;

/**
 * Hears what happens in a {@link PlaytestSession}, so the simulation does not
 * decide where it is reported. Every method does nothing by default.
 */
public interface PlaytestListener {

    PlaytestListener NONE = new PlaytestListener() {};

    /** A jump landed; {@code number} counts jumps from 1. */
    default void jumped(int number, JumpMetrics jump) {}

    /** The actor walked off an edge and landed noticeably lower. */
    default void dropped(float height, float airTime) {}

    /** The actor entered a trigger, {@code time} simulated seconds into the run. */
    default void triggerEntered(String trigger, float time) {}

    /** The actor fell out of the level and was put back at {@code start}. */
    default void respawned(String start) {}
}
