package org.halalfoursome.kebabeditor.ui.inspector;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.math.*;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

/** Edits local TRS, keeping the authoritative quaternion and uniform scale. */
public final class TransformEditor extends InspectorSection {
    private final Editor editor;
    private final SceneNode node;
    private final JTextField[] fields = new JTextField[7];
    private final String[] displayed = new String[7];
    private Quat displayedRotation;
    private Vec3 angles;

    public TransformEditor(Editor editor, SceneNode node) {
        super("Transform", null);
        this.editor = editor;
        this.node = node;
        body.setLayout(new GridLayout(0, 1, 0, 4));
        for (int row=0; row<3; row++) {
            JPanel group = new JPanel(new BorderLayout(0, 2));
            JLabel label = new JLabel(new String[]{"Position", "Rotation (degrees)", "Scale"}[row]);
            label.setFont(KebabStyle.getCurrent().uiFont());
            group.add(label, BorderLayout.NORTH);
            JPanel axes = new JPanel(new GridLayout(1, row==2 ? 1 : 3, 4, 0));
            for (int axis=0; axis<(row==2 ? 1 : 3); axis++) {
                int index = row*3+axis;
                JTextField field = new JTextField();
                fields[index] = field;
                field.setName(new String[]{"position.x","position.y","position.z","rotation.x","rotation.y","rotation.z","scale"}[index]);
                field.setFont(KebabStyle.getCurrent().techFont());
                field.addActionListener(e -> commit(index));
                field.addFocusListener(new FocusAdapter() {
                    @Override public void focusLost(FocusEvent e) { commit(index); }
                });
                JPanel pair = new JPanel(new BorderLayout(3, 0));
                if (row!=2) {
                    JLabel axisLabel = new JLabel(new String[]{"X", "Y", "Z"}[axis]);
                    axisLabel.setFont(KebabStyle.getCurrent().uiFont());
                    pair.add(axisLabel, BorderLayout.WEST);
                }
                pair.add(field, BorderLayout.CENTER);
                axes.add(pair);
            }
            group.add(axes, BorderLayout.CENTER);
            body.add(group);
        }
        refresh();
    }

    private static float component(Vec3 v, int axis) {
        return switch(axis) { case 0 -> v.getX(); case 1 -> v.getY(); default -> v.getZ(); };
    }

    private static void component(Vec3 v, int axis, float value) {
        switch(axis) { case 0 -> v.setX(value); case 1 -> v.setY(value); default -> v.setZ(value); }
    }

    public void refresh() {
        Transform t = node.getTransform();
        if (!t.getRotation().equals(displayedRotation)) {
            displayedRotation = t.getRotation().copy();
            angles = t.getRotation().toEuler().mulLocal((float)(180 / Math.PI));
        }
        for (int i=0; i<7; i++) {
            if (fields[i].hasFocus()) continue;
            float value = i<3 ? component(t.getTranslation(),i) : i<6 ? component(angles,i-3) : t.getScale();
            displayed[i] = Float.toString(value == 0 ? 0 : value);
            fields[i].setText(displayed[i]);
        }
    }

    private void commit(int index) {
        if (editor.isPlaytesting() || editor.selection().primary().unwrapOr(null) != node || !editor.containsNode(node)) return;
        JTextField field = fields[index];
        if (field.getText().equals(displayed[index])) return; // Never round-trip an untouched quaternion.
        try {
            float value = Float.parseFloat(field.getText().trim());
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Enter a finite number");
            Transform next = node.getTransform().copy();
            if (index<3) component(next.getTranslation(),index,value);
            else if (index<6) {
                Vec3 current = next.getRotation().equals(displayedRotation) ? angles.copy()
                    : next.getRotation().toEuler().mulLocal((float)(180 / Math.PI));
                component(current,index-3,value);
                // Reduce before conversion so even very large finite degree input stays finite.
                next.setRotation(Quat.fromEuler((float)Math.toRadians(current.getX()%360),
                    (float)Math.toRadians(current.getY()%360), (float)Math.toRadians(current.getZ()%360)));
                angles = current;
                displayedRotation = next.getRotation().copy();
            } else next.setScale(value);
            editor.updateTransform(node,next);
            displayed[index] = field.getText();
            field.putClientProperty("JComponent.outline", null);
            field.setToolTipText(null);
            refresh();
        } catch (IllegalArgumentException ex) {
            field.putClientProperty("JComponent.outline", "error");
            field.setToolTipText("Enter a finite number; the previous value is preserved");
        }
    }
}
