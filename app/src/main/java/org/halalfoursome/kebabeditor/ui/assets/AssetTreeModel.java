package org.halalfoursome.kebabeditor.ui.assets;

import java.nio.file.Path;
import java.util.List;
import javax.swing.tree.*;
import org.halalfoursome.kebabeditor.assets.AssetService.Entry;

/** Directory nodes are populated only when expanded or navigated to. Mutated on the EDT. */
public final class AssetTreeModel extends DefaultTreeModel {
    public static final class Directory extends DefaultMutableTreeNode {
        final Path path;
        boolean loaded;
        Directory(Path path) {
            super(path);
            this.path = path;
            add(new DefaultMutableTreeNode("Loading…"));
        }
        @Override public String toString() { return path.getFileName() == null ? path.toString() : path.getFileName().toString(); }
    }
    public AssetTreeModel(Path root) { super(new Directory(root)); }
    public Directory directoryRoot() { return (Directory) getRoot(); }
    public void populate(Directory directory, List<Entry> entries) {
        if (directory.loaded) return;
        directory.removeAllChildren();
        entries.stream().filter(Entry::directory).forEach(e -> directory.add(new Directory(e.path())));
        directory.loaded = true;
        nodeStructureChanged(directory);
    }
    public Directory child(Directory parent, Path path) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) instanceof Directory dir && dir.path.equals(path)) return dir;
        }
        return null;
    }
}
