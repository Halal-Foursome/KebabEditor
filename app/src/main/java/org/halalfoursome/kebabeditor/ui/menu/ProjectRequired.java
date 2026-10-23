package org.halalfoursome.kebabeditor.ui.menu;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.halalfoursome.kebabeditor.editor.Editor;

/**
 * Guard for actions that edit the scene. They stay enabled without a project so
 * their shortcuts explain what is missing instead of silently doing nothing.
 * They are refused during a playtest, which plays the scene as it was when it started.
 */
final class ProjectRequired {

    private ProjectRequired() {}

    static boolean check(JFrame frame, Editor editor) {
        if (editor.isPlaytesting()) {
            JOptionPane.showMessageDialog(
                frame,
                "The level is being playtested. Stop the playtest (Esc) to edit it.",
                "Playtest running",
                JOptionPane.INFORMATION_MESSAGE
            );
            return false;
        }

        if (editor.isProjectOpen()) {
            return true;
        }

        JOptionPane.showMessageDialog(
            frame,
            "Open or create a project first (File > Open project / Create new project).",
            "No project open",
            JOptionPane.INFORMATION_MESSAGE
        );
        return false;
    }

    static Runnable guard(JFrame frame, Editor editor, Runnable action) {
        return () -> {
            if (check(frame, editor)) {
                action.run();
            }
        };
    }
}
