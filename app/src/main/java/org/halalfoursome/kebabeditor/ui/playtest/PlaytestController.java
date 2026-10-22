package org.halalfoursome.kebabeditor.ui.playtest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.playtest.JumpMetrics;
import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.playtest.PathRecording;
import org.halalfoursome.kebabeditor.playtest.PlayerStart;
import org.halalfoursome.kebabeditor.playtest.PlaytestLevel;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Starts and stops playtests in the viewport.
 *
 * A playtest runs on a snapshot of the scene as it is, saved or not, and never
 * changes it, so stopping returns to exactly the level that was being edited.
 * Scene edits are refused while it runs. Each finished run is kept as path rays.
 */
public class PlaytestController {

    private final JFrame frame;
    private final Editor editor;
    private final EditorView view;
    private final List<Runnable> listeners = new ArrayList<>();

    private Maybe<PlaytestSession> session = Maybe.none();
    private Maybe<Scene> playedScene = Maybe.none();
    private Maybe<Scene> raysScene = Maybe.none();

    public PlaytestController(JFrame frame, Editor editor, EditorView view) {
        this.frame = frame;
        this.editor = editor;
        this.view = view;

        // Opening or closing a project replaces the scene: stop playing it and drop its paths.
        editor.addProjectListener(() -> {
            if (!sameScene(playedScene)) {
                stop();
            }
            if (!sameScene(raysScene)) {
                view.pathRays().clear();
                raysScene = editor.scene();
            }
        });
    }

    public boolean isRunning() {
        return session.isSome();
    }

    public PathRays pathRays() {
        return view.pathRays();
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
        if (editor.scene().isNone()) {
            refuse("No project open",
                "Open or create a project first (File > Open project / Create new project).");
            return;
        }

        Scene scene = editor.scene().unwrap();
        PlaytestLevel level = PlaytestLevel.build(scene, view.behaviorContext());

        if (level.world().solids().isEmpty()) {
            LogService.get().warn("Playtest not started: the level has nothing to stand on");
            refuse("Nothing to stand on",
                "The level has no solid objects. Add greybox shapes or colliders before running it.");
            return;
        }

        PlayerStart start = chooseStart(level);
        if (start.settings().isNone()) {
            LogService.get().warn("Playtest not started: player start '" + start.name() + "' is invalid");
            refuse("Invalid player start",
                "Player start '" + start.name() + "' has invalid parameters:\n- "
                    + String.join("\n- ", start.problems()));
            return;
        }

        PlaytestSession running = new PlaytestSession(level.world(), start);
        session = Maybe.some(running);
        playedScene = Maybe.some(scene);
        raysScene = Maybe.some(scene);
        editor.setPlaytesting(true);
        view.startPlaytest(running, new PlaytestNavigation(running, this::stop, this::failed));

        LogService.get().info(String.format(Locale.ROOT,
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
        view.stopPlaytest();
        editor.setPlaytesting(false);

        PathRecording recording = finished.finish();
        LogService.get().info(summary(finished));

        if (recording.isEmpty()) {
            LogService.get().warn("Playtest stopped before the actor moved: there is no path to draw");
        } else {
            view.pathRays().add(recording);
        }

        notifyListeners();
    }

    public void clearPathRays() {
        view.pathRays().clear();
        LogService.get().info("Path rays cleared");
    }

    private void failed(RuntimeException e) {
        LogService.get().error("Playtest stopped by an error in the simulation", e);
        stop();
    }

    // A selected player start wins, so a designer can test from any of several.
    private PlayerStart chooseStart(PlaytestLevel level) {
        List<PlayerStart> starts = level.starts();

        if (starts.isEmpty()) {
            LogService.get().warn("The level has no Player Start: starting where the camera looks. "
                + "Add one with Edit > Add player start.");
            return PlayerStart.fallback(view.spawnPoint(), view.getCamera().getYaw());
        }

        Maybe<SceneNode> selected = editor.selection().primary();
        for (PlayerStart start : starts) {
            if (start.node().isSome() && selected.isSomeAnd(node -> node == start.node().unwrap())) {
                return start;
            }
        }

        if (starts.size() > 1) {
            LogService.get().info(starts.size() + " player starts in the level, using '" + starts.getFirst().name()
                + "'. Select another one to start there.");
        }
        return starts.getFirst();
    }

    private static String summary(PlaytestSession finished) {
        List<JumpMetrics> jumps = finished.jumps();
        StringBuilder text = new StringBuilder(String.format(Locale.ROOT,
            "Playtest stopped after %.1f s: %d jump(s)", finished.elapsed(), jumps.size()));

        jumps.stream().max(Comparator.comparingDouble(JumpMetrics::distance)).ifPresent(longest ->
            text.append(String.format(Locale.ROOT, ", longest %.2f m", longest.distance())));
        jumps.stream().max(Comparator.comparingDouble(JumpMetrics::rise)).ifPresent(highest ->
            text.append(String.format(Locale.ROOT, ", highest %.2f m", highest.rise())));
        if (finished.respawns() > 0) {
            text.append(", ").append(finished.respawns()).append(" fall(s) out of the level");
        }

        return text.toString();
    }

    private boolean sameScene(Maybe<Scene> scene) {
        Maybe<Scene> current = editor.scene();
        return scene.isNone() && current.isNone()
            || scene.isSome() && current.isSomeAnd(s -> s == scene.unwrap());
    }

    private void refuse(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
