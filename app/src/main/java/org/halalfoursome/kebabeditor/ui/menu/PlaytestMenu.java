package org.halalfoursome.kebabeditor.ui.menu;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;

import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.playtest.PathThickness;
import org.halalfoursome.kebabeditor.ui.menu.generic.CheckItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.RadioItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.playtest.PlaytestController;
import org.halalfoursome.kebabeditor.ui.playtest.ReachabilityCheck;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class PlaytestMenu extends Menu {

    /** Jump limits offered without typing one in. */
    private static final List<Integer> PRESET_JUMP_LIMITS = List.of(1, 2, 3, 4, 5);

    /**
     * @param playtest none when the viewport is unavailable, which disables the menu
     */
    public PlaytestMenu(Maybe<PlaytestController> playtest) {
        super("Playtest");

        if (playtest.isNone()) {
            component().setEnabled(false);
            return;
        }

        PlaytestController controller = playtest.unwrap();
        PathRays rays = controller.pathRays();

        SingleItem run = new SingleItem("Run / stop level", Maybe.some("F5"), controller::toggle);

        CheckItem showRays = new CheckItem("Show path rays", Maybe.none(), rays::setVisible);
        SingleItem clearRays = new SingleItem("Clear path rays", Maybe.none(), controller::clearPathRays);

        // Rays show themselves after each run, so mirror their state. The path being recorded cannot be cleared.
        Runnable sync = () -> {
            showRays.setSelected(rays.isVisible());
            clearRays.setEnabled(!rays.isEmpty() && !controller.isRunning());
        };
        sync.run();
        rays.addListener(sync);
        controller.addListener(sync);

        ReachabilityCheck reachability = controller.reachability();
        CheckItem showReachability = new CheckItem("Show reachability", Maybe.some("F6"), reachability::setEnabled);
        showReachability.setSelected(reachability.isEnabled());
        reachability.addListener(() -> showReachability.setSelected(reachability.isEnabled()));

        setItems(new MenuItem[] {
            run,
            new SeparatorItem(),
            showRays,
            clearRays,
            thicknessMenu(rays),
            new SeparatorItem(),
            showReachability,
            jumpLimitMenu(reachability),
        });
    }

    private Menu jumpLimitMenu(ReachabilityCheck reachability) {
        Menu menu = new Menu("Jump limit");
        ButtonGroup group = new ButtonGroup();
        List<MenuItem> items = new ArrayList<>();
        List<Runnable> syncs = new ArrayList<>();
        Runnable sync = () -> syncs.forEach(Runnable::run);

        RadioItem none = new RadioItem("No limit", group, () -> reachability.setJumpLimit(Maybe.none()));
        syncs.add(() -> none.setSelected(reachability.jumpLimit().isNone()));
        items.add(none);

        for (int limit : PRESET_JUMP_LIMITS) {
            RadioItem item = new RadioItem(jumps(limit), group, () -> reachability.setJumpLimit(Maybe.some(limit)));
            syncs.add(() -> item.setSelected(reachability.jumpLimit().isSomeAnd(l -> l == limit)));
            items.add(item);
        }

        // Clicking selects the item before the dialog opens, so put the real choice back after it.
        RadioItem custom = new RadioItem("Custom...", group, () -> {
            askJumpLimit(reachability.jumpLimit()).ifSome(limit -> reachability.setJumpLimit(Maybe.some(limit)));
            sync.run();
        });
        syncs.add(() -> {
            Maybe<Integer> limit = reachability.jumpLimit().filter(l -> !PRESET_JUMP_LIMITS.contains(l));
            custom.setSelected(limit.isSome());
            ((AbstractButton) custom.component()).setText(limit.mapOr("Custom...", l -> "Custom (" + jumps(l) + ")..."));
        });
        items.add(custom);

        sync.run();
        reachability.addListener(sync);
        menu.setItems(items.toArray(MenuItem[]::new));
        return menu;
    }

    // None when cancelled or the input is not a jump count.
    private Maybe<Integer> askJumpLimit(Maybe<Integer> current) {
        Object input = JOptionPane.showInputDialog(
            component(),
            "Only count platforms the actor gets to within this many jumps (a drop counts as one):",
            "Jump limit",
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            String.valueOf(current.unwrapOr(PRESET_JUMP_LIMITS.getLast() + 1))
        );
        if (input == null) {
            return Maybe.none();
        }

        try {
            int limit = Integer.parseInt(input.toString().trim());
            if (limit < 0) {
                throw new NumberFormatException();
            }
            return Maybe.some(limit);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                component(),
                "'" + input + "' is not a whole number of jumps, 0 or more.",
                "Invalid jump limit",
                JOptionPane.WARNING_MESSAGE
            );
            return Maybe.none();
        }
    }

    private static String jumps(int count) {
        return count == 1 ? "1 jump" : count + " jumps";
    }

    private static Menu thicknessMenu(PathRays rays) {
        Menu menu = new Menu("Path thickness");
        ButtonGroup group = new ButtonGroup();
        List<MenuItem> items = new ArrayList<>();

        for (PathThickness thickness : PathThickness.values()) {
            RadioItem item = new RadioItem(thickness.label(), group, () -> rays.setThickness(thickness));
            item.setSelected(rays.thickness() == thickness);
            rays.addListener(() -> item.setSelected(rays.thickness() == thickness));
            items.add(item);
        }

        menu.setItems(items.toArray(MenuItem[]::new));
        return menu;
    }
}
