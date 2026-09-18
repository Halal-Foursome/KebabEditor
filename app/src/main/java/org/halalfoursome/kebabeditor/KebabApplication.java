package org.halalfoursome.kebabeditor;

import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.project.ProjectManager;
import org.halalfoursome.kebabeditor.ui.MainWindow;

public class KebabApplication {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProjectManager projectManager = new ProjectManager();
            Editor editor = new Editor(projectManager);
            MainWindow window = new MainWindow(editor);

            window.show();
        });
    }
}
