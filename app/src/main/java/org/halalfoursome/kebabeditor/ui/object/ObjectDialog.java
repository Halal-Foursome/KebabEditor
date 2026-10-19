package org.halalfoursome.kebabeditor.ui.object;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.archetype.instance.UnresolvedArchetype;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class ObjectDialog extends JDialog {

    private static final Color DUPLICATE_COLOR = new Color(235, 100, 100);

    private final Editor editor;
    private final SceneNode node;
    private final KebabStyle style = KebabStyle.getCurrent();

    private final DefaultListModel<NodeArchetype> model = new DefaultListModel<>();
    private final JList<NodeArchetype> list = new JList<>(model);
    private final JButton addButton = new JButton("Add...");
    private final JButton editButton = new JButton("Edit...");
    private final JButton removeButton = new JButton("Remove");
    private final JTextField nameField = new JTextField(24);
    private boolean discardName;

    public ObjectDialog(JFrame owner, Editor editor, SceneNode node) {
        super(owner, "Object", true);
        this.editor = editor;
        this.node = node;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(480, 380));

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        content.add(buildNameRow(), BorderLayout.NORTH);
        content.add(buildArchetypes(), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                commitName();
            }
        });

        getRootPane().registerKeyboardAction(
            e -> {
                discardName = true;
                dispose();
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        reload();
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildNameRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0));

        JLabel nameLabel = new JLabel("Name");
        nameLabel.setFont(style.uiFont());
        row.add(nameLabel, BorderLayout.WEST);

        nameField.setText(Objects.toString(node.getName(), ""));
        nameField.setFont(style.uiFont());
        nameField.addActionListener(e -> commitName());
        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commitName();
            }
        });
        row.add(nameField, BorderLayout.CENTER);

        return row;
    }

    private JPanel buildArchetypes() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));

        JLabel title = new JLabel("Archetypes");
        title.setFont(style.uiFont());
        panel.add(title, BorderLayout.NORTH);

        list.setFont(style.uiFont());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focus
            ) {
                NodeArchetype archetype = (NodeArchetype) value;
                super.getListCellRendererComponent(list, describe(archetype), index, selected, focus);

                long copies = instancesOf(archetype);
                if (isSingleInstance(archetype) && copies > 1) {
                    setForeground(DUPLICATE_COLOR);
                    setToolTipText(describe(archetype) + " is marked single, but has " + copies + " instances");
                } else {
                    setToolTipText(null);
                }

                return this;
            }
        });
        ToolTipManager.sharedInstance().registerComponent(list);
        list.addListSelectionListener(e -> updateButtons());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && list.locationToIndex(e.getPoint()) >= 0) {
                    editSelected();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(300, 200));
        panel.add(scroll, BorderLayout.CENTER);

        for (JButton button : new JButton[] { addButton, editButton, removeButton }) {
            button.setFont(style.uiFont());
        }
        addButton.addActionListener(e -> addArchetype());
        editButton.addActionListener(e -> editSelected());
        removeButton.addActionListener(e -> removeSelected());

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(removeButton);

        JPanel buttonColumn = new JPanel(new BorderLayout());
        buttonColumn.add(buttons, BorderLayout.NORTH);
        panel.add(buttonColumn, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildFooter() {
        JButton closeButton = new JButton("Close");
        closeButton.setFont(style.uiFont());
        closeButton.addActionListener(e -> {
            commitName();
            dispose();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.add(closeButton);

        return footer;
    }

    private void commitName() {
        if (discardName) {
            return;
        }

        String current = Objects.toString(node.getName(), "");
        String text = nameField.getText().trim();

        if (text.isEmpty() || text.equals(current)) {
            nameField.setText(current);
            return;
        }

        editor.renameNode(node, text);
    }

    private void addArchetype() {
        new ArchetypeInstanceDialog(this, editor, node).setVisible(true);
        reload();
    }

    private void editSelected() {
        if (list.getSelectedValue() instanceof ArchetypeInstance instance) {
            new ArchetypeInstanceDialog(this, editor, node, instance).setVisible(true);
            list.repaint();
        }
    }

    private void removeSelected() {
        NodeArchetype selected = list.getSelectedValue();

        if (selected == null) {
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
            this,
            "Remove " + describe(selected) + " from this object?",
            "Remove archetype",
            JOptionPane.YES_NO_OPTION
        );

        if (answer == JOptionPane.YES_OPTION) {
            editor.detachArchetype(node, selected);
            reload();
        }
    }

    private void reload() {
        int selectedIndex = list.getSelectedIndex();

        model.clear();
        model.addAll(node.getArchetypes());

        if (!model.isEmpty()) {
            list.setSelectedIndex(Math.min(Math.max(selectedIndex, 0), model.size() - 1));
        }

        updateButtons();
    }

    private void updateButtons() {
        NodeArchetype selected = list.getSelectedValue();

        addButton.setEnabled(!editor.availableArchetypes(node).isEmpty());
        editButton.setEnabled(selected instanceof ArchetypeInstance);
        removeButton.setEnabled(selected != null);
    }

    private long instancesOf(NodeArchetype archetype) {
        return node.getArchetypes().stream()
            .filter(other -> other.archetypeId().equals(archetype.archetypeId()))
            .count();
    }

    private static boolean isSingleInstance(NodeArchetype archetype) {
        return archetype instanceof ArchetypeInstance instance && instance.getDefinition().isSingle();
    }

    private static String describe(NodeArchetype archetype) {
        return switch (archetype) {
            case ArchetypeInstance instance -> instance.getDefinition().getDisplayName();
            case UnresolvedArchetype unresolved -> unresolved.id() + " (unresolved)";
        };
    }
}
