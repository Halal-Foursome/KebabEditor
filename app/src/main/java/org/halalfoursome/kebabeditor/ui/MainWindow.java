package org.halalfoursome.kebabeditor.ui;

import javax.swing.JFrame;

import org.halalfoursome.kebabeditor.editor.Editor;

public class MainWindow {
    
    private final JFrame frame;

    public MainWindow(Editor editor) {
        frame = new JFrame("Kebab Editor");

        setupLayout();
        setupActions();
    }

    public void show() {
        frame.setVisible(true);
    }

    private void setupLayout() {}

    private void setupActions() {}
}
