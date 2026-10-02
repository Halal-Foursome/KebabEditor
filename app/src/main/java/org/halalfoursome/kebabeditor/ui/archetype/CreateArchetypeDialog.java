package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.*;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.ui.validation.Rules;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class CreateArchetypeDialog extends JDialog {

    private final ValidationGroup validation = new ValidationGroup();
    private final Consumer<ArchetypeDefinition> onCreate;

    private final JTextField idField = new JTextField();
    private final JTextField displayNameField = new JTextField();
    private final IconPickerPanel iconPicker;
    private final ParamListPanel paramList;
    private JButton createButton;

    public CreateArchetypeDialog(
        JDialog owner, 
        Set<String> existingIds,
        Consumer<ArchetypeDefinition> consumer
    ) {
        super(owner, "Create new archetype", true);

        KebabStyle style = KebabStyle.getCurrent();
        onCreate = consumer;
        iconPicker = new IconPickerPanel(style);
        paramList = new ParamListPanel(style, validation);

        addComponentListener(new CreateArchetypeAdapter());
        validation.addListener(this::updateCreateEnabled);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(540, 640));

        add(buildCenter(style, existingIds), BorderLayout.CENTER);
        add(buildFooter(style), BorderLayout.SOUTH);

        // Escape closes the dialog
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

        displayNameField.setFont(style.uiFont());
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

    private ArchetypeDefinition buildDefinition() {
        Maybe<ArchetypeIcon> icon = iconPicker.selectedFile()
            .<ArchetypeIcon>map(file -> new ArchetypeIcon.Handle(file.getAbsolutePath()));

        return new ArchetypeDefinition(
            idField.getText(),
            displayNameField.getText().trim(),
            icon,
            paramList.params()
        );
    }

    private void updateCreateEnabled() {
        if (createButton != null) {
            createButton.setEnabled(validation.isValid());
        }
    }

    private JPanel buildFooter(KebabStyle style) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        createButton = new JButton("Create");
        createButton.setFont(style.uiFont());
        updateCreateEnabled();
        createButton.addActionListener(e -> {
            onCreate.accept(buildDefinition());
            dispose();
        });
        footer.add(createButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(style.uiFont());
        cancelButton.addActionListener(e -> dispose());
        footer.add(cancelButton);

        return footer;
    }

    private class CreateArchetypeAdapter extends ComponentAdapter {

        @Override public void componentMoved(ComponentEvent e) { 
            validation.hidePopups(); 
        }

        @Override public void componentResized(ComponentEvent e) { 
            validation.hidePopups();
        }
    }
}
