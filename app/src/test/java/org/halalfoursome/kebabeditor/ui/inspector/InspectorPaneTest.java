package org.halalfoursome.kebabeditor.ui.inspector;

import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.halalfoursome.kebabeditor.archetype.*;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.instance.*;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.math.*;
import org.halalfoursome.kebabeditor.project.EditorSessions;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.ui.archetype.ParamValueEditor;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class InspectorPaneTest {
    @TempDir Path dir;
    Editor editor;
    InspectorPane pane;
    SceneNode node;

    @BeforeEach void setup() throws Exception {
        editor = EditorSessions.create();
        editor.createProject(dir, "scene");
        SwingUtilities.invokeAndWait(() -> {
            KebabStyle.setCurrent(KebabStyle.defaultStyle());
            pane = new InspectorPane(editor);
            node = editor.spawnEmpty(Vec3.zero());
        });
        flush();
    }
    @AfterEach void close() throws Exception { SwingUtilities.invokeAndWait(pane::close); }
    void flush() throws Exception { SwingUtilities.invokeAndWait(() -> {}); SwingUtilities.invokeAndWait(() -> {}); }
    static List<Component> all(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            result.add(child);
            if (child instanceof Container c) result.addAll(all(c));
        }
        return result;
    }
    JTextField field(String name) {
        return all(pane).stream().filter(c -> name.equals(c.getName())).map(JTextField.class::cast).findFirst().orElseThrow();
    }
    void enter(String name, String value) {
        JTextField field = field(name);
        field.setText(value);
        field.postActionEvent();
    }
    boolean placeholder() {
        return all(pane).stream().anyMatch(c -> c instanceof JLabel l && l.getText()!=null && l.getText().contains("Select an object"));
    }

    @Test void selectionClearRemovalAndProjectCloseRefreshWithoutPolling() throws Exception {
        SwingUtilities.invokeAndWait(() -> assertEquals("Empty", field("object.name").getText()));
        SwingUtilities.invokeAndWait(() -> editor.selection().clear());
        flush();
        SwingUtilities.invokeAndWait(() -> assertTrue(placeholder()));
        SceneNode[] second = {null};
        SwingUtilities.invokeAndWait(() -> second[0] = editor.spawnEmpty(Vec3.unitX()));
        flush();
        SwingUtilities.invokeAndWait(() -> assertEquals("1.0", field("position.x").getText()));
        editor.scene().unwrap().remove(second[0]); // Direct Scene removal is observable as well.
        flush();
        SwingUtilities.invokeAndWait(() -> assertTrue(placeholder()));
        SwingUtilities.invokeAndWait(() -> editor.selection().select(node));
        flush();
        editor.closeProject();
        flush();
        SwingUtilities.invokeAndWait(() -> assertTrue(placeholder()));
    }

    @Test void commitsNameAndEachTransformComponentAndRejectsInvalidInput() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            enter("object.name", "  Lamp  ");
            enter("position.x", "12.25");
            enter("position.y", "-3");
            enter("rotation.y", "90");
            enter("scale", "2.5");
            assertEquals("Lamp", node.getName());
            assertEquals(new Vec3(12.25f,-3,0), node.getTransform().getTranslation());
            assertEquals(2.5f, node.getTransform().getScale());
            Vec3 rotated = node.getTransform().getRotation().rotate(Vec3.unitZ());
            assertEquals(1,rotated.getX(),1e-5);
            assertEquals(0,rotated.getZ(),1e-5);
            Transform before = node.getTransform().copy();
            for (String invalid : List.of("NaN", "Infinity", "-Infinity", "bad", "1e99", "")) {
                enter("position.z", invalid);
                enter("rotation.x", invalid);
                enter("scale", invalid);
                assertEquals(before, node.getTransform());
            }
        });
    }

    @Test void externalRefreshNeverRoundTripsQuaternionOrWritesBack() throws Exception {
        Quat rotation = Quat.fromEuler(.45f,1.15f,-.8f);
        SwingUtilities.invokeAndWait(() -> {
            node.getTransform().setRotation(rotation);
            node.getTransform().setTranslation(new Vec3(2,3,4));
            editor.sceneChanged(); // Same notification emitted by gizmo drag/cancel.
        });
        flush();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("2.0",field("position.x").getText());
            assertSame(rotation,node.getTransform().getRotation());
            field("rotation.x").postActionEvent();
            assertSame(rotation,node.getTransform().getRotation());
            enter("position.y","8");
            assertEquals(rotation,node.getTransform().getRotation());
            assertEquals(2,node.getTransform().getTranslation().getX());
        });
    }

    @Test void allParameterEditorsUpdateStoredValuesAndPersistWithTransform() throws Exception {
        ArchetypeDefinition def = new ArchetypeDefinition("test_inspector").withDisplayName("Test").withSingle();
        for (ParamType type : ParamType.values()) def.withParam(type.name(), type);
        editor.addArchetype(def);
        ArchetypeInstance instance = new ArchetypeInstance(def);
        SwingUtilities.invokeAndWait(() -> editor.attachArchetype(node, instance));
        flush();
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(editor.availableArchetypes(node).contains(def));
            assertThrows(IllegalArgumentException.class, () -> editor.attachArchetype(node,new ArchetypeInstance(def)));
            for (ParamType type : ParamType.values()) {
                ParamValueEditor control = all(pane).stream().filter(c -> ("param."+type.name()).equals(c.getName()))
                    .map(ParamValueEditor.class::cast).findFirst().orElseThrow();
                List<Component> children = all(control);
                switch(type) {
                    case INT, FLOAT -> {
                        JSpinner spinner = children.stream().filter(JSpinner.class::isInstance).map(JSpinner.class::cast).findFirst().orElseThrow();
                        if(type==ParamType.INT) spinner.setValue(7); else spinner.setValue(3.5d);
                    }
                    case STRING, REL_PATH -> {
                        JTextField field = children.stream().filter(JTextField.class::isInstance).map(JTextField.class::cast).findFirst().orElseThrow();
                        field.setText(type==ParamType.STRING ? "hello" : "assets/mesh.gltf"); field.postActionEvent();
                    }
                    case VEC3 -> {
                        for (Component child: children) if (child instanceof JTextField f) { f.setText("2"); f.postActionEvent(); }
                    }
                    case BOOL -> children.stream().filter(JCheckBox.class::isInstance).map(JCheckBox.class::cast).findFirst().orElseThrow().doClick();
                    case NODE_REF -> children.stream().filter(JComboBox.class::isInstance).map(JComboBox.class::cast).findFirst().orElseThrow().setSelectedIndex(1);
                    case COLOR -> assertNotNull(children.stream().filter(JButton.class::isInstance).findFirst().orElse(null)); // Chooser is a native modal UI.
                }
            }
            enter("object.name","Saved object");
            enter("position.x","5"); enter("rotation.z","30"); enter("scale","3");
        });
        flush();
        assertEquals(7,instance.get("INT")); assertEquals(3.5f,instance.get("FLOAT"));
        assertEquals(true,instance.get("BOOL")); assertEquals("hello",instance.get("STRING"));
        assertEquals("assets/mesh.gltf",instance.get("REL_PATH")); assertEquals(new Vec3(2,2,2),instance.get("VEC3"));
        assertEquals(org.halalfoursome.kebabeditor.utils.Maybe.some(node.getId().value().toString()),instance.get("NODE_REF"));
        editor.setArchetypeParam(instance,"COLOR",Color.ORANGE);
        Transform saved = node.getTransform().copy();
        editor.saveProject(); editor.openProject(editor.currentProjectFile().unwrap());
        SceneNode loaded = editor.scene().unwrap().traverse().filter(n -> n.getName().equals("Saved object")).findFirst().orElseThrow();
        assertEquals(saved.getTranslation(),loaded.getTransform().getTranslation());
        assertEquals(saved.getScale(),loaded.getTransform().getScale());
        assertEquals(1,Math.abs(saved.getRotation().dot(loaded.getTransform().getRotation())),1e-5);
        ArchetypeInstance loadedInstance=(ArchetypeInstance)loaded.getArchetypes().getFirst();
        for(ParamType type:ParamType.values()) assertEquals(instance.get(type.name()),loadedInstance.get(type.name()));
        SwingUtilities.invokeAndWait(() -> editor.selection().select(loaded)); flush();
        SwingUtilities.invokeAndWait(() -> editor.detachArchetype(loaded,loadedInstance)); flush();
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(loaded.getArchetypes().isEmpty());
            assertTrue(editor.availableArchetypes(loaded).stream().anyMatch(d -> d.getId().equals(def.getId())));
            assertTrue(all(pane).stream().noneMatch(ParamValueEditor.class::isInstance));
        });
    }

    private static final class TestFocus extends DefaultKeyboardFocusManager {
        void focus(Component component) { setGlobalFocusOwner(component); }
    }

    @Test void preservesFocusedInputAndIgnoresLateCommitsFromOldSelection() throws Exception {
        KeyboardFocusManager previous = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        TestFocus focus = new TestFocus();
        try {
            SwingUtilities.invokeAndWait(() -> {
                KeyboardFocusManager.setCurrentKeyboardFocusManager(focus);
                JTextField x = field("position.x");
                focus.focus(x);
                assertTrue(x.hasFocus());
                x.setText("17");
                node.getTransform().setTranslation(new Vec3(9, 8, 7));
                editor.sceneChanged();
            });
            flush();
            SwingUtilities.invokeAndWait(() -> {
                assertEquals("17",field("position.x").getText());
                assertEquals("8.0",field("position.y").getText());
                assertEquals(new Vec3(9,8,7),node.getTransform().getTranslation());
                field("position.x").postActionEvent();
                assertEquals(new Vec3(17,8,7),node.getTransform().getTranslation());
                JTextField oldName = field("object.name");
                SceneNode second = editor.spawnEmpty(Vec3.zero());
                oldName.setText("wrong object"); oldName.postActionEvent();
                assertEquals("Empty",node.getName()); assertEquals("Empty",second.getName());
            });
        } finally { SwingUtilities.invokeAndWait(() -> KeyboardFocusManager.setCurrentKeyboardFocusManager(previous)); }
    }

    private static void layoutTree(Container c) {
        c.doLayout();
        for(Component child:c.getComponents()) if(child instanceof Container nested) layoutTree(nested);
    }

    @Test void layoutsFitSidebarAndUseBothFlatLafThemes() throws Exception {
        SwingUtilities.invokeAndWait(() -> editor.attachArchetype(node, new ArchetypeInstance(
            new ArchetypeDefinition("vector").withDisplayName("Vector test").withParam("offset",ParamType.VEC3))));
        flush();
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel previous = UIManager.getLookAndFeel();
            try {
                for (LookAndFeel theme : List.of(new com.formdev.flatlaf.FlatDarkLaf(),new com.formdev.flatlaf.FlatLightLaf())) {
                    UIManager.setLookAndFeel(theme);
                    SwingUtilities.updateComponentTreeUI(pane);
                    for (int width : new int[]{240,350,500}) {
                        pane.setSize(width,800); layoutTree(pane);
                        for (Component c : all(pane)) if(c instanceof JTextField) {
                            Rectangle bounds = SwingUtilities.convertRectangle(c.getParent(),c.getBounds(),pane);
                            assertTrue(bounds.x >= 0 && bounds.x+bounds.width<=width, "field exceeds sidebar: " + bounds);
                            assertTrue(bounds.width>10, "field must be usable");
                        }
                        if(width==350) {
                            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(width,800,java.awt.image.BufferedImage.TYPE_INT_RGB);
                            Graphics2D graphics=image.createGraphics(); pane.printAll(graphics); graphics.dispose();
                            java.nio.file.Files.createDirectories(Path.of("build/inspector-preview"));
                            javax.imageio.ImageIO.write(image,"png",Path.of("build/inspector-preview/"+theme.getName()+".png").toFile());
                        }
                    }
                }
            } catch(Exception e) { throw new AssertionError(e); }
            finally {
                try { UIManager.setLookAndFeel(previous); SwingUtilities.updateComponentTreeUI(pane); }
                catch(UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
            }
        });
    }

    @Test void unknownArchetypeIsReadOnlyAndClosedPaneUnsubscribes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            node.addArchetype(new UnresolvedArchetype("unknown",new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("kept",42)));
            editor.sceneChanged();
        }); flush();
        SwingUtilities.invokeAndWait(() -> {
            JTextArea raw = all(pane).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast).findFirst().orElseThrow();
            assertFalse(raw.isEditable()); assertTrue(raw.getText().contains("42"));
            pane.close(); editor.selection().clear();
        }); flush();
        SwingUtilities.invokeAndWait(() -> assertFalse(placeholder()));
    }
}
