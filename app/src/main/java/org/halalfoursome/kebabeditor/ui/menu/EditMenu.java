package org.halalfoursome.kebabeditor.ui.menu;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.editor.Deletion;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.render.GizmoMode;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.RadioItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.object.ObjectDialog;
import org.halalfoursome.kebabeditor.utils.ErrorDialogs;
import org.halalfoursome.kebabeditor.utils.FileChooser;
import org.halalfoursome.kebabeditor.utils.FileChooser.FileChooserMode;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class EditMenu extends Menu {

    private static final float LIGHT_HEIGHT = 2f;

    /**
     * @param spawnPoint where new objects are placed, usually what the viewport camera looks at
     * @param view       the viewport whose gizmo the tool items switch; none when it is unavailable
     */
    public EditMenu(
        JFrame frame, 
        Editor editor, 
        Supplier<Vec3> spawnPoint, 
        Maybe<EditorView> view
    ) {
        super("Edit");

        SingleItem undo = new SingleItem("Undo", Maybe.some("control Z"), editor::undo);
        SingleItem redo = new SingleItem("Redo", Maybe.some("control Y"), editor::redo);
        syncHistoryItems(editor, view, undo, redo);

        ButtonGroup tools = new ButtonGroup();

        setItems(new MenuItem[] {
            undo,
            redo,
            new SeparatorItem(),
            spawnItem(
                frame,
                editor,
                "Add greybox box",
                Maybe.some("control B"),
                () -> editor.spawnGreybox(GreyboxShape.BOX, spawnPoint.get())
            ),
            spawnItem(
                frame,
                editor,
                "Add greybox sphere",
                Maybe.some("control shift B"),
                () -> editor.spawnGreybox(GreyboxShape.SPHERE, spawnPoint.get())
            ),
            spawnItem(
                frame,
                editor,
                "Add empty",
                Maybe.none(),
                () -> editor.spawnEmpty(spawnPoint.get())
            ),
            spawnItem(
                frame,
                editor,
                "Add player start",
                Maybe.none(),
                () -> editor.spawnArchetype(BuiltinArchetypes.PLAYER_START, spawnPoint.get())
            ),
            new SeparatorItem(),
            lightItem(frame, editor, spawnPoint, "Add point light", BuiltinArchetypes.POINT_LIGHT),
            lightItem(frame, editor, spawnPoint, "Add directional light", BuiltinArchetypes.DIR_LIGHT),
            lightItem(frame, editor, spawnPoint, "Add spot light", BuiltinArchetypes.SPOT_LIGHT),
            new SeparatorItem(),
            importModelItem(frame, editor, spawnPoint),
            new SeparatorItem(),
            editObjectItem(frame, editor),
            spawnItem(
                frame,
                editor,
                "Delete selected object",
                Maybe.some("DELETE"),
                () -> deleteSelected(frame, editor)
            ),
            new SeparatorItem(),
            toolItem(view, "Move tool  (1)", "1", GizmoMode.TRANSLATE, tools),
            toolItem(view, "Rotate tool  (2)", "2", GizmoMode.ROTATE, tools),
            toolItem(view, "Scale tool  (3)", "3", GizmoMode.SCALE, tools),
        });
    }

    // Undo and redo only act on the scene while its viewport has focus, so the
    // shortcuts are left to whatever else is focused, like a text field.
    private static void syncHistoryItems(Editor editor, Maybe<EditorView> view, SingleItem undo, SingleItem redo) {
        BooleanSupplier sceneActive = () -> view.isSomeAnd(EditorView::isFocusOwner);
        Runnable sync = () -> {
            undo.setEnabled(sceneActive.getAsBoolean() && editor.history().canUndo());
            redo.setEnabled(sceneActive.getAsBoolean() && editor.history().canRedo());
        };

        sync.run();
        editor.history().addListener(sync);
        view.ifSome(v -> v.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                sync.run();
            }

            @Override
            public void focusLost(FocusEvent e) {
                sync.run();
            }
        }));
    }

    private static SingleItem spawnItem(
        JFrame frame,
        Editor editor,
        String label,
        Maybe<String> keyStroke,
        Runnable action
    ) {
        return new SingleItem(label, keyStroke, ProjectRequired.guard(frame, editor, action));
    }

    private static SingleItem importModelItem(JFrame frame, Editor editor, Supplier<Vec3> spawnPoint) {
        Runnable importModel = () -> new FileChooser(FileChooserMode.FILES)
            .addFilter("glTF model", "gltf", "glb")
            .open(frame, file -> {
                try {
                    editor.importModel(file.toPath(), spawnPoint.get());
                } catch (IOException e) {
                    ErrorDialogs.show(frame, "Cannot import model", e);
                }
            });

        return spawnItem(frame, editor, "Import glTF model...", Maybe.some("control I"), importModel);
    }

    private static SingleItem editObjectItem(JFrame frame, Editor editor) {
        Runnable editObject = () -> editor.selection().primary().ifSomeOrElse(
            node -> new ObjectDialog(frame, editor, node).setVisible(true),
            () -> JOptionPane.showMessageDialog(
                frame,
                "Select an object first.",
                "Edit object",
                JOptionPane.INFORMATION_MESSAGE
            )
        );

        return spawnItem(frame, editor, "Edit selected object...", Maybe.some("control E"), editObject);
    }

    private static void deleteSelected(JFrame frame, Editor editor) {
        if (editor.selection().nodes().isEmpty()) {
            return;
        }

        Deletion deletion = editor.planDeletion(editor.selection().nodes());

        if (!deletion.needsConfirmation() || confirmDeletion(frame, deletion)) {
            editor.delete(deletion);
        }
    }

    private static boolean confirmDeletion(JFrame frame, Deletion deletion) {
        StringBuilder message = new StringBuilder();

        if (deletion.childCount() > 0) {
            message.append("This also deletes ").append(deletion.childCount()).append(" child object(s).\n");
        }
        if (!deletion.references().isEmpty()) {
            message.append(deletion.references().size())
                .append(" reference(s) in other objects point to what is being deleted and will be cleared.");
        }

        String[] options = { "Delete", "Cancel" };
        int choice = JOptionPane.showOptionDialog(
            frame,
            message.toString().strip(),
            "Delete object",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[1]
        );

        return choice == 0;
    }

    private static SingleItem lightItem(
        JFrame frame,
        Editor editor,
        Supplier<Vec3> spawnPoint,
        String label,
        String archetypeId
    ) {
        Vec3 lifted = new Vec3(0, LIGHT_HEIGHT, 0);

        return spawnItem(
            frame,
            editor,
            label,
            Maybe.none(),
            () -> editor.spawnArchetype(archetypeId, spawnPoint.get().add(lifted))
        );
    }

    // The mode also changes from the viewport's 1/2/3 keys, so each item mirrors it.
    private static RadioItem toolItem(
        Maybe<EditorView> view,
        String label,
        String keyStroke,
        GizmoMode mode,
        ButtonGroup group
    ) {
        RadioItem item = new RadioItem(
            label,
            Maybe.some(keyStroke),
            group,
            () -> view.ifSome(v -> v.setGizmoMode(mode))
        );

        view.ifSomeOrElse(v -> {
            Runnable sync = () -> item.setSelected(v.getGizmoMode() == mode);
            sync.run();
            v.addGizmoModeListener(sync);
        }, () -> item.component().setEnabled(false));

        return item;
    }
}
