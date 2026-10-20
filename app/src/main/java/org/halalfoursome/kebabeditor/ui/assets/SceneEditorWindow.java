package org.halalfoursome.kebabeditor.ui.assets;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.ui.menu.EditMenu;
import org.halalfoursome.kebabeditor.ui.menu.ViewMenu;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuBar;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.navigation.FreeFlightNavigation;
import org.halalfoursome.kebabeditor.ui.object.ObjectDialog;
import org.halalfoursome.kebabeditor.utils.ErrorDialogs;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

// Saving writes only this scene, never the shared archetype definitions
public final class SceneEditorWindow extends JFrame {

    private final Editor editor;
    private final EditorView view;

    public SceneEditorWindow(Editor editor) {
        super(editor.currentProjectFile().unwrap().getFileName() + " — Scene Editor");
        this.editor = editor;
        view = new EditorView(editor);
        view.setNavigationMode(new FreeFlightNavigation());
        view.addNodeActivatedListener(node -> new ObjectDialog(this, editor, node).setVisible(true));
        add(view, BorderLayout.CENTER);

        JLabel help = new JLabel("Edit objects with the gizmo or Edit menu · Ctrl+S saves this scene");
        help.setFont(KebabStyle.getCurrent().uiFont());
        add(help, BorderLayout.SOUTH);

        setupMenus();
        setupClosing();
        setSize(1000, 750);
        setLocationByPlatform(true);
    }

    private void setupMenus() {
        MenuBar bar = new MenuBar(this);
        Menu file = new Menu("File");
        file.setItems(new MenuItem[] {
            new SingleItem("Save scene", Maybe.some("control S"), this::saveScene)
        });
        bar.addMenu(file);
        bar.addMenu(new EditMenu(this, editor, view::spawnPoint, Maybe.some(view)));
        bar.addMenu(new ViewMenu(this, editor, Maybe.some(view)));
    }

    private void saveScene() {
        try {
            editor.saveScene();
            LogService.get().info("Saved scene " + editor.currentProjectFile().unwrap());
        } catch (IOException e) {
            ErrorDialogs.show(this, "Cannot save scene", e);
        } catch (SceneFormatException e) {
            ErrorDialogs.show(this, "Cannot save scene", e);
        }
    }

    private void setupClosing() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                int answer = JOptionPane.showConfirmDialog(
                    SceneEditorWindow.this,
                    "Close this scene? Save first to keep edits.",
                    "Close scene",
                    JOptionPane.OK_CANCEL_OPTION
                );
                if (answer == JOptionPane.OK_OPTION) {
                    dispose();
                }
            }

            @Override
            public void windowClosed(WindowEvent event) {
                view.close();
            }
        });
    }
}
