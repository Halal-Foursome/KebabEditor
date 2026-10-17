
package org.halalfoursome.kebabeditor.ui.assets;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import org.halalfoursome.kebabeditor.editor.Editor;

public class AssetExplorerTab extends JPanel {

    private final Editor editor;
    private final AssetTreeModel model;
    private final JTree tree;
    private final JLabel statusLabel;

    private int loadVersion = 0;

    public AssetExplorerTab(Editor editor) {
        super(new BorderLayout());

        this.editor = editor;
        this.model = new AssetTreeModel();
        this.tree = new JTree(model);
        this.statusLabel = new JLabel("No project open");

        setupUI();

        editor.addProjectListener(() ->
                SwingUtilities.invokeLater(this::refresh)
        );

        refresh();
    }

    private void setupUI() {
        JPanel toolbar = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());

        toolbar.add(refreshButton);

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION
        );

        DefaultTreeCellRenderer renderer =
                new DefaultTreeCellRenderer() {

                    @Override
                    public java.awt.Component getTreeCellRendererComponent(
                            JTree tree,
                            Object value,
                            boolean selected,
                            boolean expanded,
                            boolean leaf,
                            int row,
                            boolean hasFocus) {

                        super.getTreeCellRendererComponent(
                                tree, value, selected,
                                expanded, leaf, row, hasFocus
                        );

                        DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) value;

                        if (node.getUserObject() instanceof Path path) {
                            Path name = path.getFileName();
                            setText(name == null
                                    ? path.toString()
                                    : name.toString());
                        }

                        return this;
                    }
                };

        tree.setCellRenderer(renderer);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        int version = ++loadVersion;

        if (!editor.isProjectOpen()) {
            model.clear();
            statusLabel.setText("No project open");
            return;
        }

        Path projectFile = editor.currentProjectFile().unwrap();
        Path directory = projectFile.toAbsolutePath().getParent();

        statusLabel.setText("Loading assets...");

        new SwingWorker<AssetTreeModel, Void>() {

            @Override
            protected AssetTreeModel doInBackground()
                    throws IOException {

                AssetTreeModel loaded = new AssetTreeModel();
                loaded.load(directory);
                return loaded;
            }

            @Override
            protected void done() {
                if (version != loadVersion) {
                    return;
                }

                try {
                    AssetTreeModel loaded = get();

                    tree.setModel(loaded);
                    tree.expandRow(0);

                    statusLabel.setText(directory.toString());

                } catch (Exception ex) {
                    model.clear();
                    tree.setModel(model);
                    statusLabel.setText(
                            "Failed to load assets: "
                                    + ex.getMessage()
                    );
                }
            }
        }.execute();
    }
}
