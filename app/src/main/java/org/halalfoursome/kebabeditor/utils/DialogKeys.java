package org.halalfoursome.kebabeditor.utils;

import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

public final class DialogKeys {

    private DialogKeys() {}

    public static void closeOnEscape(JDialog dialog) {
        closeOnEscape(dialog, dialog::dispose);
    }

    public static void closeOnEscape(JDialog dialog, Runnable onEscape) {
        dialog.getRootPane().registerKeyboardAction(
            e -> onEscape.run(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
}
