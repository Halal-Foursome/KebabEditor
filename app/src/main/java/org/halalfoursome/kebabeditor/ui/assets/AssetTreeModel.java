
package org.halalfoursome.kebabeditor.ui.assets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class AssetTreeModel extends DefaultTreeModel {

    public AssetTreeModel() {
        super(new DefaultMutableTreeNode("No project"));
    }

    public void load(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Not a directory: " + directory);
        }

        DefaultMutableTreeNode root = buildNode(directory);
        setRoot(root);
    }

    public void clear() {
        setRoot(new DefaultMutableTreeNode("No project"));
    }

    private DefaultMutableTreeNode buildNode(Path path)
            throws IOException {

        DefaultMutableTreeNode node =
                new DefaultMutableTreeNode(path);

        if (!Files.isDirectory(
                path, LinkOption.NOFOLLOW_LINKS)) {
            return node;
        }

        List<Path> children;

        try (Stream<Path> stream = Files.list(path)) {
            children = stream
                    .sorted(
                            Comparator
                                    .comparing(
                                            (Path p) -> !Files.isDirectory(
                                                    p, LinkOption.NOFOLLOW_LINKS)
                                    )
                                    .thenComparing(
                                            p -> p.getFileName()
                                                    .toString()
                                                    .toLowerCase()
                                    )
                    )
                    .toList();
        }

        for (Path child : children) {
            node.add(buildNode(child));
        }

        return node;
    }
}
