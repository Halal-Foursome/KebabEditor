package org.halalfoursome.kebabeditor.ui.menu;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;

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
import org.halalfoursome.kebabeditor.utils.ErrorDialogs;
import org.halalfoursome.kebabeditor.utils.FileChooser;
import org.halalfoursome.kebabeditor.utils.FileChooser.FileChooserMode;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class EditMenu extends Menu {

    /**
     * @param spawnPoint where new objects are placed, usually what the viewport camera looks at
     * @param view       the viewport whose gizmo the tool items switch; none when it is unavailable
     */
    public EditMenu(JFrame frame, Editor editor, Supplier<Vec3> spawnPoint, Maybe<EditorView> view) {
        super("Edit");

        SingleItem spawnBox = new SingleItem("Add greybox box", Optional.of("control B"),
            ProjectRequired.guard(frame, editor, () -> editor.spawnGreybox(GreyboxShape.BOX, spawnPoint.get())));

        SingleItem spawnSphere = new SingleItem("Add greybox sphere", Optional.of("control shift B"),
            ProjectRequired.guard(frame, editor, () -> editor.spawnGreybox(GreyboxShape.SPHERE, spawnPoint.get())));

        SingleItem importModel = new SingleItem("Import glTF model...", Optional.of("control I"),
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

        ButtonGroup tools = new ButtonGroup();

        setItems(new MenuItem[] {
            spawnBox,
            spawnSphere,
            new SeparatorItem(),
            importModel,
            new SeparatorItem(),
            toolItem(view, "Move tool  (1)", "alt 1", GizmoMode.TRANSLATE, tools),
            toolItem(view, "Rotate tool  (2)", "alt 2", GizmoMode.ROTATE, tools),
            toolItem(view, "Scale tool  (3)", "alt 3", GizmoMode.SCALE, tools),
        });
    }

    // The mode also changes from the viewport's 1/2/3 keys, so each item mirrors it.
    private static RadioItem toolItem(Maybe<EditorView> view, String label, String keyStroke, GizmoMode mode, ButtonGroup group) {
        RadioItem item = new RadioItem(label, Optional.of(keyStroke), group,
            () -> view.ifSome(v -> v.setGizmoMode(mode)));

        view.ifSomeOrElse(v -> {
            Runnable sync = () -> item.setSelected(v.getGizmoMode() == mode);
            sync.run();
            v.addGizmoModeListener(sync);
        }, () -> item.component().setEnabled(false));

        return item;
    }
}
