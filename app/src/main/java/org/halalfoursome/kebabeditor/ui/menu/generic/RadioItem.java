package org.halalfoursome.kebabeditor.ui.menu.generic;


import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

/** One choice of a group; at most one item of a {@link ButtonGroup} is selected. */
public class RadioItem implements MenuItem {
    private JRadioButtonMenuItem component;

    public RadioItem(String label, ButtonGroup group, Runnable onSelect) {
        this(label, Maybe.none(), group, onSelect);
    }

    public RadioItem(String label, Maybe<String> keyStroke, ButtonGroup group, Runnable onSelect) {
        KebabStyle style = KebabStyle.getCurrent();

        component = new JRadioButtonMenuItem(label);
        component.setFont(style.uiFont());
        component.addActionListener(e -> onSelect.run());
        group.add(component);

        keyStroke.ifSome(key -> component.setAccelerator(KeyStroke.getKeyStroke(key)));
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
