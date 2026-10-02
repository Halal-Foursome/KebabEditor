package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.*;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;

public class DuplicateAsDialog extends AbstractArchetypeDialog {

    private final ArchetypeDefinition definition;
    private final Supplier<Collection<String>> existingIds;
    private final Consumer<ArchetypeDefinition> onDuplicate;
    private JTextField idField;

    public DuplicateAsDialog(
        JDialog owner,
        ArchetypeDefinition definitionToDuplicate,
        Supplier<Collection<String>> existingIds,
        Consumer<ArchetypeDefinition> consumer
    ) {
        super(owner, "Duplicate archetype as...", "Duplicate");

        definition = definitionToDuplicate;
        this.existingIds = existingIds;
        onDuplicate = consumer;

        init(new Dimension(540, 240));
    }

    @Override
    protected void buildForm(JPanel form, GridBagConstraints constraints) {
        idField = addIdField(form, constraints, existingIds);
    }

    @Override
    protected void onConfirm() {
        onDuplicate.accept(definition.duplicateAs(idField.getText()));
    }
}
