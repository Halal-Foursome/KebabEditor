package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.halalfoursome.kebabeditor.archetype.ParamType;

public class ParamTypeRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
        JList<?> list, Object value, int index,
        boolean isSelected, boolean cellHasFocus
    ) {
        return super.getListCellRendererComponent(
            list,
            value instanceof ParamType type ? type.displayName() : value,
            index, isSelected, cellHasFocus
        );
    }
}
