package org.halalfoursome.kebabeditor.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.halalfoursome.kebabeditor.utils.DialogKeys;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class KebabDialog extends JDialog {

    private static final String GIF_RESOURCE = "/img/kebab.gif";

    public KebabDialog(JDialog owner) {
        super(owner, "Kebab", true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JButton close = new JButton("Close");
        close.setFont(KebabStyle.getCurrent().uiFont());
        close.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.add(close);

        add(buildAnimation(), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(close);
        DialogKeys.closeOnEscape(this);

        pack();
        setLocationRelativeTo(owner);
    }

    private JLabel buildAnimation() {
        URL gif = getClass().getResource(GIF_RESOURCE);
        if (gif == null) {
            return new JLabel("No kebab today.", SwingConstants.CENTER);
        }

        return new JLabel(new ImageIcon(gif));
    }
}
