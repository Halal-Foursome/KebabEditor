package org.halalfoursome.kebabeditor.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.halalfoursome.kebabeditor.utils.DialogKeys;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public class LicenseDialog extends JDialog {

    private static final String LICENSE_RESOURCE = "/LICENSE";

    private final Font uiFont = KebabStyle.getCurrent().uiFont();

    public LicenseDialog(JDialog owner) {
        super(owner, "MIT License", true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JButton close = new JButton("Close");
        close.setFont(uiFont);
        close.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(close);

        add(buildLicenseView(), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(close);
        DialogKeys.closeOnEscape(this);

        pack();
        setLocationRelativeTo(owner);
    }

    private JScrollPane buildLicenseView() {
        JTextArea text = new JTextArea(loadLicense(), 22, 64);
        text.setFont(uiFont);
        text.setEditable(false);
        text.setLineWrap(false);
        text.setCaretPosition(0);
        text.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(
            text,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scroll.setPreferredSize(new Dimension(560, 360));

        return scroll;
    }

    private String loadLicense() {
        try (InputStream in = getClass().getResourceAsStream(LICENSE_RESOURCE)) {
            if (in == null) {
                return "MIT license file not found.";
            }

            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Could not read the MIT license:\n" + e.getMessage();
        }
    }
}
