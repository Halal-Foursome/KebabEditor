package org.halalfoursome.kebabeditor.ui;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.render.Camera;
import org.halalfoursome.kebabeditor.ui.navigation.NavigationMode;
import org.halalfoursome.kebabeditor.ui.playtest.PlaytestViewport;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.Getter;

/**
 * The playtest side of an {@link EditorView}: the running session, the paths of
 * finished ones, and the editor camera to give back when it stops.
 */
public final class ViewportPlaytest implements PlaytestViewport {

    /** The editor camera before a playtest took it over. */
    private record CameraPose(Vec3 position, float yaw, float pitch) {}

    private final EditorView view;
    private final PathRays pathRays = new PathRays();

    @Getter
    private Maybe<PlaytestSession> session = Maybe.none();
    private NavigationMode editingNavigation;
    private CameraPose editingCamera;

    ViewportPlaytest(EditorView view) {
        this.view = view;
    }

    public boolean isRunning() {
        return session.isSome();
    }

    @Override
    public Maybe<BehaviorContext> behaviorContext() {
        return view.behaviorContext();
    }

    /** Recorded paths of finished playtests, drawn in the viewport. */
    @Override
    public PathRays pathRays() {
        return pathRays;
    }

    /**
     * Hand the viewport to a playtest: editing input stops, the gizmo, selection
     * and icons are hidden, and {@code navigation} drives the camera.
     */
    @Override
    public void startPlaytest(PlaytestSession session, NavigationMode navigation) {
        if (isRunning()) {
            throw new IllegalStateException("A playtest is already running");
        }

        view.cancelEditingInput();

        Camera camera = view.getCamera();
        editingCamera = new CameraPose(camera.getPosition().copy(), camera.getYaw(), camera.getPitch());
        editingNavigation = view.getNavigationMode();
        this.session = Maybe.some(session);
        view.setNavigationMode(navigation);
    }

    @Override
    public void stopPlaytest() {
        if (!isRunning()) {
            return;
        }

        session = Maybe.none();
        view.setNavigationMode(editingNavigation);
        editingNavigation = null;

        Camera camera = view.getCamera();
        camera.getPosition().set(editingCamera.position());
        camera.setOrientation(editingCamera.yaw(), editingCamera.pitch());
        editingCamera = null;
    }
}
