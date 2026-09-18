package org.halalfoursome.kebabeditor.ui.menu;

import javax.swing.JComponent;
import javax.swing.JSeparator;

public class SeparatorItem implements MenuItem {
    private JSeparator component;

    public SeparatorItem() {
        component = new JSeparator();
    }

    @Override
    public JComponent component() {
        return component;
    }
}
