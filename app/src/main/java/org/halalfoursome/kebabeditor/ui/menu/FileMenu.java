package org.halalfoursome.kebabeditor.ui.menu;

import java.awt.event.WindowEvent;
import java.util.Optional;

import javax.swing.JFrame;

import org.halalfoursome.kebabeditor.editor.Editor;
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
}
