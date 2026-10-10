package org.halalfoursome.kebabeditor.ui.logs;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.halalfoursome.kebabeditor.logging.LogEntry;

final class LogTableModel extends AbstractTableModel {

    enum Column {
        TIME("Time"),
        LEVEL("Level"),
        MESSAGE("Message");

        private final String title;

        Column(String title) {
            this.title = title;
        }
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Column[] COLUMNS = Column.values();

    private List<LogEntry> entries = List.of();

    void setEntries(List<LogEntry> updated) {
        entries = updated;
        fireTableDataChanged();
    }

    LogEntry entryAt(int row) {
        return entries.get(row);
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column].title;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        LogEntry entry = entries.get(row);

        return switch (COLUMNS[column]) {
            case TIME -> entry.timestamp().format(TIME_FORMAT);
            case LEVEL -> entry.level().name();
            case MESSAGE -> entry.message();
        };
    }
}
