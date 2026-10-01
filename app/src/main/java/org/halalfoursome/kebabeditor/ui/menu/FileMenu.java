package org.halalfoursome.kebabeditor.ui.menu;

import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.project.RecentProjects;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.project.CreateProjectDialog;
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

                fileChooser.open(frame, file -> editor.openProject(file.toPath()));
            }),

            openRecent,

            new SeparatorItem(),

            new SingleItem("Save", Optional.of("control S"), () -> {
                // TODO: Save file
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
                    } catch (IOException e) {
                        recent.remove(path);
                        updateRecent(menu, frame, editor);
                        JOptionPane.showMessageDialog(
                            frame,
                            path.getFileName() + ": " + e.getMessage(),
                            "Failed to open file",
                            JOptionPane.ERROR_MESSAGE
                        );
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
