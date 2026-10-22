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
    private final java.util.List<FieldValidator> vectorValidators = new java.util.ArrayList<>();
    private final java.util.List<Runnable> commitListeners = new java.util.ArrayList<>();
    private boolean updating;
    private Supplier<Maybe<java.nio.file.Path>> pathBase = Maybe::none;

    /** Scene-relative paths are available when this editor is used on an instance. */
    public void setPathBase(Supplier<Maybe<java.nio.file.Path>> base) { pathBase = base; }

    /** Commits only on Enter, focus loss or an explicit control action, never on typing. */
    public void addCommitListener(Runnable listener) { commitListeners.add(listener); }

    private void committed() {
        if (!updating && validation.isValid()) commitListeners.forEach(Runnable::run);
    }

    private void bindCommits(java.awt.Component component) {
        if (component instanceof JSpinner spinner) {
            spinner.addChangeListener(e -> committed());
        } else if (component instanceof JTextField field) {
            field.addActionListener(e -> committed());
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusLost(java.awt.event.FocusEvent e) { committed(); }
            });
        } else if (component instanceof JCheckBox button) {
            button.addActionListener(e -> committed());
        } else if (component instanceof JComboBox<?> box) {
            box.addActionListener(e -> committed());
            return;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) bindCommits(child);
        }
    }

    public boolean isEditing() {
        java.awt.Component focus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focus != null && SwingUtilities.isDescendingFrom(focus, this);
    }

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
        updating = true;
        dispose();
        removeAll();

        JComponent editor = switch (type) {
            case INT -> intEditor(value instanceof Integer i ? i : 0);
            case FLOAT -> floatEditor(value instanceof Float f ? f : 0f);
            case BOOL -> boolEditor(value instanceof Boolean b && b);
            case STRING -> textEditor(value instanceof String s ? s : "");
            case REL_PATH -> pathEditor(value instanceof String s ? s : "");
            case VEC3 -> vec3Editor(value instanceof Vec3 v ? v : Vec3.zero());
            case NODE_REF -> nodes.isSome() ? nodeEditor(nodes.unwrap().get(), value) : noDefault();
            case COLOR -> colorEditor(value instanceof Color c ? c : Color.BLACK);
        };

        editor.setFont(style.uiFont());
        bindCommits(editor);
        add(editor, BorderLayout.CENTER);
        revalidate();
        repaint();
        updating = false;
    }

    public Object value() {
        if (!validation.isValid()) throw new IllegalArgumentException("Invalid parameter value");
        Object result = reader.get();
        if (result instanceof Float f && !Float.isFinite(f)
            || result instanceof Vec3 v && (!Float.isFinite(v.getX()) || !Float.isFinite(v.getY()) || !Float.isFinite(v.getZ()))) {
            throw new IllegalArgumentException("Values must be finite");
        }
        return result;
    }

    public void dispose() {
        vectorValidators.forEach(validation::remove);
        vectorValidators.clear();
    }

    private void commitSpinner(JSpinner spinner) {
        boolean previous = updating;
        updating = true;
        try { spinner.commitEdit(); }
        catch (java.text.ParseException e) { throw new IllegalArgumentException("Invalid number", e); }
        finally { updating = previous; }
    }

    private JComponent intEditor(int value) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setFont(style.techFont());
        reader = () -> { commitSpinner(spinner); return ((Number) spinner.getValue()).intValue(); };

        return spinner;
    }

    private JComponent floatEditor(float value) {
        JSpinner spinner = new JSpinner(
            new SpinnerNumberModel((double) value, -(double) Float.MAX_VALUE, (double) Float.MAX_VALUE, 0.1)
        );
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setFont(style.techFont());
        reader = () -> { commitSpinner(spinner); return ((Number) spinner.getValue()).floatValue(); };

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

    private JComponent pathEditor(String value) {
        JTextField field = new JTextField(value);
        reader = field::getText;
        if (pathBase.get().isNone()) return field;
        JPanel row = new JPanel(new BorderLayout(4, 0));
        JButton browse = new JButton("…");
        browse.setFont(style.uiFont());
        browse.setToolTipText("Choose a file (stored relative to the scene)");
        browse.addActionListener(e -> pathBase.get().ifSome(base ->
            new org.halalfoursome.kebabeditor.utils.FileChooser(
                org.halalfoursome.kebabeditor.utils.FileChooser.FileChooserMode.FILES)
                .startIn(base).open(this, file -> {
                    field.setText(base.relativize(file.toPath().toAbsolutePath().normalize()).toString().replace('\\', '/'));
                    committed();
                })
        ));
        row.add(field, BorderLayout.CENTER);
        row.add(browse, BorderLayout.EAST);
        return row;
    }

    private JComponent vec3Editor(Vec3 value) {
        JPanel row = new JPanel(new java.awt.GridLayout(1, 3, 4, 0));
        JTextField[] fields = new JTextField[3];
        float[] values = {value.getX(), value.getY(), value.getZ()};
        for (int i=0; i<3; i++) {
            JPanel axis = new JPanel(new BorderLayout(2, 0));
            JLabel label = new JLabel(new String[]{"X", "Y", "Z"}[i]);
            label.setFont(style.uiFont());
            fields[i] = new JTextField(Float.toString(values[i]));
            fields[i].setFont(style.techFont());
            vectorValidators.add(validation.add(fields[i], text -> {
                try {
                    return Float.isFinite(Float.parseFloat(text.trim())) ? Maybe.none() : Maybe.some("Enter a finite number");
                } catch (NumberFormatException e) { return Maybe.some("Enter a finite number"); }
            }));
            axis.add(label, BorderLayout.WEST);
            axis.add(fields[i], BorderLayout.CENTER);
            row.add(axis);
        }
        reader = () -> {
            try {
                return new Vec3(Float.parseFloat(fields[0].getText().trim()),
                    Float.parseFloat(fields[1].getText().trim()), Float.parseFloat(fields[2].getText().trim()));
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid vector", e); }
        };
        return row;
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
                committed();
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
