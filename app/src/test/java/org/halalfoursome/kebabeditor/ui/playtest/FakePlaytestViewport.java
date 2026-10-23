package org.halalfoursome.kebabeditor.ui.playtest;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.playtest.PhysicsTestSupport;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.ui.navigation.NavigationMode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/** A viewport without OpenGL: remembers what the playtest asked of it. */
public final class FakePlaytestViewport implements PlaytestViewport {

    public Maybe<BehaviorContext> context = Maybe.some(PhysicsTestSupport.NO_ASSETS);
    public final PathRays pathRays = new PathRays();

    public Maybe<PlaytestSession> playing = Maybe.none();
    public Maybe<NavigationMode> navigation = Maybe.none();
    public int stops;

    @Override
    public Maybe<BehaviorContext> behaviorContext() {
        return context;
    }

    @Override
    public PathRays pathRays() {
        return pathRays;
    }

    @Override
    public void startPlaytest(PlaytestSession session, NavigationMode navigation) {
        this.playing = Maybe.some(session);
        this.navigation = Maybe.some(navigation);
    }

    @Override
    public void stopPlaytest() {
        playing = Maybe.none();
        navigation = Maybe.none();
        stops++;
    }
}
