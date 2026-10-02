package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.ui.validation.Rules;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class EditArchetypeDialog extends JDialog {

    private final ValidationGroup validation = new ValidationGroup();
    private final ArchetypeDefinition definition;
    private final Runnable callback;

    private final JTextField displayNameField = new JTextField();
    private final IconPickerPanel iconPicker;
    private final ParamListPanel paramList;
    private JButton saveButton;

    public EditArchetypeDialog(
        JDialog owner, 
        ArchetypeDefinition definitionToEdit,
        Runnable onArchetypeEdited
    ) {
        super(owner, "Edit archetype", true);

        KebabStyle style = KebabStyle.getCurrent();
        callback = onArchetypeEdited;
        definition = definitionToEdit;
        iconPicker = new IconPickerPanel(style, definition.getIcon());
        paramList = new ParamListPanel(style, validation, definition.getParams());

        addComponentListener(new EditArchetypeAdapter());
        validation.addListener(this::updateEditEnabled);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(540, 640));

        add(buildCenter(style), BorderLayout.CENTER);
        add(buildFooter(style), BorderLayout.SOUTH);

        // Escape closes the dialog
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        setLocationRelativeTo(owner);
    }

    private JComponent buildCenter(KebabStyle style) {
        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 16, 4, 16);

        displayNameField.setFont(style.uiFont());
        displayNameField.setText(definition.getDisplayName());
        validation.add(displayNameField, Rules.notEmpty("Display name"));
        addLabeled(center, constraints, "Display name", displayNameField, style);

        addLabeled(center, constraints, "Icon", iconPicker, style);

        // the parameter list takes all remaining space
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        center.add(paramList, constraints);

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

    private void buildDefinition(ArchetypeDefinition definition) {
        definition.setDisplayName(displayNameField.getText().trim());
        definition.setIcon(iconPicker.selectedIcon());
        definition.setParams(paramList.params());
    }

    private void updateEditEnabled() {
        if (saveButton != null) {
            saveButton.setEnabled(validation.isValid());
        }
    }

    private JPanel buildFooter(KebabStyle style) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        saveButton = new JButton("Save");
        saveButton.setFont(style.uiFont());
        updateEditEnabled();
        saveButton.addActionListener(e -> {
            buildDefinition(definition);
            callback.run();
            dispose();
        });
        footer.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(style.uiFont());
        cancelButton.addActionListener(e -> dispose());
        footer.add(cancelButton);

        return footer;
    }

    private class EditArchetypeAdapter extends ComponentAdapter {

        @Override public void componentMoved(ComponentEvent e) { 
            validation.hidePopups(); 
        }

        @Override public void componentResized(ComponentEvent e) { 
            validation.hidePopups();
        }
    }
}
