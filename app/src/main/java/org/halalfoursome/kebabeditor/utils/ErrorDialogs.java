package org.halalfoursome.kebabeditor.utils;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import javax.swing.JOptionPane;

public final class ErrorDialogs {

    private ErrorDialogs() {}

    public static void show(Component parent, String title, IOException e) {
        JOptionPane.showMessageDialog(parent, describe(e), title, JOptionPane.ERROR_MESSAGE);
    }

    static String describe(IOException e) {
        return switch (e) {
            case FileAlreadyExistsException x -> name(x) + " already exists";
            case NoSuchFileException x        -> name(x) + " does not exist";
            case AccessDeniedException x      -> "Access denied: " + name(x);
            case DirectoryNotEmptyException x -> name(x) + " is not empty";
            case FileSystemException x        -> x.getReason() != null
                ? name(x) + ": " + x.getReason()
                : name(x);
            default -> e.getMessage() != null
                ? e.getMessage()
                : e.getClass().getSimpleName();
        };
    }

    private static String name(FileSystemException e) {
        if (e.getFile() == null) {
            return "File";
        }

        Path fileName = Path.of(e.getFile()).getFileName();
        return fileName != null ? fileName.toString() : e.getFile();
    }
}
