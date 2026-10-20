package org.halalfoursome.kebabeditor.ui.assets;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import org.halalfoursome.kebabeditor.assets.*;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.utils.*;

public final class AssetExplorerTab extends JPanel {
    private final Editor editor;
    private final AssetOpenHandler opener;
    private final JTree tree = new JTree(new DefaultMutableTreeNode("No project open"));
    private final DefaultListModel<AssetService.Entry> files = new DefaultListModel<>();
    private final JList<AssetService.Entry> list = new JList<>(files);
    private final JLabel location = new JLabel("No project open");
    private final JLabel status = new JLabel("Open or create a project to browse its assets.");
    private final Runnable projectListener = () -> {
        if (SwingUtilities.isEventDispatchThread()) refresh();
        else SwingUtilities.invokeLater(this::refresh);
    };
    private final List<SwingWorker<?, ?>> workers = new ArrayList<>();
    private AssetService service;
    private AssetTreeModel model;
    private Path selectedDirectory;
    private long revision;
    private long navigation;
    private boolean selectingTree;
    private boolean attached;

    public AssetExplorerTab(Editor editor, AssetOpenHandler opener) {
        super(new BorderLayout(0, 4));
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Create AssetExplorerTab on the EDT");
        this.editor = editor;
        this.opener = opener;
        Font font = KebabStyle.getCurrent().uiFont();
        tree.setFont(font);
        list.setFont(font);
        location.setFont(font);
        status.setFont(font);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected,
                    boolean expanded, boolean leaf, int row, boolean focus) {
                super.getTreeCellRendererComponent(t, value, selected, expanded, leaf, row, focus);
                setFont(KebabStyle.getCurrent().uiFont());
                setIcon((expanded ? LucideIcon.FOLDER_OPEN : LucideIcon.FOLDER).icon(18, getForeground()));
                return this;
            }
        });
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(l, value, index, selected, focus);
                AssetService.Entry entry = (AssetService.Entry) value;
                setFont(KebabStyle.getCurrent().uiFont());
                setIcon(entry.type().icon.icon(22, getForeground()));
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                setToolTipText(entry.path().toString());
                return this;
            }
        });
        tree.addTreeSelectionListener(e -> {
            if (!selectingTree && tree.getLastSelectedPathComponent() instanceof AssetTreeModel.Directory dir) navigate(dir.path);
        });
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override public void treeWillExpand(TreeExpansionEvent e) {
                if (e.getPath().getLastPathComponent() instanceof AssetTreeModel.Directory dir) expand(dir);
            }
            @Override public void treeWillCollapse(TreeExpansionEvent e) { }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && index >= 0
                        && list.getCellBounds(index, index).contains(e.getPoint())) activate(files.get(index));
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "openAsset");
        list.getActionMap().put("openAsset", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (list.getSelectedValue() != null) activate(list.getSelectedValue()); }
        });
        JButton refresh = new JButton("Refresh");
        refresh.setFont(font);
        refresh.addActionListener(e -> refresh());
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.add(location, BorderLayout.CENTER);
        toolbar.add(refresh, BorderLayout.EAST);
        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(list), BorderLayout.CENTER);
        right.add(status, BorderLayout.SOUTH);
        JScrollPane folders = new JScrollPane(tree);
        folders.setMinimumSize(new Dimension(100, 60));
        right.setMinimumSize(new Dimension(140, 60));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, folders, right);
        split.setResizeWeight(0.25);
        split.setDividerLocation(220);
        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    @Override public void addNotify() {
        super.addNotify();
        attached = true;
        editor.addProjectListener(projectListener);
        refresh();
    }
    @Override public void removeNotify() {
        attached = false;
        editor.removeProjectListener(projectListener);
        cancelWorkers();
        super.removeNotify();
    }
    private void cancelWorkers() {
        revision++;
        workers.forEach(w -> w.cancel(true));
        workers.clear();
    }

    public void refresh() {
        if (!attached) return;
        cancelWorkers();
        Path previousRoot = service == null ? null : service.project().root();
        service = editor.projectAssets().map(AssetService::new).unwrapOr(null);
        files.clear();
        if (service == null) {
            selectedDirectory = null;
            tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("No project open")));
            location.setText("No project open");
            status.setText("Open or create a project to browse its assets.");
            return;
        }
        model = new AssetTreeModel(service.project().root());
        tree.setModel(model);
        Path target = service.project().root().equals(previousRoot) && selectedDirectory != null
            ? selectedDirectory : service.project().root();
        navigate(target);
    }

    private record Level(Path path, List<AssetService.Entry> entries) { }
    private void navigate(Path target) {
        if (service == null) return;
        AssetService requestService = service;
        long generation = revision;
        long request = ++navigation;
        files.clear();
        status.setText("Loading…");
        SwingWorker<List<Level>, Void> worker = new SwingWorker<>() {
            @Override protected List<Level> doInBackground() throws Exception {
                Path root = requestService.project().root();
                Path chosen = target;
                // A deleted or inaccessible directory falls back to the project root on refresh.
                try { requestService.list(chosen); }
                catch (java.io.IOException e) { chosen = root; }
                List<Path> chain = new ArrayList<>();
                for (Path dir = chosen; dir != null && dir.startsWith(root); dir = dir.getParent()) {
                    chain.addFirst(dir);
                    if (dir.equals(root)) break;
                }
                List<Level> levels = new ArrayList<>();
                for (Path dir : chain) levels.add(new Level(dir, requestService.list(dir)));
                return levels;
            }
            @Override protected void done() {
                workers.remove(this);
                if (isCancelled() || generation != revision || request != navigation) return;
                try {
                    List<Level> levels = get();
                    AssetTreeModel.Directory node = model.directoryRoot();
                    selectingTree = true;
                    try {
                        for (Level level : levels) {
                            if (!node.path.equals(level.path())) node = model.child(node, level.path());
                            if (node == null) break;
                            model.populate(node, level.entries());
                            tree.expandPath(new TreePath(node.getPath()));
                        }
                        if (node != null) {
                            TreePath path = new TreePath(node.getPath());
                            tree.setSelectionPath(path);
                            tree.scrollPathToVisible(path);
                        }
                    } finally { selectingTree = false; }
                    Level last = levels.getLast();
                    selectedDirectory = last.path();
                    files.addAll(last.entries());
                    String relative = service.project().root().relativize(selectedDirectory).toString();
                    location.setText(relative.isEmpty() ? service.project().root().getFileName().toString() : relative);
                    status.setText(files.isEmpty() ? "This directory is empty." : files.size() + " items");
                } catch (CancellationException e) { /* Component was detached. */ }
                catch (Exception e) { failure(e); }
            }
        };
        workers.add(worker);
        worker.execute();
    }

    private void expand(AssetTreeModel.Directory dir) {
        if (dir.loaded || service == null) return;
        AssetService requestService = service;
        AssetTreeModel requestModel = model;
        long generation = revision;
        SwingWorker<List<AssetService.Entry>, Void> worker = new SwingWorker<>() {
            @Override protected List<AssetService.Entry> doInBackground() throws Exception { return requestService.list(dir.path); }
            @Override protected void done() {
                workers.remove(this);
                if (isCancelled() || revision != generation || model != requestModel) return;
                try { model.populate(dir, get()); }
                catch (Exception e) { failure(e); }
            }
        };
        workers.add(worker);
        worker.execute();
    }

    private void activate(AssetService.Entry entry) {
        if (entry.directory()) navigate(entry.path());
        else {
            AssetService requestService = service;
            // Revalidate paths in the background, including symlinks changed since listing.
            long generation = revision;
            SwingWorker<AssetType, Void> worker = new SwingWorker<>() {
                @Override protected AssetType doInBackground() throws Exception { return requestService.type(entry.path()); }
                @Override protected void done() {
                    workers.remove(this);
                    if (isCancelled() || generation != revision) return;
                    try {
                        // Dispatch runs on the EDT; the window handlers do their loading in workers.
                        AssetType type = get();
                        if (type == AssetType.MODEL || type == AssetType.SCENE) opener.open(type, entry.path());
                        else status.setText("No editor for this file type.");
                    } catch (Exception e) { failure(e); }
                }
            };
            workers.add(worker);
            worker.execute();
        }
    }
    private void failure(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        status.setText("Cannot read assets: " + cause.getMessage());
        LogService.get().error("Asset Explorer: " + cause.getMessage(), cause);
    }

    // Also useful for UI integration tests, without an OpenGL viewport.
    public Path selectedDirectory() { return selectedDirectory; }
    public List<Path> displayedPaths() { return java.util.Collections.list(files.elements()).stream().map(AssetService.Entry::path).toList(); }
}
