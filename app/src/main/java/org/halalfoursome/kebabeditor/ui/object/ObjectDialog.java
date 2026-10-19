package org.halalfoursome.kebabeditor.ui.object;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class ObjectDialog extends JDialog {

    public ObjectDialog(JFrame owner, SceneNode node) {
        super(owner, "Object", true);

        KebabStyle style = KebabStyle.getCurrent();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel content = new JPanel(new BorderLayout(8, 0));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        JLabel nameLabel = new JLabel("Name");
        nameLabel.setFont(style.uiFont());
        content.add(nameLabel, BorderLayout.WEST);

        JTextField nameField = new JTextField(Objects.toString(node.getName(), ""), 24);
        nameField.setFont(style.uiFont());
        nameField.setEditable(false);
        content.add(nameField, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.setFont(style.uiFont());
        closeButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.add(closeButton);

        add(content, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        getRootPane().setDefaultButton(closeButton);

        pack();
        setLocationRelativeTo(owner);
    }
}
