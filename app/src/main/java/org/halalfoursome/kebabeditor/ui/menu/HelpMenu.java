package org.halalfoursome.kebabeditor.ui.menu;

import javax.swing.JFrame;

import org.halalfoursome.kebabeditor.KebabApplication;
import org.halalfoursome.kebabeditor.ui.AboutDialog;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.halalfoursome.kebabeditor.utils.WebLinks;

public class HelpMenu extends Menu {

    public HelpMenu(JFrame frame) {
        super("Help");

        setItems(new MenuItem[] {
            new SingleItem(
                "Documentation",
                Maybe.some("F1"),
                () -> WebLinks.open(frame, KebabApplication.DOCUMENTATION_URL)
            ),
            new SeparatorItem(),
            new SingleItem(
                "About",
                Maybe.some("F12"),
                () -> new AboutDialog(frame).setVisible(true)
            )
        });
    }
}
