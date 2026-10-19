package org.halalfoursome.kebabeditor.ui.menu;

import java.io.IOException;
import java.util.function.Supplier;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
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

        ButtonGroup tools = new ButtonGroup();

        setItems(new MenuItem[] {
            spawnBox,
            spawnSphere,
            spawnEmpty,
            new SeparatorItem(),
            spawnPointLight,
            spawnDirLight,
            spawnSpotLight,
            new SeparatorItem(),
            importModel,
            new SeparatorItem(),
            toolItem(view, "Move tool  (1)", "1", GizmoMode.TRANSLATE, tools),
            toolItem(view, "Rotate tool  (2)", "2", GizmoMode.ROTATE, tools),
            toolItem(view, "Scale tool  (3)", "3", GizmoMode.SCALE, tools),
        });
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
