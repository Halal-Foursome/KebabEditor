package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Supplier;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.ui.validation.FieldValidator;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

public final class ParamDefaultEditor extends JPanel {

    private static final int WIDTH = 170;

    private final KebabStyle style;
    private final ValidationGroup validation;

    private Supplier<Object> reader = () -> Maybe.none();
    private FieldValidator validator;

    public ParamDefaultEditor(KebabStyle style, ValidationGroup validation) {
        super(new BorderLayout());
        this.style = style;
        this.validation = validation;

        setPreferredSize(new Dimension(WIDTH, 28));
    }

    public void showFor(ParamType type, Object value) {
        dispose();
        removeAll();

        JComponent editor = switch (type) {
            case INT -> intEditor(value instanceof Integer i ? i : 0);
            case FLOAT -> floatEditor(value instanceof Float f ? f : 0f);
            case BOOL -> boolEditor(value instanceof Boolean b && b);
            case STRING, REL_PATH -> textEditor(value instanceof String s ? s : "");
            case VEC3 -> vec3Editor(value instanceof Vec3 v ? v : Vec3.zero());
            case NODE_REF -> noDefault();
        };

        editor.setFont(style.uiFont());
        add(editor, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public Object value() {
        return reader.get();
    }

    public void dispose() {
        if (validator != null) {
            validation.remove(validator);
            validator = null;
        }
    }

    private JComponent intEditor(int value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        reader = () -> ((Number) spinner.getValue()).intValue();

        return spinner;
    }

    private JComponent floatEditor(float value) {
        JSpinner spinner = new JSpinner(
            new SpinnerNumberModel((double) value, -(double) Float.MAX_VALUE, (double) Float.MAX_VALUE, 0.1)
        );
        reader = () -> ((Number) spinner.getValue()).floatValue();

        return spinner;
    }

    private JComponent boolEditor(boolean value) {
        JCheckBox box = new JCheckBox("true", value);
        reader = box::isSelected;

        return box;
    }

    private JComponent textEditor(String value) {
        JTextField field = new JTextField(value);
        reader = field::getText;

        return field;
    }

    private JComponent vec3Editor(Vec3 value) {
        JTextField field = new JTextField(value.toString());
        validator = validation.add(field, text -> Vec3.fromString(text).isSome()
            ? Maybe.none()
            : Maybe.some("Default must look like (1.0, 2.0, 3.0)")
        );
        reader = () -> Vec3.fromString(field.getText()).unwrapOr(Vec3.zero());

        return field;
    }

    private JComponent noDefault() {
        reader = () -> Maybe.none();
        JLabel label = new JLabel("none (per scene)");
        label.setEnabled(false);

        return label;
    }
}
