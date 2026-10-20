package org.halalfoursome.kebabeditor.ui.assets;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.halalfoursome.kebabeditor.assets.AssetService.Entry;

// Directory nodes are filled on expand or navigation; mutated on the EDT only
public final class AssetTreeModel extends DefaultTreeModel {

    public static final class Directory extends DefaultMutableTreeNode {

        private final Path path;
        private boolean loaded;

        private Directory(Path path) {
            super(path);
            this.path = path;
            add(new DefaultMutableTreeNode("Loading…"));
        }

        public Path path() {
            return path;
        }

        public boolean loaded() {
            return loaded;
        }

        @Override
        public String toString() {
            Path name = path.getFileName();
            return name == null ? path.toString() : name.toString();
        }
    }

    public AssetTreeModel(Path root) {
        super(new Directory(root));
    }

    public Directory directoryRoot() {
        return (Directory) getRoot();
    }

    public void populate(Directory directory, List<Entry> entries) {
        if (directory.loaded) {
            return;
        }
        directory.removeAllChildren();
        for (Entry entry : entries) {
            if (entry.directory()) {
                directory.add(new Directory(entry.path()));
            }
        }
        directory.loaded = true;
        nodeStructureChanged(directory);
    }

    public Directory child(Directory parent, Path path) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) instanceof Directory dir && dir.path.equals(path)) {
                return dir;
            }
        }
        return null;
    }

    public void collectLoaded(Set<Path> into) {
        collectLoaded(directoryRoot(), into);
    }

    private void collectLoaded(Directory directory, Set<Path> into) {
        if (!directory.loaded) {
            return;
        }
        into.add(directory.path);
        for (int i = 0; i < directory.getChildCount(); i++) {
            if (directory.getChildAt(i) instanceof Directory child) {
                collectLoaded(child, into);
            }
        }
    }
}
