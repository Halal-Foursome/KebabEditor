package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.Dimension;
import java.awt.GridBagConstraints;

import javax.swing.*;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.ui.validation.Rules;

public class EditArchetypeDialog extends AbstractArchetypeDialog {

    private final ArchetypeDefinition definition;
    private final Runnable callback;

    private final JTextField displayNameField = new JTextField();
    private final IconPickerPanel iconPicker;
    private final ParamListPanel paramList;

    public EditArchetypeDialog(
        JDialog owner,
        ArchetypeDefinition definitionToEdit,
        Runnable onArchetypeEdited
    ) {
        super(owner, "Edit archetype", "Save");

        definition = definitionToEdit;
        callback = onArchetypeEdited;
        iconPicker = new IconPickerPanel(style, definition.getIcon());
        paramList = new ParamListPanel(style, validation, definition.getParams());

        init(new Dimension(540, 640));
    }

    @Override
    protected void buildForm(JPanel form, GridBagConstraints constraints) {
        displayNameField.setFont(style.uiFont());
        displayNameField.setText(definition.getDisplayName());
        validation.add(displayNameField, Rules.notEmpty("Display name"));
        addLabeled(form, constraints, "Display name", displayNameField);

        addLabeled(form, constraints, "Icon", iconPicker);

        // the parameter list takes all remaining space
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        form.add(paramList, constraints);
    }

    @Override
    protected void onConfirm() {
        definition.setDisplayName(displayNameField.getText().trim());
        definition.setIcon(iconPicker.selectedIcon());
        definition.setParams(paramList.params());
        callback.run();
    }
}
