package org.halalfoursome.kebabeditor.playtest;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** The one-line account of a finished playtest that is logged when it stops. */
public final class PlaytestSummary {

    private PlaytestSummary() {}

    public static String of(PlaytestSession finished) {
        return describe(finished.elapsed(), finished.jumps(), finished.respawns());
    }

    public static String describe(float seconds, List<JumpMetrics> jumps, int respawns) {
        StringBuilder text = new StringBuilder(String.format(Locale.ROOT,
            "Playtest stopped after %.1f s: %d jump(s)", seconds, jumps.size()));

        jumps.stream().max(Comparator.comparingDouble(JumpMetrics::distance)).ifPresent(longest ->
            text.append(String.format(Locale.ROOT, ", longest %.2f m", longest.distance())));
        jumps.stream().max(Comparator.comparingDouble(JumpMetrics::rise)).ifPresent(highest ->
            text.append(String.format(Locale.ROOT, ", highest %.2f m", highest.rise())));
        if (respawns > 0) {
            text.append(", ").append(respawns).append(" fall(s) out of the level");
        }

        return text.toString();
    }
}
