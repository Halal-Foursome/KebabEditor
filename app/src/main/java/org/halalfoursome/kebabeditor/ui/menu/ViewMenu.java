package org.halalfoursome.kebabeditor.ui.menu;

import java.util.Optional;

import javax.swing.JFrame;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;

public class ViewMenu extends Menu {

    public ViewMenu(JFrame frame, Editor editor) {
        super("View");

        setItems(new MenuItem[] {
            new SingleItem("Blockout view", Optional.of("control V B"), () -> {

            }),

            new SeparatorItem(),
        });
    }
}
