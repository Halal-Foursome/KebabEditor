package org.halalfoursome.kebabeditor.ui.menu.generic;


import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class SingleItem implements MenuItem {
    private JMenuItem component;

    public SingleItem(String label, Maybe<String> keyStroke, Runnable onClick) {
        KebabStyle style = KebabStyle.getCurrent();

        component = new JMenuItem(label);
        component.setFont(style.uiFont());
        component.addActionListener(e -> onClick.run());

        keyStroke.ifSome(key -> component.setAccelerator(KeyStroke.getKeyStroke(key)));
    }

    public void setEnabled(boolean enabled) {
        component.setEnabled(enabled);
    }

    @Override
    public JComponent component() {
        return component;
    }
}
