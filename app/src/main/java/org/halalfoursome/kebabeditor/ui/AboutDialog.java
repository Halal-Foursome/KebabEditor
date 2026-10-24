package org.halalfoursome.kebabeditor.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.time.Year;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.halalfoursome.kebabeditor.KebabApplication;
import org.halalfoursome.kebabeditor.utils.DialogKeys;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public class AboutDialog extends JDialog {
    
    private static final String COPYRIGHT = generateCopyright();

    private final Font uiFont = KebabStyle.getCurrent().uiFont();

    public AboutDialog(JFrame owner) {
        super(owner, "About " + KebabApplication.APP_NAME, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(28, 36, 20, 36));

        // Big centered SVG icon
        JLabel icon = new JLabel(
            new FlatSVGIcon("img/logo.svg", 112, 112)
        );
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new KebabDialog(AboutDialog.this).setVisible(true);
            }
        });

        // Application name and Gradle version
        JLabel heading = new JLabel(
            KebabApplication.APP_NAME + " " + loadVersion()
        );
        heading.setFont(uiFont.deriveFont(Font.BOLD, 24f));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel copyright = new JLabel(COPYRIGHT);
        copyright.setFont(uiFont);
        copyright.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel authors = new JLabel(KebabApplication.AUTHORS);
        authors.setFont(uiFont);
        authors.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(icon);
        content.add(Box.createVerticalStrut(18));
        content.add(heading);
        content.add(Box.createVerticalStrut(8));
        content.add(copyright);
        content.add(Box.createVerticalStrut(4));
        content.add(authors);
        content.add(Box.createVerticalStrut(24));

        // License on the left, Close on the right
        JPanel buttons = new JPanel(new BorderLayout(12, 0));

        JButton licenseButton = new JButton("License");
        licenseButton.setFont(uiFont);
        licenseButton.addActionListener(e -> new LicenseDialog(this).setVisible(true));

        JButton closeButton = new JButton("Close");
        closeButton.setFont(uiFont);
        closeButton.addActionListener(e -> dispose());

        buttons.add(licenseButton, BorderLayout.WEST);
        buttons.add(closeButton, BorderLayout.EAST);

        content.add(buttons);

        setContentPane(content);
        getRootPane().setDefaultButton(closeButton);
        DialogKeys.closeOnEscape(this);

        pack();
        setLocationRelativeTo(owner);
    }

    private static String loadVersion() {
        Properties properties = new Properties();

        try (InputStream in = AboutDialog.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {}

        return properties.getProperty("app.version", "dev");
    }

    private static String generateCopyright() {
        String header = "Copyright © ";
        int firstYear = KebabApplication.FIRST_YEAR;
        int currentYear = Year.now().getValue();

        return firstYear >= currentYear
            ? header + firstYear
            : header + firstYear + "-" + currentYear;
    }
}
