package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.ui.validation.FieldValidator;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

public final class ParamValueEditor extends JPanel {

    private static final int WIDTH = 170;

    private final KebabStyle style;
    private final ValidationGroup validation;
    private final Maybe<Supplier<List<SceneNode>>> nodes;

    private Supplier<Object> reader = () -> Maybe.none();
    private FieldValidator validator;

    public ParamValueEditor(KebabStyle style, ValidationGroup validation) {
        this(style, validation, Maybe.none());
    }

    public ParamValueEditor(KebabStyle style, ValidationGroup validation, Supplier<List<SceneNode>> nodes) {
        this(style, validation, Maybe.some(nodes));
    }

    private ParamValueEditor(KebabStyle style, ValidationGroup validation, Maybe<Supplier<List<SceneNode>>> nodes) {
        super(new BorderLayout());
        this.style = style;
        this.validation = validation;
        this.nodes = nodes;

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
            case NODE_REF -> nodes.isSome() ? nodeEditor(nodes.unwrap().get(), value) : noDefault();
            case COLOR -> colorEditor(value instanceof Color c ? c : Color.BLACK);
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

    // The button shows the colour and opens the chooser on click; Cancel keeps the current one
    private JComponent colorEditor(Color value) {
        Color[] current = { value };
        JButton button = new JButton();
        Runnable refresh = () -> {
            button.setBackground(current[0]);
            button.setForeground(isLight(current[0]) ? Color.BLACK : Color.WHITE);
            button.setText(String.format("#%02X%02X%02X", current[0].getRed(), current[0].getGreen(), current[0].getBlue()));
        };

        button.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(
                SwingUtilities.getWindowAncestor(button),
                "Choose colour",
                current[0]
            );

            if (chosen != null) {
                current[0] = chosen;
                refresh.run();
            }
        });
        refresh.run();
        reader = () -> current[0];

        return button;
    }

    private static boolean isLight(Color color) {
        return 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue() > 140;
    }

    private record NodeChoice(Maybe<String> id, String label) {

        @Override
        public String toString() {
            return label;
        }
    }

    private JComponent nodeEditor(List<SceneNode> sceneNodes, Object value) {
        Maybe<String> current = value instanceof Maybe<?> maybe && maybe.isSome()
            ? Maybe.some(String.valueOf(maybe.unwrap()))
            : Maybe.none();

        NodeChoice none = new NodeChoice(Maybe.none(), "(none)");
        DefaultComboBoxModel<NodeChoice> model = new DefaultComboBoxModel<>();
        model.addElement(none);
        NodeChoice selected = none;

        for (SceneNode node : sceneNodes) {
            String id = node.getId().value().toString();
            NodeChoice choice = new NodeChoice(Maybe.some(id), nodeLabel(node, id));
            model.addElement(choice);

            if (current.isSomeAnd(id::equals)) {
                selected = choice;
            }
        }

        if (selected == none && current.isSome()) {
            selected = new NodeChoice(current, "(missing) " + current.unwrap());
            model.addElement(selected);
        }

        JComboBox<NodeChoice> box = new JComboBox<>(model);
        box.setSelectedItem(selected);
        reader = () -> ((NodeChoice) box.getSelectedItem()).id();

        return box;
    }

    private static String nodeLabel(SceneNode node, String id) {
        String name = Objects.toString(node.getName(), "");

        return (name.isBlank() ? "(unnamed)" : name) + " (" + id.substring(0, 8) + ")";
    }

    private JComponent noDefault() {
        reader = () -> Maybe.none();
        JLabel label = new JLabel("none (per scene)");
        label.setEnabled(false);

        return label;
    }
}
