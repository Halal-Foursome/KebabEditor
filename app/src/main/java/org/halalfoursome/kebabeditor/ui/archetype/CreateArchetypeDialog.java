package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.*;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.ui.validation.Rules;

public class CreateArchetypeDialog extends AbstractArchetypeDialog {

    private final Supplier<Collection<String>> existingIds;
    private final Consumer<ArchetypeDefinition> onCreate;

    private final JTextField displayNameField = new JTextField();
    private final IconPickerPanel iconPicker = new IconPickerPanel(style);
    private final ParamListPanel paramList = new ParamListPanel(style, validation);
    private JTextField idField;

    public CreateArchetypeDialog(
        JDialog owner,
        Supplier<Collection<String>> existingIds,
        Consumer<ArchetypeDefinition> consumer
    ) {
        super(owner, "Create new archetype", "Create");

        this.existingIds = existingIds;
        onCreate = consumer;

        init(new Dimension(540, 640));
    }

    @Override
    protected void buildForm(JPanel form, GridBagConstraints constraints) {
        idField = addIdField(form, constraints, existingIds);

        displayNameField.setFont(style.uiFont());
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
        onCreate.accept(new ArchetypeDefinition(
            idField.getText(),
            displayNameField.getText().trim(),
            iconPicker.selectedIcon(),
            paramList.params()
        ));
    }
}
