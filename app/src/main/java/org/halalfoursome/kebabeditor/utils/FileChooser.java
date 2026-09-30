package org.halalfoursome.kebabeditor.utils;

import java.io.File;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

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

    public FileChooser(FileChooserMode mode, String... extensions) {
        this(mode);
        if (extensions.length > 0) {
            addFilter(String.join(", ", extensions), extensions);
        }
    }

    public FileChooser addFilter(String description, String... extensions) {
        FileNameExtensionFilter filter = new FileNameExtensionFilter(description, extensions);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(filter);
        if (fileChooser.getChoosableFileFilters().length == 1) {
            fileChooser.setFileFilter(filter);
        }
        return this;
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
