package org.halalfoursome.kebabeditor.ui.menu;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;

import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.playtest.PathThickness;
import org.halalfoursome.kebabeditor.ui.menu.generic.CheckItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.RadioItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.playtest.PlaytestController;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class PlaytestMenu extends Menu {

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

        setItems(new MenuItem[] {
            run,
            new SeparatorItem(),
            showRays,
            clearRays,
            thicknessMenu(rays),
        });
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
