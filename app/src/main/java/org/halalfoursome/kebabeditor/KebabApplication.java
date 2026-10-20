package org.halalfoursome.kebabeditor;

import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.project.EditorSessions;
import org.halalfoursome.kebabeditor.project.FileRecentProjects;
import org.halalfoursome.kebabeditor.ui.MainWindow;
import org.halalfoursome.kebabeditor.utils.KebabStyle;


public class KebabApplication {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KebabStyle.setCurrent(KebabStyle.defaultStyle());

            Editor editor = EditorSessions.create(new FileRecentProjects());
            MainWindow window = new MainWindow(editor);
            LogService.get().info("Kebab Editor started");

            window.show();
        });
    }
}
