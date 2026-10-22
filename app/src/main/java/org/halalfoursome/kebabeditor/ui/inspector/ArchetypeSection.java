package org.halalfoursome.kebabeditor.ui.inspector;

import java.awt.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.halalfoursome.kebabeditor.archetype.*;
import org.halalfoursome.kebabeditor.archetype.instance.*;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.ui.archetype.ParamValueEditor;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

final class ArchetypeSection extends InspectorSection {
    private final Editor editor;
    private final SceneNode node;
    private final NodeArchetype archetype;
    private final Map<String, ParamValueEditor> controls = new LinkedHashMap<>();
    private final Map<String, Object> shown = new HashMap<>();
    private final ValidationGroup validation = new ValidationGroup();
    private String nodeLabels = "";

    ArchetypeSection(Editor editor, SceneNode node, NodeArchetype archetype) {
        super(archetype instanceof ArchetypeInstance i ? i.getDefinition().getDisplayName()
            : archetype.archetypeId() + " (unresolved)", icon(archetype));
        this.editor = editor;
        this.node = node;
        this.archetype = archetype;
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        if (archetype instanceof ArchetypeInstance instance) {
            for (var entry : instance.getDefinition().getParams().entrySet()) {
                String name = entry.getKey();
                ParamSpec spec = entry.getValue();
                JPanel row = new JPanel(new BorderLayout(0, 3));
                JLabel label = new JLabel(name + " (" + spec.type().displayName() + ")");
                label.setFont(KebabStyle.getCurrent().uiFont());
                row.add(label, BorderLayout.NORTH);
                ParamValueEditor control = new ParamValueEditor(KebabStyle.getCurrent(), validation, editor::sceneNodes);
                control.setName("param." + name);
                control.setPathBase(() -> editor.currentProjectFile().map(file -> file.toAbsolutePath().getParent()));
                control.addCommitListener(() -> {
                    if (editor.selection().primary().unwrapOr(null) != node
                        || !editor.sceneNodes().contains(node) || !node.getArchetypes().contains(instance)) return;
                    try {
                        Object value = control.value();
                        if (!Objects.equals(value, shown.get(name))) editor.setArchetypeParam(instance, name, value);
                        else editor.sceneChanged(); // A deferred external update can now replace untouched input.
                        control.putClientProperty("JComponent.outline", null);
                        control.setToolTipText(null);
                    } catch (IllegalArgumentException e) {
                        control.putClientProperty("JComponent.outline", "error");
                        control.setToolTipText(e.getMessage());
                    }
                });
                controls.put(name, control);
                row.add(control, BorderLayout.CENTER);
                row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
                body.add(row);
            }
        } else if (archetype instanceof UnresolvedArchetype unresolved) {
            JTextArea raw = new JTextArea(unresolved.rawJson().toPrettyString());
            raw.setEditable(false);
            raw.setLineWrap(true);
            raw.setWrapStyleWord(true);
            raw.setFont(KebabStyle.getCurrent().techFont());
            body.add(raw);
        }
        JButton remove = new JButton("Remove archetype");
        remove.setFont(KebabStyle.getCurrent().uiFont());
        remove.addActionListener(e -> {
            if (editor.selection().primary().unwrapOr(null) != node) return;
            if (JOptionPane.showConfirmDialog(this, "Remove this archetype from the object?",
                "Remove archetype", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                editor.detachArchetype(node, archetype);
            }
        });
        body.add(remove);
        refresh();
    }

    void refresh() {
        if (!(archetype instanceof ArchetypeInstance instance)) return;
        String labels = editor.sceneNodes().stream().map(n -> n.getId() + ":" + n.getName()).toList().toString();
        boolean referencesChanged = !labels.equals(nodeLabels);
        nodeLabels = labels;
        for (var entry : controls.entrySet()) {
            String name = entry.getKey();
            ParamValueEditor control = entry.getValue();
            Object value = read(instance, name);
            ParamType type = instance.getDefinition().getParams().get(name).type();
            if (!control.isEditing() && (!shown.containsKey(name) || !Objects.equals(shown.get(name), value)
                || referencesChanged && type == ParamType.NODE_REF)) {
                control.showFor(type, value);
                shown.put(name, value instanceof Vec3 v ? v.copy() : value);
            }
        }
    }

    private static Object read(ArchetypeInstance instance, String name) {
        try { return instance.get(name); }
        catch (org.halalfoursome.kebabeditor.archetype.error.UnknownParamException e) {
            throw new IllegalStateException("Parameter vanished from its definition", e);
        }
    }

    void dispose() {
        controls.values().forEach(ParamValueEditor::dispose);
        validation.hidePopups();
    }

    private static Icon icon(NodeArchetype archetype) {
        if (!(archetype instanceof ArchetypeInstance instance) || instance.getDefinition().getIcon().isNone()) return null;
        return switch (instance.getDefinition().getIcon().unwrap()) {
            case ArchetypeIcon.Bundled b -> new FlatSVGIcon(b.resource(), 18, 18);
            case ArchetypeIcon.Loaded l -> new ImageIcon(l.icon().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
            case ArchetypeIcon.Handle h -> h.path().toLowerCase(Locale.ROOT).endsWith(".svg")
                ? new FlatSVGIcon(new File(h.path())).derive(18, 18)
                : h.loaded().map(l -> (Icon)new ImageIcon(l.icon().getScaledInstance(18, 18, Image.SCALE_SMOOTH))).unwrapOr(null);
        };
    }
}
