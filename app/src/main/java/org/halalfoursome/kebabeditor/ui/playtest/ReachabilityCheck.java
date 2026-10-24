package org.halalfoursome.kebabeditor.ui.playtest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.Timer;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.playtest.ActorSettings;
import org.halalfoursome.kebabeditor.playtest.PlayerStart;
import org.halalfoursome.kebabeditor.playtest.PlaytestLevel;
import org.halalfoursome.kebabeditor.playtest.reach.Platform;
import org.halalfoursome.kebabeditor.playtest.reach.PlatformGraph;
import org.halalfoursome.kebabeditor.playtest.reach.ReachabilityMarks;
import org.halalfoursome.kebabeditor.playtest.reach.ReachabilityReport;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Shows which platforms the actor can get to, without running a playtest.
 *
 * While enabled it follows the editor: with two or more objects selected it
 * shows the way with the fewest jumps from the first selected to the last;
 * otherwise it checks the whole level from the player start. Scene edits,
 * selection changes and a new jump limit update it on their own.
 *
 * Updates run a moment after the last change, so dragging an object does not
 * recompute every frame. The log gets a line only when the outcome changes.
 */
public class ReachabilityCheck {

    /** How long the scene must stay unchanged before the check runs again. */
    static final int UPDATE_DELAY_MILLIS = 120;

    static final String ESTIMATE_NOTE =
        "Reachability: jumps are estimated from the actor's settings at full run speed; "
            + "walls and ceilings in the way are not checked.";

    private final Editor editor;
    private final PlaytestViewport viewport;
    private final LogService log;
    private final Runnable requestUpdate;
    private final List<Runnable> listeners = new ArrayList<>();

    private boolean enabled;
    private Maybe<Integer> jumpLimit = Maybe.none();
    private List<String> lastLogged = List.of();

    /** @param log where outcomes and problems are written */
    public ReachabilityCheck(Editor editor, PlaytestViewport viewport, LogService log) {
        this(editor, viewport, log, update -> {
            Timer timer = new Timer(UPDATE_DELAY_MILLIS, e -> update.run());
            timer.setRepeats(false);
            return timer::restart;
        });
    }

    /** @param delay turns the update into what asks for one; tests run it at once */
    ReachabilityCheck(Editor editor, PlaytestViewport viewport, LogService log, Function<Runnable, Runnable> delay) {
        this.editor = editor;
        this.viewport = viewport;
        this.log = log;
        this.requestUpdate = delay.apply(this::update);

        // Any edit may change what is reachable; a new selection may ask for a different way.
        editor.addSceneListener(this::changed);
        editor.selection().addListener(this::changed);
    }

    public ReachabilityMarks marks() {
        return viewport.reachabilityMarks();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;

        if (enabled) {
            log.info(ESTIMATE_NOTE);
            update();
        } else {
            marks().clear();
            lastLogged = List.of();
        }
        notifyListeners();
    }

    /** Only platforms the actor gets to within this many jumps count; none for no limit. */
    public Maybe<Integer> jumpLimit() {
        return jumpLimit;
    }

    public void setJumpLimit(Maybe<Integer> jumpLimit) {
        jumpLimit.ifSome(limit -> {
            if (limit < 0) {
                throw new IllegalArgumentException("A jump limit cannot be negative, got " + limit);
            }
        });
        if (this.jumpLimit.equals(jumpLimit)) {
            return;
        }
        this.jumpLimit = jumpLimit;

        if (enabled) {
            update();
        }
        notifyListeners();
    }

    /** Called when the check is switched on or off or its jump limit changes. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void changed() {
        if (enabled) {
            requestUpdate.run();
        }
    }

    /** Check the scene as it is now. */
    void update() {
        if (!enabled) {
            return;
        }

        Maybe<BehaviorContext> context = viewport.behaviorContext();
        if (editor.scene().isNone() || context.isNone()) {
            marks().clear();
            return;
        }

        Scene scene = editor.scene().unwrap();
        PlaytestLevel level = PlaytestLevel.build(scene, context.unwrap());
        List<SceneNode> selected = editor.selection().nodes();

        if (selected.size() >= 2) {
            between(scene, context.unwrap(), level, selected.getFirst(), selected.getLast());
        } else {
            fromStart(scene, context.unwrap(), level);
        }
    }

    private void fromStart(Scene scene, BehaviorContext context, PlaytestLevel level) {
        Maybe<PlayerStart> chosen = level.startFor(editor.selection().primary());
        if (chosen.isNone()) {
            problem("the level has no player start to check from. "
                + "Add one (Edit > Add player start), or Shift+click two objects to see the way between them.");
            return;
        }

        PlayerStart start = chosen.unwrap();
        if (start.settings().isNone()) {
            problem("player start '" + start.name() + "' has invalid parameters: "
                + String.join("; ", start.problems()) + ".");
            return;
        }

        PlatformGraph graph = PlatformGraph.build(scene, context, start.settings().unwrap());
        Maybe<Platform> ground = graph.platformUnder(start.feet());
        if (ground.isNone()) {
            problem("player start '" + start.name() + "' is not above any solid.");
            return;
        }

        show(ReachabilityReport.fromStart(graph, start.name(), List.of(ground.unwrap()), jumpLimit));
    }

    private void between(Scene scene, BehaviorContext context, PlaytestLevel level, SceneNode from, SceneNode to) {
        PlatformGraph graph = PlatformGraph.build(scene, context, actorSettings(level));
        List<Platform> fromPlatforms = graph.platformsAt(from);
        List<Platform> toPlatforms = graph.platformsAt(to);

        if (fromPlatforms.isEmpty() || toPlatforms.isEmpty()) {
            SceneNode floating = fromPlatforms.isEmpty() ? from : to;
            problem("'" + floating.getName() + "' is neither solid nor above anything solid.");
            return;
        }

        show(ReachabilityReport.between(graph, from.getName(), fromPlatforms, to.getName(), toPlatforms, jumpLimit));
    }

    // Two arbitrary objects say nothing about the actor, so take the level's first valid player start.
    private static ActorSettings actorSettings(PlaytestLevel level) {
        for (PlayerStart start : level.starts()) {
            if (start.settings().isSome()) {
                return start.settings().unwrap();
            }
        }
        return ActorSettings.DEFAULT;
    }

    private void show(ReachabilityReport report) {
        marks().show(report);
        logOnce(report.summary(), false);
    }

    private void problem(String message) {
        marks().clear();
        logOnce(List.of("Reachability: " + message), true);
    }

    // The check runs after every edit; only a different outcome is worth a log line.
    private void logOnce(List<String> lines, boolean warning) {
        if (lines.equals(lastLogged)) {
            return;
        }
        lastLogged = lines;
        lines.forEach(warning ? log::warn : log::info);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
