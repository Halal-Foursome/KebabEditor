package org.halalfoursome.kebabeditor.ui.menu;


import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.ui.EditorView;
import org.halalfoursome.kebabeditor.ui.menu.generic.CheckItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.RadioItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SeparatorItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.placement.PlacementTool;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class ViewMenu extends Menu {

    /**
     * @param view the viewport; none when it is unavailable
     */
    public ViewMenu(JFrame frame, Editor editor, Maybe<EditorView> view) {
        super("View");

        setItems(new MenuItem[] {
            blockoutMenu(frame, editor, view.map(EditorView::getPlacement)),

            new SeparatorItem(),

            showIconsItem(view),
        });
    }

    private static CheckItem showIconsItem(Maybe<EditorView> view) {
        CheckItem showIcons = new CheckItem("Show icons", Maybe.none(), on -> view.ifSome(v -> v.setShowIcons(on)));

        showIcons.setSelected(view.map(EditorView::isShowIcons).unwrapOr(false));
        showIcons.component().setEnabled(view.isSome());

        return showIcons;
    }

    private static Menu blockoutMenu(JFrame frame, Editor editor, Maybe<PlacementTool> placement) {
        Menu blockout = new Menu("Blockout view");
        if (placement.isNone()) {
            blockout.component().setEnabled(false);
            return blockout;
        }

        PlacementTool tool = placement.unwrap();

        CheckItem placementRay = new CheckItem("Placement ray", Maybe.some("control shift P"), on -> {
            if (on && !ProjectRequired.check(frame, editor)) {
                on = false;
            }
            tool.setActive(on);
        });

        ButtonGroup shapes = new ButtonGroup();
        RadioItem box = new RadioItem("Place box", shapes, () -> tool.setShape(GreyboxShape.BOX));
        RadioItem sphere = new RadioItem("Place sphere", shapes, () -> tool.setShape(GreyboxShape.SPHERE));

        // The tool also changes on its own (Esc leaves placement), so mirror its state.
        Runnable sync = () -> {
            placementRay.setSelected(tool.isActive());
            box.setSelected(tool.getShape() == GreyboxShape.BOX);
            sphere.setSelected(tool.getShape() == GreyboxShape.SPHERE);
        };
        sync.run();
        tool.addListener(sync);
        // A closed project leaves nothing to place into.
        editor.addProjectListener(() -> {
            if (!editor.isProjectOpen()) {
                tool.setActive(false);
            }
        });

        CheckItem snapToGrid = new CheckItem("Snap to grid", Maybe.some("control shift G"), editor::setSnapping);
        snapToGrid.setSelected(editor.isSnapping());

        SingleItem snapDistance = new SingleItem("Snap distance...", Maybe.none(),
            ProjectRequired.guard(frame, editor, () -> askSnapDistance(frame, editor)));

        blockout.setItems(new MenuItem[] {
            placementRay,
            new SeparatorItem(),
            box,
            sphere,
            new SeparatorItem(),
            snapToGrid,
            snapDistance,
        });
        return blockout;
    }

    // The value lives on the scene's snap_distance object, so it is saved with the level.
    private static void askSnapDistance(JFrame frame, Editor editor) {
        Object input = JOptionPane.showInputDialog(
            frame,
            "Grid step in meters, stored on the level's Snap Distance object:",
            "Snap distance",
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            String.valueOf(editor.snapDistance())
        );
        if (input == null) {
            return;
        }

        try {
            editor.setSnapDistance(Float.parseFloat(input.toString().trim().replace(',', '.')));
        } catch (IllegalArgumentException e) {
            // NumberFormatException included: keep the old value and say why.
            JOptionPane.showMessageDialog(
                frame,
                "'" + input + "' is not a positive number; the snap distance is unchanged.",
                "Invalid snap distance",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }
}
