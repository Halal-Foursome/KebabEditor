package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.*;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.ui.validation.Rules;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class DuplicateAsDialog extends JDialog {
    
    private final ValidationGroup validation = new ValidationGroup();
    private final ArchetypeDefinition definition;
    private final Consumer<ArchetypeDefinition> onDuplicate;
    private final JTextField idField = new JTextField();
    private JButton duplicateButton;

    public DuplicateAsDialog(
        JDialog owner, 
        ArchetypeDefinition definitionToDuplicate,
        Set<String> existingIds,
        Consumer<ArchetypeDefinition> consumer
    ) {
        super(owner, "Duplicate archetype as...", true);

        KebabStyle style = KebabStyle.getCurrent();
        onDuplicate = consumer;
        definition = definitionToDuplicate;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(540, 240));

        addComponentListener(new DuplicateAsArchetypeAdapter());
        validation.addListener(this::updateDuplicateEnabled);

        add(buildCenter(style, existingIds), BorderLayout.CENTER);
        add(buildFooter(style), BorderLayout.SOUTH);

        pack();

        // Escape to close dialog
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        setLocationRelativeTo(owner);
    }

    private JComponent buildCenter(KebabStyle style, Set<String> existingIds) {
        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 16, 4, 16);

        idField.setFont(style.uiFont());
        validation.add(idField, Rules.all(
            Rules.identifier("ID"),
            Rules.unique("ID", () -> existingIds)
        ));
        addLabeled(center, constraints, "ID", idField, style);

        JLabel idNote = label("The ID cannot be changed after the archetype is created.", style);
        idNote.setFont(style.uiFont().deriveFont(Font.ITALIC, style.uiFont().getSize2D() - 1f));
        center.add(idNote, constraints);

        return center;
    }

    private void addLabeled(
        JPanel panel, GridBagConstraints constraints,
        String text, JComponent component, KebabStyle style
    ) {
        panel.add(label(text, style), constraints);
        panel.add(component, constraints);
    }

    private JLabel label(String text, KebabStyle style) {
        JLabel label = new JLabel(text);
        label.setFont(style.uiFont());
        return label;
    }

    private ArchetypeDefinition buildDefinition() {
        return definition.duplicateAs(idField.getText());
    }

    private JPanel buildFooter(KebabStyle style) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        duplicateButton = new JButton("Duplicate");
        duplicateButton.setFont(style.uiFont());
        updateDuplicateEnabled();
        duplicateButton.addActionListener(e -> {
            onDuplicate.accept(buildDefinition());
            dispose();
        });
        footer.add(duplicateButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(style.uiFont());
        cancelButton.addActionListener(e -> dispose());
        footer.add(cancelButton);

        return footer;
    }

    private void updateDuplicateEnabled() {
        if (duplicateButton != null) {
            duplicateButton.setEnabled(validation.isValid());
        }
    }

    private class DuplicateAsArchetypeAdapter extends ComponentAdapter {

        @Override public void componentMoved(ComponentEvent e) { 
            validation.hidePopups(); 
        }

        @Override public void componentResized(ComponentEvent e) { 
            validation.hidePopups();
        }
    }
}
