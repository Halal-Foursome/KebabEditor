package org.halalfoursome.kebabeditor.ui.menu;

import org.halalfoursome.kebabeditor.playtest.PathRays;
import org.halalfoursome.kebabeditor.ui.menu.generic.CheckItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
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

        // Rays show themselves after each run, so mirror their state.
        Runnable sync = () -> {
            showRays.setSelected(rays.isVisible());
            clearRays.setEnabled(!rays.isEmpty());
        };
        sync.run();
        rays.addListener(sync);

        setItems(new MenuItem[] {
            run,
            new SeparatorItem(),
            showRays,
            clearRays,
        });
    }
}
