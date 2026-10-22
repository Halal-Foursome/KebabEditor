package org.halalfoursome.kebabeditor.ui.playtest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.playtest.LoggingPlaytestListener;
import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.playtest.PathRecording;
import org.halalfoursome.kebabeditor.playtest.PlayerStart;
import org.halalfoursome.kebabeditor.playtest.PlaytestLevel;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.playtest.PlaytestSummary;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Starts and stops playtests in the viewport.
 *
 * A playtest runs on a snapshot of the scene as it is, saved or not, and never
 * changes it, so stopping returns to exactly the level that was being edited.
 * Scene edits are refused while it runs. Each finished run is kept as path rays.
 */
public class PlaytestController {

    private final Editor editor;
    private final PlaytestViewport viewport;
    private final UserNotice notice;
    private final LogService log;
    private final List<Runnable> listeners = new ArrayList<>();

    private Maybe<PlaytestSession> session = Maybe.none();
    private Maybe<Scene> playedScene = Maybe.none();
    private Maybe<Scene> raysScene = Maybe.none();

    /**
     * @param notice tells the user why a playtest cannot start
     * @param log    where starts, stops and the run's events are written
     */
    public PlaytestController(Editor editor, PlaytestViewport viewport, UserNotice notice, LogService log) {
        this.editor = editor;
        this.viewport = viewport;
        this.notice = notice;
        this.log = log;

        // Opening or closing a project replaces the scene: stop playing it and drop its paths.
        editor.addProjectListener(() -> {
            if (!sameScene(playedScene)) {
                stop();
            }
            if (!sameScene(raysScene)) {
                viewport.pathRays().clear();
                raysScene = editor.scene();
            }
        });
    }

    public boolean isRunning() {
        return session.isSome();
    }

    public PathRays pathRays() {
        return viewport.pathRays();
    }

    /** Called when a playtest starts or stops, e.g. to switch the Run button's icon. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void toggle() {
        if (isRunning()) {
            stop();
        } else {
            start();
        }
    }

    public void start() {
        if (isRunning()) {
            return;
        }

        Maybe<BehaviorContext> context = viewport.behaviorContext();
        if (editor.scene().isNone() || context.isNone()) {
            notice.show("No project open",
                "Open or create a project first (File > Open project / Create new project).");
            return;
        }

        Scene scene = editor.scene().unwrap();
        PlaytestLevel level = PlaytestLevel.build(scene, context.unwrap());

        if (level.hasNothingToStandOn()) {
            log.warn("Playtest not started: the level has nothing to stand on");
            notice.show("Nothing to stand on",
                "The level has no solid objects. Add greybox shapes or colliders before running it.");
            return;
        }

        PlayerStart start = chooseStart(level);
        if (start.settings().isNone()) {
            log.warn("Playtest not started: player start '" + start.name() + "' is invalid");
            notice.show("Invalid player start",
                "Player start '" + start.name() + "' has invalid parameters:\n- "
                    + String.join("\n- ", start.problems()));
            return;
        }

        PlaytestSession running = new PlaytestSession(level.world(), start, new LoggingPlaytestListener(log));
        session = Maybe.some(running);
        playedScene = Maybe.some(scene);
        raysScene = Maybe.some(scene);
        editor.setPlaytesting(true);
        viewport.startPlaytest(running, new PlaytestNavigation(running, this::stop, this::failed));

        log.info(String.format(Locale.ROOT,
            "Playtest started at '%s' with %d solid(s) and %d trigger(s). "
                + "WASD / arrows run, Space jumps, drag to look around, Esc stops.",
            start.name(), level.world().solids().size(), level.world().triggers().size()));
        notifyListeners();
    }

    public void stop() {
        if (session.isNone()) {
            return;
        }

        PlaytestSession finished = session.unwrap();
        session = Maybe.none();
        playedScene = Maybe.none();
        viewport.stopPlaytest();
        editor.setPlaytesting(false);

        PathRecording recording = finished.finish();
        log.info(PlaytestSummary.of(finished));

        if (recording.isEmpty()) {
            log.warn("Playtest stopped before the actor moved: there is no path to draw");
        } else {
            viewport.pathRays().add(recording);
        }

        notifyListeners();
    }

    public void clearPathRays() {
        viewport.pathRays().clear();
        log.info("Path rays cleared");
    }

    void failed(RuntimeException e) {
        log.error("Playtest stopped by an error in the simulation", e);
        stop();
    }

    // A selected player start wins, so a designer can test from any of several.
    private PlayerStart chooseStart(PlaytestLevel level) {
        Maybe<PlayerStart> chosen = level.startFor(editor.selection().primary());

        if (chosen.isNone()) {
            log.warn("The level has no Player Start: starting where the camera looks. "
                + "Add one with Edit > Add player start.");
            return PlayerStart.fallback(viewport.spawnPoint(), viewport.cameraYaw());
        }

        PlayerStart start = chosen.unwrap();
        boolean selected = editor.selection().primary()
            .isSomeAnd(node -> start.node().isSomeAnd(startNode -> startNode == node));
        if (level.starts().size() > 1 && !selected) {
            log.info(level.starts().size() + " player starts in the level, using '" + chosen.unwrap().name()
                + "'. Select another one to start there.");
        }
        return chosen.unwrap();
    }

    private boolean sameScene(Maybe<Scene> scene) {
        Maybe<Scene> current = editor.scene();
        return scene.isNone() && current.isNone()
            || scene.isSome() && current.isSomeAnd(s -> s == scene.unwrap());
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
