package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class ParamListPanel extends JPanel {

    private final KebabStyle style;
    private final ValidationGroup validation;
    private final JPanel rows = new JPanel();

    public ParamListPanel(
        KebabStyle style, 
        ValidationGroup validation, 
        Map<String, ParamType> params
    ) {
        this(style, validation);

        for (var entry : params.entrySet()) {
            addRow(entry.getKey(), entry.getValue());
        }
    }

    public ParamListPanel(KebabStyle style, ValidationGroup validation) {
        super(new BorderLayout(0, 4));
        this.style = style;
        this.validation = validation;

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScroll(), BorderLayout.CENTER);
    }

    public void addRow(String paramName, ParamType paramType) {
        rows.add(new ParamRowPanel(style, validation, this::removeRow, this::namesExcept, paramName, paramType));
        refresh();
    }

    public void addRow() {
        rows.add(new ParamRowPanel(style, validation, this::removeRow, this::namesExcept));
        refresh();
    }

    public Map<String, ParamType> params() {
        Map<String, ParamType> params = new LinkedHashMap<>();
        for (var component : rows.getComponents()) {
            if (component instanceof ParamRowPanel row) {
                params.put(row.name().trim(), row.type());
            }
        }
        return params;
    }

    private Collection<String> namesExcept(ParamRowPanel self) {
        return Arrays.stream(rows.getComponents())
            .filter(component -> component != self && component instanceof ParamRowPanel)
            .map(component -> ((ParamRowPanel) component).name().trim())
            .toList();
    }

    private void removeRow(ParamRowPanel row) {
        rows.remove(row);
        refresh();
    }

    private void refresh() {
        rows.revalidate();
        rows.repaint();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());

        JLabel title = new JLabel("Parameters");
        title.setFont(style.uiFont());
        header.add(title, BorderLayout.WEST);

        JButton addButton = new JButton("Add");
        addButton.setFont(style.uiFont());
        addButton.addActionListener(e -> addRow());
        header.add(addButton, BorderLayout.EAST);

        return header;
    }

    private JScrollPane buildScroll() {
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(rows, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setPreferredSize(new Dimension(0, 140));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }
}
