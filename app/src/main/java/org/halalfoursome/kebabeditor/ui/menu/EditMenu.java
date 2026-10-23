package org.halalfoursome.kebabeditor.ui.menu;

import java.io.IOException;
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

        SingleItem spawnBox = new SingleItem("Add greybox box", Maybe.some("control B"),
            ProjectRequired.guard(frame, editor, () -> editor.spawnGreybox(GreyboxShape.BOX, spawnPoint.get())));

        SingleItem spawnSphere = new SingleItem("Add greybox sphere", Maybe.some("control shift B"),
            ProjectRequired.guard(frame, editor, () -> editor.spawnGreybox(GreyboxShape.SPHERE, spawnPoint.get())));

        SingleItem spawnEmpty = new SingleItem("Add empty", Maybe.none(),
            ProjectRequired.guard(frame, editor, () -> editor.spawnEmpty(spawnPoint.get())));

        SingleItem spawnPlayerStart = new SingleItem("Add player start", Maybe.none(),
            ProjectRequired.guard(frame, editor, () -> editor.spawnArchetype(BuiltinArchetypes.PLAYER_START, spawnPoint.get())));

        SingleItem spawnPointLight = lightItem(frame, editor, spawnPoint, "Add point light", BuiltinArchetypes.POINT_LIGHT);
        SingleItem spawnDirLight = lightItem(frame, editor, spawnPoint, "Add directional light", BuiltinArchetypes.DIR_LIGHT);
        SingleItem spawnSpotLight = lightItem(frame, editor, spawnPoint, "Add spot light", BuiltinArchetypes.SPOT_LIGHT);

        SingleItem importModel = new SingleItem("Import glTF model...", Maybe.some("control I"),
            ProjectRequired.guard(frame, editor, () ->
                new FileChooser(FileChooserMode.FILES)
                    .addFilter("glTF model", "gltf", "glb")
                    .open(frame, file -> {
                        try {
                            editor.importModel(file.toPath(), spawnPoint.get());
                        } catch (IOException e) {
                            ErrorDialogs.show(frame, "Cannot import model", e);
                        }
                    })
            ));

        SingleItem editObject = new SingleItem("Edit selected object...", Maybe.some("control E"),
            ProjectRequired.guard(frame, editor, () ->
                editor.selection().primary().ifSomeOrElse(
                    node -> new ObjectDialog(frame, editor, node).setVisible(true),
                    () -> JOptionPane.showMessageDialog(
                        frame,
                        "Select an object first.",
                        "Edit object",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                )
            ));

        SingleItem deleteObject = new SingleItem("Delete selected object", Maybe.some("DELETE"),
            ProjectRequired.guard(frame, editor, () -> deleteSelected(frame, editor)));

        ButtonGroup tools = new ButtonGroup();

        setItems(new MenuItem[] {
            spawnBox,
            spawnSphere,
            spawnEmpty,
            spawnPlayerStart,
            new SeparatorItem(),
            spawnPointLight,
            spawnDirLight,
            spawnSpotLight,
            new SeparatorItem(),
            importModel,
            new SeparatorItem(),
            editObject,
            deleteObject,
            new SeparatorItem(),
            toolItem(view, "Move tool  (1)", "1", GizmoMode.TRANSLATE, tools),
            toolItem(view, "Rotate tool  (2)", "2", GizmoMode.ROTATE, tools),
            toolItem(view, "Scale tool  (3)", "3", GizmoMode.SCALE, tools),
        });
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

    private static SingleItem lightItem(JFrame frame, Editor editor, Supplier<Vec3> spawnPoint, String label, String archetypeId) {
        return new SingleItem(label, Maybe.none(),
            ProjectRequired.guard(frame, editor, () ->
                editor.spawnArchetype(archetypeId, spawnPoint.get().add(new Vec3(0, LIGHT_HEIGHT, 0)))
            ));
    }

    // The mode also changes from the viewport's 1/2/3 keys, so each item mirrors it.
    private static RadioItem toolItem(Maybe<EditorView> view, String label, String keyStroke, GizmoMode mode, ButtonGroup group) {
        RadioItem item = new RadioItem(label, Maybe.some(keyStroke), group,
            () -> view.ifSome(v -> v.setGizmoMode(mode)));

        view.ifSomeOrElse(v -> {
            Runnable sync = () -> item.setSelected(v.getGizmoMode() == mode);
            sync.run();
            v.addGizmoModeListener(sync);
        }, () -> item.component().setEnabled(false));

        return item;
    }
}
