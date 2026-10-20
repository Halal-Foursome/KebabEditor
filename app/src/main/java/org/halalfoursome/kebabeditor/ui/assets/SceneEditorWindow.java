package org.halalfoursome.kebabeditor.ui.assets;

import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.*;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.ui.menu.EditMenu;
import org.halalfoursome.kebabeditor.ui.menu.ViewMenu;
import org.halalfoursome.kebabeditor.ui.menu.generic.*;
import org.halalfoursome.kebabeditor.ui.navigation.FreeFlightNavigation;
import org.halalfoursome.kebabeditor.ui.object.ObjectDialog;
import org.halalfoursome.kebabeditor.utils.*;
import org.halalfoursome.kebabeditor.logging.LogService;

/** A fixed scene session; save writes only this scene, never another window's shared archetype registry. */
public final class SceneEditorWindow extends JFrame {
    public SceneEditorWindow(Editor editor) {
        super(editor.currentProjectFile().unwrap().getFileName() + " — Scene Editor");
        EditorView view = new EditorView(editor);
        view.setNavigationMode(new FreeFlightNavigation());
        view.addNodeActivatedListener(node -> new ObjectDialog(this, editor, node).setVisible(true));
        add(view, BorderLayout.CENTER);
        JLabel help = new JLabel("Edit objects with the gizmo or Edit menu · Ctrl+S saves this scene");
        help.setFont(KebabStyle.getCurrent().uiFont());
        add(help, BorderLayout.SOUTH);
        MenuBar bar = new MenuBar(this);
        Menu file = new Menu("File");
        file.setItems(new MenuItem[] { new SingleItem("Save scene", Maybe.some("control S"), () -> {
            try {
                editor.saveScene();
                LogService.get().info("Saved scene " + editor.currentProjectFile().unwrap());
            } catch (java.io.IOException e) { ErrorDialogs.show(this, "Cannot save scene", e); }
            catch (org.halalfoursome.kebabeditor.scene.error.SceneFormatException e) {
                ErrorDialogs.show(this, "Cannot save scene", e);
            }
        }) });
        bar.addMenu(file);
        bar.addMenu(new EditMenu(this, editor, view::spawnPoint, Maybe.some(view)));
        bar.addMenu(new ViewMenu(this, editor, Maybe.some(view)));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1000, 750);
        setLocationByPlatform(true);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                if (JOptionPane.showConfirmDialog(SceneEditorWindow.this,
                    "Close this scene? Save first to keep edits.", "Close scene", JOptionPane.OK_CANCEL_OPTION)
                    == JOptionPane.OK_OPTION) dispose();
            }
            @Override public void windowClosed(WindowEvent event) { view.close(); }
        });
    }
}
