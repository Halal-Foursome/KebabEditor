package org.halalfoursome.kebabeditor.playtest.reach;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.utils.Maybe;

/** The reachability check the viewport shows, until the level changes or it is cleared. */
public final class ReachabilityMarks {

    private Maybe<ReachabilityReport> report = Maybe.none();
    private final List<Runnable> listeners = new ArrayList<>();

    public Maybe<ReachabilityReport> report() {
        return report;
    }

    public boolean isEmpty() {
        return report.isNone();
    }

    public void show(ReachabilityReport report) {
        this.report = Maybe.some(report);
        notifyListeners();
    }

    public void clear() {
        if (report.isNone()) {
            return;
        }
        report = Maybe.none();
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
