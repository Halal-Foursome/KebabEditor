package org.halalfoursome.kebabeditor.utils;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.logging.LogService;

public final class WebLinks {

    private WebLinks() {}

    // Not every desktop can open a browser, so the link is shown for copying when it cannot
    public static void open(Component parent, String url) {
        boolean canBrowse = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        if (!canBrowse) {
            showLink(parent, url);
            return;
        }

        Thread opener = new Thread(() -> browse(parent, url), "open-link");
        opener.setDaemon(true);
        opener.start();
    }

    private static void browse(Component parent, String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            LogService.get().warn("Could not open " + url + ": " + e.getMessage());
            SwingUtilities.invokeLater(() -> showLink(parent, url));
        }
    }

    private static void showLink(Component parent, String url) {
        KebabStyle style = KebabStyle.getCurrent();

        JLabel message = new JLabel("The browser could not be opened. Copy the link instead:");
        message.setFont(style.uiFont());

        JTextField link = new JTextField(url, 36);
        link.setFont(style.uiFont());
        link.setEditable(false);
        link.selectAll();

        String[] options = { "Copy", "Close" };
        int choice = JOptionPane.showOptionDialog(
            parent,
            new Object[] { message, link },
            "Open link",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == 0) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
        }
    }
}
