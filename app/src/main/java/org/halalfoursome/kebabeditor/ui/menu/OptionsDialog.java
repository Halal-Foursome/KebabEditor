package org.halalfoursome.kebabeditor.ui.menu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class OptionsDialog extends JDialog {


    JButton saveButton = new JButton("Save");
    JButton cancelButton = new JButton("Cancel");

    public OptionsDialog(JFrame owner) {
        super(owner, "Options", true);

        KebabStyle style = KebabStyle.getCurrent();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(760, 460));

        add(buildSidebar(style), BorderLayout.WEST);
        add(buildMainPanel(style, owner), BorderLayout.CENTER);
        add(buildFooter(style), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildSidebar(KebabStyle style) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(
            new Dimension(KebabStyle.SIDEBAR_WIDTH, 0)
        );

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JButton blockoutButton = new JButton("Blockout view");
        JButton characterButton = new JButton("Character view");
        JButton prefabButton = new JButton("Prefab view");

        JButton[] buttons = {
            blockoutButton,
            characterButton,
            prefabButton
        };

        for (JButton button : buttons) {
            style.styleScrollbarButton(button);
            content.add(button);
        }

        final JButton[] selected = { blockoutButton };

        for (JButton button : buttons) {
            button.addActionListener(e -> {
                style.setScrollbarButtonSelected(selected[0], false);

                selected[0] = button;

                style.setScrollbarButtonSelected(selected[0], true);
            });
        }

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        sidebar.add(scrollPane, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel buildMainPanel(KebabStyle style, JFrame owner) {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        return content;
    }

    private JPanel buildFooter(KebabStyle style) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));

        cancelButton.addActionListener(e -> dispose());

        saveButton.addActionListener(e -> dispose());

        footer.add(cancelButton);
        footer.add(saveButton);

        return footer;
    }

}
