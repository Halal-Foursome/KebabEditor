package org.halalfoursome.kebabeditor.ui.logs;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.halalfoursome.kebabeditor.logging.LogEntry;
import org.halalfoursome.kebabeditor.logging.LogLevel;
import org.halalfoursome.kebabeditor.logging.LogService;

/** Swing view for LogService. Construct on the Swing Event Dispatch Thread. */
public final class LogsTab extends JPanel {
    private final LogService logService;
    private final LogTableModel model = new LogTableModel();
    private final JTable table = new JTable(model);
    private final JTextArea details = new JTextArea(4, 40);
    private final AtomicBoolean refreshScheduled = new AtomicBoolean();
    private final Runnable logListener = this::scheduleRefresh;

    public LogsTab(LogService logService) {
        super(new BorderLayout(0, 6));
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Create LogsTab on the Swing EDT");
        }
        this.logService = Objects.requireNonNull(logService, "logService");

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 2));
        JComboBox<String> levelFilter = new JComboBox<>(new String[] {"All", "INFO", "WARN", "ERROR"});
        JButton clearButton = new JButton("Clear");
        toolbar.add(new JLabel("Level:"));
        toolbar.add(levelFilter);
        toolbar.add(clearButton);
        add(toolbar, BorderLayout.NORTH);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(75);
        table.getColumnModel().getColumn(2).setPreferredWidth(600);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        TableRowSorter<LogTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        levelFilter.addActionListener(event -> {
            String selected = (String) levelFilter.getSelectedItem();
            sorter.setRowFilter(selected == null || selected.equals("All") ? null :
                new javax.swing.RowFilter<LogTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                        return selected.equals(entry.getStringValue(1));
                    }
                });
        });
        clearButton.addActionListener(event -> logService.clear());

        details.setEditable(false);
        details.setLineWrap(false);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedDetails();
            }
        });

        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
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
                        table, value, isSelected, hasFocus, row, column
                );

                if (!isSelected) {
                    int modelRow = table.convertRowIndexToModel(row);
                    LogLevel level = model.getEntry(modelRow).level();

                    component.setForeground(switch (level) {
                        case INFO -> new Color(110, 190, 130);
                        case WARN -> new Color(230, 180, 80);
                        case ERROR -> new Color(235, 100, 100);
                    });
                }

                return component;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        logService.addListener(logListener);
        refresh();
    }

    /** Call when the tab is permanently removed, to avoid retaining its UI in memory. */
    public void dispose() {
        logService.removeListener(logListener);
    }

    private void scheduleRefresh() {
        if (refreshScheduled.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                refreshScheduled.set(false);
                refresh();
            });
        }
    }

    private void refresh() {
        model.setEntries(logService.entries());
        showSelectedDetails();
    }

    private void showSelectedDetails() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            details.setText("");
            return;
        }
        LogEntry entry = model.getEntry(table.convertRowIndexToModel(viewRow));
        StringBuilder text = new StringBuilder(entry.message());
        if (entry.cause() != null) {
            StringWriter buffer = new StringWriter();
            entry.cause().printStackTrace(new PrintWriter(buffer));
            text.append("\n\n").append(buffer);
        }
        details.setText(text.toString());
        details.setCaretPosition(0);
    }

    private static final class LogTableModel extends AbstractTableModel {
        private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
        private final List<LogEntry> entries = new ArrayList<>();
        private static final String[] COLUMNS = {"Time", "Level", "Message"};

        void setEntries(List<LogEntry> updated) {
            entries.clear();
            entries.addAll(updated);
            fireTableDataChanged();
        }

        LogEntry getEntry(int row) {
            return entries.get(row);
        }

        @Override public int getRowCount() { return entries.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public Class<?> getColumnClass(int column) { return String.class; }

        @Override public Object getValueAt(int row, int column) {
            LogEntry entry = entries.get(row);
            return switch (column) {
                case 0 -> entry.timestamp().format(TIME);
                case 1 -> entry.level().name();
                case 2 -> entry.message();
                default -> throw new IndexOutOfBoundsException("column " + column);
            };
        }
    }
}
