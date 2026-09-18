package org.halalfoursome.kebabeditor.ui.menu;

import javax.swing.JComponent;
import javax.swing.JMenu;

import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class Menu implements MenuItem {
    private JMenu component;

    public Menu(String label, MenuItem[] items) {
        KebabStyle style = KebabStyle.getCurrent();

        component = new JMenu(label);
        component.setFont(style.uiFont());

        for (MenuItem item : items) {
            component.add(item.component());
        }
    }

    public void setItems(MenuItem[] items) {
        component.removeAll();

        for (MenuItem item : items) {
            component.add(item.component());
        }
    }

    @Override
    public JComponent component() {
        return component;
    }
}
