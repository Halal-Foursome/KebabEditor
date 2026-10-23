package org.halalfoursome.kebabeditor.ui.object;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.halalfoursome.kebabeditor.archetype.ParamSpec;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.error.InvalidParamType;
import org.halalfoursome.kebabeditor.archetype.error.UnknownParamException;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.editor.InitialValues;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.ui.archetype.ParamValueEditor;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class ArchetypeInstanceDialog extends JDialog {

    private final Editor editor;
    private final SceneNode node;
    private final Maybe<ArchetypeInstance> editing;
    private final KebabStyle style = KebabStyle.getCurrent();
    private final ValidationGroup validation = new ValidationGroup();

    private final JComboBox<ArchetypeDefinition> archetypeBox = new JComboBox<>();
    private final JPanel parameters = new JPanel(new GridBagLayout());
    private final Map<String, ParamValueEditor> valueEditors = new LinkedHashMap<>();
    private final JButton saveButton = new JButton("Save");

    public ArchetypeInstanceDialog(JDialog owner, Editor editor, SceneNode node) {
        this(owner, editor, node, Maybe.none());
    }

    public ArchetypeInstanceDialog(JDialog owner, Editor editor, SceneNode node, ArchetypeInstance instance) {
        this(owner, editor, node, Maybe.some(instance));
    }

    private ArchetypeInstanceDialog(JDialog owner, Editor editor, SceneNode node, Maybe<ArchetypeInstance> editing) {
        super(owner, editing.isSome() ? "Edit archetype" : "Add archetype", true);
        this.editor = editor;
        this.node = node;
        this.editing = editing;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(560, 420));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                validation.hidePopups();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                validation.hidePopups();
            }
        });
        validation.addListener(this::updateSaveEnabled);

        fillArchetypes();

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        content.add(buildArchetypeRow(), BorderLayout.NORTH);
        content.add(buildParameters(), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        showParameters();
        pack();
        setLocationRelativeTo(owner);
    }

    private void fillArchetypes() {
        if (editing.isSome()) {
            archetypeBox.addItem(editing.unwrap().getDefinition());
            archetypeBox.setEnabled(false);

            return;
        }

        editor.availableArchetypes(node).forEach(archetypeBox::addItem);
        archetypeBox.addActionListener(e -> showParameters());
    }

    private JPanel buildArchetypeRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0));

        JLabel label = new JLabel("Archetype");
        label.setFont(style.uiFont());
        row.add(label, BorderLayout.WEST);

        archetypeBox.setFont(style.uiFont());
        archetypeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focus
            ) {
                ArchetypeDefinition definition = (ArchetypeDefinition) value;
                String text = definition == null
                    ? "Nothing left to add"
                    : definition.getDisplayName() + "  (" + definition.getId() + ")";

                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        row.add(archetypeBox, BorderLayout.CENTER);

        return row;
    }

    private JScrollPane buildParameters() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(parameters, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setPreferredSize(new Dimension(520, 280));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    private JPanel buildFooter() {
        saveButton.setFont(style.uiFont());
        saveButton.addActionListener(e -> save());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(style.uiFont());
        cancelButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.add(saveButton);
        footer.add(cancelButton);

        return footer;
    }

    private void showParameters() {
        valueEditors.values().forEach(ParamValueEditor::dispose);
        valueEditors.clear();
        parameters.removeAll();

        if (archetypeBox.getSelectedItem() instanceof ArchetypeDefinition definition) {
            addParameterRows(definition);
        } else {
            parameters.add(label("Every archetype that can only be added once is already on this object."), rowAt(0, 0));
        }

        parameters.revalidate();
        parameters.repaint();
        updateSaveEnabled();
    }

    private void addParameterRows(ArchetypeDefinition definition) {
        if (definition.getParams().isEmpty()) {
            parameters.add(label("This archetype has no parameters."), rowAt(0, 0));

            return;
        }

        int row = 0;

        for (Map.Entry<String, ParamSpec> entry : definition.getParams().entrySet()) {
            ParamSpec spec = entry.getValue();

            GridBagConstraints labelConstraints = rowAt(row, 0);
            parameters.add(label(entry.getKey() + "  (" + spec.type().displayName() + ")"), labelConstraints);

            ParamValueEditor valueEditor = new ParamValueEditor(style, validation, editor::sceneNodes);
            valueEditor.showFor(spec.type(), initialValue(definition, entry.getKey(), spec));
            GridBagConstraints editorConstraints = rowAt(row, 1);
            editorConstraints.weightx = 1.0;
            editorConstraints.fill = GridBagConstraints.HORIZONTAL;
            parameters.add(valueEditor, editorConstraints);

            valueEditors.put(entry.getKey(), valueEditor);
            row++;
        }
    }

    private Object initialValue(ArchetypeDefinition definition, String name, ParamSpec spec) {
        if (editing.isNone()) {
            return InitialValues.forParam(node, definition, name, spec);
        }

        try {
            return editing.unwrap().get(name);
        } catch (UnknownParamException e) {
            return spec.defaultValue();
        }
    }

    private void save() {
        ArchetypeDefinition definition = (ArchetypeDefinition) archetypeBox.getSelectedItem();

        try {
            if (editing.isSome()) {
                apply(editing.unwrap());
            } else {
                ArchetypeInstance created = new ArchetypeInstance(definition);
                apply(created);
                editor.attachArchetype(node, created);
            }

            dispose();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot add archetype", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void apply(ArchetypeInstance target) {
        for (Map.Entry<String, ParamValueEditor> entry : valueEditors.entrySet()) {
            try {
                target.set(entry.getKey(), entry.getValue().value());
            } catch (UnknownParamException | InvalidParamType e) {
                throw new IllegalStateException("Cannot set " + target.archetypeId() + "." + entry.getKey(), e);
            }
        }
    }

    private void updateSaveEnabled() {
        saveButton.setEnabled(archetypeBox.getSelectedItem() != null && validation.isValid());
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(style.uiFont());

        return label;
    }

    private static GridBagConstraints rowAt(int row, int column) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 8, 4, 8);

        return constraints;
    }
}
