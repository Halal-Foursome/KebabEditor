package org.halalfoursome.kebabeditor.ui.logs;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.halalfoursome.kebabeditor.logging.LogLevel;

final class LogLevelRenderer extends DefaultTableCellRenderer {

    private static final Color INFO_COLOR = new Color(110, 190, 130);
    private static final Color WARN_COLOR = new Color(230, 180, 80);
    private static final Color ERROR_COLOR = new Color(235, 100, 100);

    private final LogTableModel model;

    LogLevelRenderer(LogTableModel model) {
        this.model = model;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column
    ) {
        Component component = super.getTableCellRendererComponent(
            table,
            value,
            isSelected,
            hasFocus,
            row,
            column
        );

        if (!isSelected) {
            LogLevel level = model.entryAt(table.convertRowIndexToModel(row)).level();
            component.setForeground(colorFor(level));
        }

        return component;
    }

    private static Color colorFor(LogLevel level) {
        return switch (level) {
            case INFO -> INFO_COLOR;
            case WARN -> WARN_COLOR;
            case ERROR -> ERROR_COLOR;
        };
    }
}
