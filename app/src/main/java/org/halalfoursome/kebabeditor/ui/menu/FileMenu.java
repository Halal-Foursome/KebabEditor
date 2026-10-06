package org.halalfoursome.kebabeditor.ui.menu;

import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JFrame;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.project.RecentProjects;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.project.CreateProjectDialog;
import org.halalfoursome.kebabeditor.utils.ErrorDialogs;
import org.halalfoursome.kebabeditor.utils.FileChooser;
import org.halalfoursome.kebabeditor.utils.FileChooser.FileChooserMode;

public class FileMenu extends Menu {

    public FileMenu(JFrame frame, Editor editor) {
        super("File");

        Menu openRecent = new Menu("Open recent...");
        updateRecent(openRecent, frame, editor);
        editor.addProjectListener(() -> updateRecent(openRecent, frame, editor));

        setItems(new MenuItem[] {
            new SingleItem("Create new project...", Optional.of("control N"), () -> {
                CreateProjectDialog createProjectDialog = new CreateProjectDialog(frame, editor);
                createProjectDialog.setVisible(true);
            }),

            new SeparatorItem(),

            new SingleItem("Open project", Optional.of("control O"), () -> {
                FileChooser fileChooser = new FileChooser(FileChooserMode.FILES)
                    .addFilter("glTF scene", "gltf", "glb");

                fileChooser.open(frame, file -> {
                    try {
                        editor.openProject(file.toPath());
                    } catch (SceneException e) {
                        ErrorDialogs.show(frame, "Scene error, cannot open project", e);
                    } catch (IOException e) {
                        ErrorDialogs.show(frame, "I/O error, cannot open project", e);
                    }
                });
            }),

            openRecent,

            new SeparatorItem(),

            new SingleItem("Save", Optional.of("control S"), () -> {
                if (editor.isProjectOpen()) {
                    try {
                        editor.saveProject();
                    } catch (SceneFormatException e) {
                        ErrorDialogs.show(frame, "Scene format error, cannot save project", e);
                    } catch (IOException e) {
                        ErrorDialogs.show(frame, "I/O error, cannot save project", e);
                    }
                }
            }),

            new SeparatorItem(),

            new SingleItem("Exit", Optional.of("control Q"), () -> {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }),
        });
    }

    private static void updateRecent(Menu menu, JFrame frame, Editor editor) {
        RecentProjects recent = editor.recentProjects();
        List<Path> paths = recent.paths();
        List<MenuItem> items = new ArrayList<>();

        for (int i = 0; i < paths.size(); i++) {
            Path path = paths.get(i);
            items.add(new SingleItem(
                path.toString(),
                i == 0 ? Optional.of("control shift R") : Optional.empty(),
                () -> {
                    try {
                        editor.openProject(path);
                    } catch (SceneException e) {
                        recent.remove(path);
                        updateRecent(menu, frame, editor);
                        ErrorDialogs.show(frame, "Scene error, failed to open scene file", e);
                    } catch (IOException e) {
                        recent.remove(path);
                        updateRecent(menu, frame, editor);
                        ErrorDialogs.show(frame, "I/O error, failed to open scene file", e);
                    }
                }
            ));
        }

        if (!paths.isEmpty()) {
            items.add(new SeparatorItem());
            items.add(new SingleItem("Clear recent projects", Optional.empty(), () -> {
                recent.clear();
                updateRecent(menu, frame, editor);
            }));
        }

        menu.setItems(items.toArray(MenuItem[]::new));
    }
}
