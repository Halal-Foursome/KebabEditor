package org.halalfoursome.kebabeditor.ui.menu;


import javax.swing.JFrame;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.options.OptionsDialog;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class OptionsMenu extends Menu {

    public OptionsMenu(JFrame frame, Editor editor) {
        super("Options");

        setItems(new MenuItem[] {
            new SingleItem("Blockout view options", Maybe.some("control O B"), () -> {
                OptionsDialog optionsDialog = new OptionsDialog(frame);
                optionsDialog.setVisible(true);
            }),

            new SeparatorItem(),
        });
    }
}
