package org.halalfoursome.kebabeditor;

import java.util.List;

import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.project.FileProjectLoader;
import org.halalfoursome.kebabeditor.project.FileProjectWriter;
import org.halalfoursome.kebabeditor.project.FileRecentProjects;
import org.halalfoursome.kebabeditor.project.ModelImporter;
import org.halalfoursome.kebabeditor.project.ProjectLoader;
import org.halalfoursome.kebabeditor.project.ProjectManager;
import org.halalfoursome.kebabeditor.project.ProjectWriter;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.halalfoursome.kebabeditor.scene.resolve.CompositeModelExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.GltfMeshExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.ModelExtractor;
import org.halalfoursome.kebabeditor.ui.MainWindow;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

import com.fasterxml.jackson.databind.ObjectMapper;

public class KebabApplication {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KebabStyle.setCurrent(KebabStyle.defaultStyle());

            ObjectMapper mapper = new ObjectMapper();
            ModelExtractor modelExtractor = new CompositeModelExtractor(List.of(
                // Add other extractors later?
                new GltfMeshExtractor()
            ));
            ProjectWriter writer = new FileProjectWriter(new GltfSceneWriter(mapper), mapper);
            ProjectLoader loader = new FileProjectLoader(new GltfSceneReader(mapper), mapper, modelExtractor);
            ProjectManager projectManager = new ProjectManager(
                new FileRecentProjects(),
                writer,
                loader
            );
            
            Editor editor = new Editor(projectManager, new ModelImporter(mapper));
            MainWindow window = new MainWindow(editor);
            LogService.get().info("Kebab Editor started");

            window.show();
        });
    }
}
