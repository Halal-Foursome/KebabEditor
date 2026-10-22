package org.halalfoursome.kebabeditor.ui.inspector;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.instance.*;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.*;

/** Event-driven, headless-testable inspector. Construct and attach on the EDT. */
public final class InspectorPane extends JPanel implements AutoCloseable {
    private final Editor editor;
    private static final class Content extends JPanel implements Scrollable {
        Content() { super(new BorderLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return visible.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private final JPanel content = new Content();
    private final List<ArchetypeSection> sections = new ArrayList<>();
    private final AtomicBoolean scheduled = new AtomicBoolean();
    private final Runnable listener = this::scheduleRefresh;
    private SceneNode node;
    private JTextField name;
    private String displayedName;
    private TransformEditor transform;
    private record SectionState(NodeArchetype instance, String title, Object icon, Map<String, ?> params) {}
    private List<SectionState> structure = List.of();
    private boolean attached;
    private boolean closed;
    private JButton add;

    public InspectorPane(Editor editor) {
        super(new BorderLayout());
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Create InspectorPane on the EDT");
        this.editor = editor;
        setPreferredSize(new Dimension(350, 600));
        setMinimumSize(new Dimension(240, 100));
        JLabel title = new JLabel("Inspector");
        title.setFont(KebabStyle.getCurrent().uiFont());
        title.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(title, BorderLayout.NORTH);
        content.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        attach();
        refresh();
    }

    private void attach() {
        if (attached || closed) return;
        editor.selection().addListener(listener);
        editor.addSceneListener(listener);
        attached = true;
    }

    @Override public void addNotify() { super.addNotify(); attach(); refresh(); }
    @Override public void removeNotify() { detach(); super.removeNotify(); }
    private void detach() {
        editor.selection().removeListener(listener);
        editor.removeSceneListener(listener);
        attached = false;
    }
    @Override public void close() { closed = true; detach(); sections.forEach(ArchetypeSection::dispose); }

    private void scheduleRefresh() {
        if (scheduled.compareAndSet(false, true)) SwingUtilities.invokeLater(() -> {
            scheduled.set(false);
            if (attached && !closed) refresh();
        });
    }

    private List<SectionState> structureOf(SceneNode selected) {
        return selected.getArchetypes().stream().map(a -> a instanceof ArchetypeInstance i
            ? new SectionState(a, i.getDefinition().getDisplayName(), i.getDefinition().getIcon(),
                Map.copyOf(i.getDefinition().getParams()))
            : new SectionState(a, a.archetypeId(), null, Map.of())).toList();
    }

    private void refresh() {
        SceneNode selected = editor.selection().primary().unwrapOr(null);
        if (selected != null && !editor.sceneNodes().contains(selected)) selected = null;
        List<SectionState> nextStructure = selected == null ? List.of() : structureOf(selected);
        if (selected != node || !structure.equals(nextStructure) || content.getComponentCount() == 0) {
            sections.forEach(ArchetypeSection::dispose);
            sections.clear();
            node = selected;
            structure = nextStructure;
            content.removeAll();
            if (node == null) {
                JLabel hint = new JLabel("Select an object to inspect");
                hint.setFont(KebabStyle.getCurrent().uiFont());
                content.add(hint, BorderLayout.NORTH);
                name = null;
                transform = null;
            } else build();
            content.revalidate();
            content.repaint();
        }
        if (node != null) {
            if (!name.hasFocus()) {
                displayedName = Objects.toString(node.getName(), "");
                name.setText(displayedName);
            }
            transform.refresh();
            sections.forEach(ArchetypeSection::refresh);
            add.setEnabled(!editor.availableArchetypes(node).isEmpty());
        }
    }

    private void build() {
        SceneNode target = node;
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.add(new JLabel(LucideIcon.COMPONENT.icon(18, UIManager.getColor("Label.foreground"))), BorderLayout.WEST);
        name = new JTextField(Objects.toString(target.getName(), ""));
        name.setName("object.name");
        name.setFont(KebabStyle.getCurrent().uiFont());
        JTextField field = name;
        Runnable commit = () -> {
            if (editor.selection().primary().unwrapOr(null) != target || !editor.sceneNodes().contains(target)) return;
            if (field.getText().equals(displayedName)) { scheduleRefresh(); return; }
            String value = field.getText().trim();
            if (!value.isEmpty() && !value.equals(target.getName())) editor.renameNode(target, value);
            displayedName = Objects.toString(target.getName(), "");
            field.setText(displayedName);
        };
        name.addActionListener(e -> commit.run());
        name.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commit.run(); }
        });
        header.add(name, BorderLayout.CENTER);
        stack.add(header);
        transform = new TransformEditor(editor, target);
        stack.add(transform);
        for (NodeArchetype instance : target.getArchetypes()) {
            ArchetypeSection section = new ArchetypeSection(editor, target, instance);
            sections.add(section);
            stack.add(section);
        }
        add = new JButton("Add Archetype");
        add.setFont(KebabStyle.getCurrent().uiFont());
        add.setAlignmentX(Component.CENTER_ALIGNMENT);
        add.addActionListener(e -> {
            if (editor.selection().primary().unwrapOr(null) != target) return;
            JPopupMenu menu = new JPopupMenu();
            for (ArchetypeDefinition definition : editor.availableArchetypes(target)) {
                JMenuItem item = new JMenuItem(definition.getDisplayName());
                item.setToolTipText(definition.getId());
                item.setFont(KebabStyle.getCurrent().uiFont());
                item.addActionListener(event -> {
                    if (editor.selection().primary().unwrapOr(null) != target || !editor.sceneNodes().contains(target)) return;
                    try { editor.attachArchetype(target, new ArchetypeInstance(definition)); }
                    catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot add archetype", JOptionPane.ERROR_MESSAGE);
                    }
                });
                menu.add(item);
            }
            menu.show(add, 0, add.getHeight());
        });
        stack.add(add);
        // BorderLayout sizes the stack to the viewport width, including narrow sidebars.
        content.add(stack, BorderLayout.NORTH);
    }
}
