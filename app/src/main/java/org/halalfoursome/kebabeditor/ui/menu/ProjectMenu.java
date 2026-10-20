package org.halalfoursome.kebabeditor.ui.menu;


import javax.swing.JFrame;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.ui.archetype.ArchetypeManagerDialog;
import org.halalfoursome.kebabeditor.ui.menu.generic.Menu;
import org.halalfoursome.kebabeditor.ui.menu.generic.MenuItem;
import org.halalfoursome.kebabeditor.ui.menu.generic.SingleItem;
import org.halalfoursome.kebabeditor.ui.project.AssetScopeDialog;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class ProjectMenu extends Menu {

    public ProjectMenu(JFrame frame, Editor editor) {
        super("Project");

        SingleItem manageArchetypes = new SingleItem(
            "Manage archetypes", 
            Maybe.some("control shift A"), 
            () -> new ArchetypeManagerDialog(frame, editor).setVisible(true)
        );

        SingleItem assetFolders = new SingleItem(
            "Asset folders...",
            Maybe.none(),
            () -> new AssetScopeDialog(frame, editor).setVisible(true)
        );

        Runnable updateEnabled = () -> {
            manageArchetypes.setEnabled(editor.isProjectOpen());
            assetFolders.setEnabled(editor.isProjectOpen());
        };
        updateEnabled.run();
        editor.addProjectListener(updateEnabled);

        setItems(new MenuItem[] { manageArchetypes, assetFolders });
    }
}
