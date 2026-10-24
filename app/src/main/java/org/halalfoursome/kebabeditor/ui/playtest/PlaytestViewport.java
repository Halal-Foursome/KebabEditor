package org.halalfoursome.kebabeditor.ui.playtest;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.playtest.reach.ReachabilityMarks;
import org.halalfoursome.kebabeditor.ui.navigation.NavigationMode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/** What a playtest needs from the viewport it runs in. */
public interface PlaytestViewport {

    /** Resolves models and prefabs of the open project; none without one. */
    Maybe<BehaviorContext> behaviorContext();

    /** Paths of finished playtests, drawn in the viewport. */
    PathRays pathRays();

    /** The last reachability check, highlighted in the viewport. */
    ReachabilityMarks reachabilityMarks();

    /** Hand the viewport to the playtest; {@code navigation} drives its camera until {@link #stopPlaytest}. */
    void startPlaytest(PlaytestSession session, NavigationMode navigation);

    /** Give the viewport back to editing, with the camera where it was. */
    void stopPlaytest();
}
