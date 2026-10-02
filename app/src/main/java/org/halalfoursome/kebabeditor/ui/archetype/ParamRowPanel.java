package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.ui.validation.FieldValidator;
import org.halalfoursome.kebabeditor.ui.validation.Rules;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class ParamRowPanel extends JPanel {

    private final JTextField nameField = new JTextField();
    private final JComboBox<ParamType> typeBox = new JComboBox<>(ParamType.values());

    public ParamRowPanel(
        KebabStyle style,
        ValidationGroup validation,
        Consumer<ParamRowPanel> onRemove,
        Function<ParamRowPanel, Collection<String>> otherNames,
        String paramName,
        ParamType paramType
    ) {
        this(style, validation, onRemove, otherNames);

        nameField.setText(paramName);
        typeBox.setSelectedIndex(paramType.ordinal());
    }

    public ParamRowPanel(
        KebabStyle style,
        ValidationGroup validation,
        Consumer<ParamRowPanel> onRemove,
        Function<ParamRowPanel, Collection<String>> otherNames
    ) {
        super(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        FieldValidator nameValidator = validation.add(nameField, Rules.all(
            Rules.identifier("Parameter name"),
            Rules.unique("Parameter name", () -> otherNames.apply(this))
        ));
        nameField.setFont(style.uiFont());
        c.weightx = 1.0;
        add(nameField, c);

        typeBox.setFont(style.uiFont());
        typeBox.setRenderer(new ParamTypeRenderer());
        c.weightx = 0;
        add(typeBox, c);

        JButton removeButton = new JButton("Remove");
        removeButton.setFont(style.uiFont());
        removeButton.addActionListener(e -> {
            // the row must leave the list first, or the others still see its name as taken
            onRemove.accept(this);
            validation.remove(nameValidator);
        });
        add(removeButton, c);
    }

    public String name() {
        return nameField.getText();
    }

    public ParamType type() {
        return (ParamType) typeBox.getSelectedItem();
    }

    // BoxLayout would otherwise stretch the row vertically
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
