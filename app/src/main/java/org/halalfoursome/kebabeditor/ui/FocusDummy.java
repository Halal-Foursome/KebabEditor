package org.halalfoursome.kebabeditor.ui;

import java.awt.Dimension;

import javax.swing.JPanel;

public class FocusDummy extends JPanel {

    public FocusDummy() {
        setFocusable(true);
        setRequestFocusEnabled(true);
        setSize(new Dimension(0, 0));
        setPreferredSize(new Dimension(0, 0));
    }
}