package org.halalfoursome.kebabeditor.ui.menu.generic;

import java.util.Optional;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import org.halalfoursome.kebabeditor.utils.KebabStyle;

/** One choice of a group; at most one item of a {@link ButtonGroup} is selected. */
public class RadioItem implements MenuItem {
    private JRadioButtonMenuItem component;

    public RadioItem(String label, ButtonGroup group, Runnable onSelect) {
        this(label, Optional.empty(), group, onSelect);
    }

    public RadioItem(String label, Optional<String> keyStroke, ButtonGroup group, Runnable onSelect) {
        KebabStyle style = KebabStyle.getCurrent();

        component = new JRadioButtonMenuItem(label);
        component.setFont(style.uiFont());
        component.addActionListener(e -> onSelect.run());
        group.add(component);

        if (keyStroke.isPresent()) {
            component.setAccelerator(KeyStroke.getKeyStroke(keyStroke.get()));
        }
    }

    /** Updates the selection without firing the callback. */
    public void setSelected(boolean selected) {
        component.setSelected(selected);
    }

    @Override
    public JComponent component() {
        return component;
    }
}
