package org.halalfoursome.kebabeditor.utils;

import java.io.File;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class FileChooser {
    public enum FileChooserMode {
        FILES,
        DIRS,
        ALL;

        private int toSwing() {
            switch (this) {
                case FILES:
                    return JFileChooser.FILES_ONLY;
                case DIRS:
                    return JFileChooser.DIRECTORIES_ONLY;
                case ALL:
                    return JFileChooser.FILES_AND_DIRECTORIES;
                default:
                    return -1;
            }
        }
    }

    private JFileChooser fileChooser;

    public FileChooser(FileChooserMode mode) {
        this.fileChooser = new JFileChooser() {{
            setFileSelectionMode(mode.toSwing());
        }};
    }

    public void open(JFrame frame, Consumer<File> onOpen) {
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            onOpen.accept(fileChooser.getSelectedFile());
        }
    }

    public void save(JFrame frame, Consumer<File> onSave) {
        int result = fileChooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            onSave.accept(fileChooser.getSelectedFile());
        }
    }
}
