package org.halalfoursome.kebabeditor.utils;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileChooser {
    
    @FunctionalInterface
    public interface FileHandler {
        void accept(File file) throws IOException;
    }

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

    public void open(Component frame, FileHandler onOpen) {
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            handle(frame, onOpen, "Failed to open file");
        }
    }

    public void save(Component frame, FileHandler onSave) {
        int result = fileChooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            handle(frame, onSave, "Failed to save file");
        }
    }

    private void handle(Component frame, FileHandler handler, String title) {
        File file = fileChooser.getSelectedFile();
        
        try {
            handler.accept(file);
        } catch (IOException e) {
            ErrorDialogs.show(frame, title, e);
        }
    }
}
