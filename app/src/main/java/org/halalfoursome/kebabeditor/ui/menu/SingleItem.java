package org.halalfoursome.kebabeditor.ui.menu;

import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class SingleItem implements MenuItem {
    private JMenuItem component;

    public SingleItem(String label, Optional<String> keyStroke, Runnable onClick) {
        KebabStyle style = KebabStyle.getCurrent();

        component = new JMenuItem(label);
        component.setFont(style.uiFont());
        component.addActionListener(e -> onClick.run());

        if (keyStroke.isPresent()) {
            component.setAccelerator(KeyStroke.getKeyStroke(keyStroke.get()));
        }
    }

    @Override
    public JComponent component() {
        return component;
    }
}
