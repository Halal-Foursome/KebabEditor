package org.halalfoursome.kebabeditor.ui.assets;

import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.*;
import org.halalfoursome.kebabeditor.project.EditorSessions;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.ResolvedModel;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.ui.navigation.FreeFlightNavigation;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

/** Read-only model view: no object activation, placement, transform gizmos or save actions. */
public final class ModelPreviewWindow extends JFrame {
    public ModelPreviewWindow(ResolvedModel model) {
        super(model.path().getFileName() + " — Model Preview");
        EditorView view = new EditorView(EditorSessions.create(), model);
        view.setNavigationMode(new FreeFlightNavigation());
        add(view, BorderLayout.CENTER);
        JLabel help = new JLabel("Drag to look · WASD to fly · wheel to dolly · read-only preview");
        help.setFont(KebabStyle.getCurrent().uiFont());
        add(help, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationByPlatform(true);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { view.close(); }
        });
    }
}
