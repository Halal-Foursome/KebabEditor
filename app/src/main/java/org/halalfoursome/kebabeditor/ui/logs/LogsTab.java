package org.halalfoursome.kebabeditor.ui.logs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.halalfoursome.kebabeditor.logging.LogLevel;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.ui.logs.LogTableModel.Column;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

/** Swing view for LogService. Construct on the Swing Event Dispatch Thread. */
public final class LogsTab extends JPanel {

    private static final String ALL_LEVELS = "All";
    private static final int TIME_COLUMN_WIDTH = 110;
    private static final int LEVEL_COLUMN_WIDTH = 90;
    private static final int ROW_PADDING = 6;

    private final LogService logService;
    private final LogTableModel model = new LogTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<LogTableModel> sorter = new TableRowSorter<>(model);
    private final AtomicBoolean refreshScheduled = new AtomicBoolean();
    private final Runnable logListener = this::scheduleRefresh;

    public LogsTab(LogService logService) {
        super(new BorderLayout(0, 6));
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Create LogsTab on the Swing EDT");
        }
        this.logService = Objects.requireNonNull(logService, "logService");

        KebabStyle style = KebabStyle.getCurrent();

        add(buildToolbar(style.uiFont()), BorderLayout.NORTH);
        add(buildContent(style), BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        logService.addListener(logListener);
        refresh();
    }

    @Override
    public void removeNotify() {
        logService.removeListener(logListener);
        super.removeNotify();
    }

    private JPanel buildToolbar(Font font) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 2));

        DefaultComboBoxModel<Object> levels = new DefaultComboBoxModel<>();
        levels.addElement(ALL_LEVELS);
        for (LogLevel level : LogLevel.values()) {
            levels.addElement(level);
        }
        JComboBox<Object> levelFilter = new JComboBox<>(levels);
        levelFilter.addActionListener(event -> filterByLevel(levelFilter.getSelectedItem()));

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(event -> logService.clear());

        toolbar.add(new JLabel("Level:"));
        toolbar.add(levelFilter);
        toolbar.add(clearButton);
        for (Component component : toolbar.getComponents()) {
            component.setFont(font);
        }

        return toolbar;
    }

    private JScrollPane buildContent(KebabStyle style) {
        configureTable(style.uiFont());

        return new JScrollPane(table);
    }

    private void configureTable(Font font) {
        table.setFont(font);
        table.getTableHeader().setFont(font);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(table.getFontMetrics(font).getHeight() + ROW_PADDING);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowSorter(sorter);
        table.setDefaultRenderer(String.class, new LogLevelRenderer(model));

        fixColumnWidth(Column.TIME, TIME_COLUMN_WIDTH);
        fixColumnWidth(Column.LEVEL, LEVEL_COLUMN_WIDTH);
    }

    private void fixColumnWidth(Column column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column.ordinal());
        tableColumn.setMinWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setPreferredWidth(width);
    }

    private void filterByLevel(Object selected) {
        if (selected instanceof LogLevel level) {
            sorter.setRowFilter(new RowFilter<LogTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    return entry.getModel().entryAt(entry.getIdentifier()).level() == level;
                }
            });
        } else {
            sorter.setRowFilter(null);
        }
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
    }
}
