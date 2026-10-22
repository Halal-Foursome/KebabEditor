package org.halalfoursome.kebabeditor.playtest;

import java.util.Locale;

import org.halalfoursome.kebabeditor.logging.LogService;

/** Writes playtest events to the editor's log, where designers read the metrics. */
public final class LoggingPlaytestListener implements PlaytestListener {

    private final LogService log;

    public LoggingPlaytestListener(LogService log) {
        this.log = log;
    }

    @Override
    public void jumped(int number, JumpMetrics jump) {
        log.info("Playtest jump " + number + ": " + jump.describe());
    }

    @Override
    public void dropped(float height, float airTime) {
        log.info(String.format(Locale.ROOT, "Playtest drop: fell %.2f m, %.2f s in the air", height, airTime));
    }

    @Override
    public void triggerEntered(String trigger, float time) {
        log.info(String.format(Locale.ROOT, "Playtest trigger '%s' entered at %.2f s", trigger, time));
    }

    @Override
    public void respawned(String start) {
        log.warn("Playtest: the actor fell out of the level and respawned at '" + start + "'");
    }
}
