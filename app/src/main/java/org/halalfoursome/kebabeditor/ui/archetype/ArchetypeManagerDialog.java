package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.*;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.utils.ErrorDialogs;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class ArchetypeManagerDialog extends JDialog {

    private final Editor editor;
    private final DefaultListModel<ArchetypeDefinition> model = new DefaultListModel<>();

    private JList<ArchetypeDefinition> archetypeList;
    private JButton createButton;
    private JButton editButton;
    private JButton duplicateAsButton;
    private JButton deleteButton;

    public ArchetypeManagerDialog(JFrame owner, Editor editor) {
        super(owner, "Manage archetypes", true);

        this.editor = editor;

        KebabStyle style = KebabStyle.getCurrent();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(640, 480));

        add(buildCenter(style), BorderLayout.CENTER);
        add(buildFooter(style), BorderLayout.SOUTH);

        pack();

        // Escape closes the dialog
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        setLocationRelativeTo(owner);
    }

    private JComponent buildCenter(KebabStyle style) {
        reload();

        archetypeList = new JList<>(model);
        archetypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        archetypeList.setFont(style.uiFont());
        archetypeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean selected, boolean focus
            ) {
                ArchetypeDefinition def = (ArchetypeDefinition) value;
                return super.getListCellRendererComponent(
                    list,
                    def.getDisplayName() + "  (" + def.getId() + ")",
                    index, selected, focus
                );
            }
        });
        archetypeList.addListSelectionListener(e -> updateButtons());

        JScrollPane scroll = new JScrollPane(archetypeList);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return scroll;
    }

    private JPanel buildFooter(KebabStyle style) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        createButton = new JButton("Create");
        createButton.setFont(style.uiFont());
        createButton.addActionListener(e -> {
            Set<String> existingIds = editor.archetypes().stream()
                .map(ArchetypeDefinition::getId)
                .collect(Collectors.toSet());

            new CreateArchetypeDialog(this, existingIds, this::addArchetype).setVisible(true);
        });
        footer.add(createButton);

        editButton = new JButton("Edit");
        editButton.setFont(style.uiFont());
        footer.add(editButton);

        duplicateAsButton = new JButton("Duplicate as...");
        duplicateAsButton.setFont(style.uiFont());
        footer.add(duplicateAsButton);

        deleteButton = new JButton("Delete");
        deleteButton.setFont(style.uiFont());
        footer.add(deleteButton);

        updateButtons();
        return footer;
    }

    private void addArchetype(ArchetypeDefinition definition) {
        try {
            editor.addArchetype(definition);
            reload();
            archetypeList.setSelectedValue(definition, true);
        } catch (IOException e) {
            ErrorDialogs.show(this, "Cannot save archetype", e);
        }
    }

    // the editor holds the real archetypes; the list model is only a view of them
    private void reload() {
        model.clear();
        model.addAll(editor.archetypes());
    }

    private void updateButtons() {
        boolean selected = archetypeList.getSelectedValue() != null;
        editButton.setEnabled(selected);
        duplicateAsButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
    }
}
