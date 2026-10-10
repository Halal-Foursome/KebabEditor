package org.halalfoursome.kebabeditor;

import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.project.ProjectLoader;
import org.halalfoursome.kebabeditor.project.ProjectManager;
import org.halalfoursome.kebabeditor.project.ProjectWriter;
import org.halalfoursome.kebabeditor.project.RecentProjects;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.halalfoursome.kebabeditor.ui.MainWindow;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

import com.fasterxml.jackson.databind.ObjectMapper;

public class KebabApplication {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KebabStyle.setCurrent(KebabStyle.defaultStyle());

            ObjectMapper mapper = new ObjectMapper();
            ProjectWriter writer = new ProjectWriter(new GltfSceneWriter(), mapper);
            ProjectLoader loader = new ProjectLoader(new GltfSceneReader(), mapper);
            ProjectManager projectManager = new ProjectManager(
                new RecentProjects(),
                writer,
                loader
            );
            
            Editor editor = new Editor(projectManager);
            MainWindow window = new MainWindow(editor);
            LogService.get().info("Kebab Editor started");

            window.show();
        });
    }
}
