package org.halalfoursome.kebabeditor.ui.menu;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class MenuBar extends JMenuBar {
    public MenuBar(JFrame frame) {
        KebabStyle style = KebabStyle.getCurrent();

        setFont(style.uiFont());
        frame.setJMenuBar(this);
    }

    public void addMenu(String label, MenuItem[] items) {
        add(new JMenu(label) {{
            for (MenuItem item : items) {
                add(item.component());
            }
        }});
    }

    public void addBarSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(8, 24));
        add(separator);
    }
}
