package org.halalfoursome.kebabeditor.ui.assets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.halalfoursome.kebabeditor.assets.AssetOpenHandler;
import org.halalfoursome.kebabeditor.assets.AssetService;
import org.halalfoursome.kebabeditor.assets.AssetService.Entry;
import org.halalfoursome.kebabeditor.assets.AssetType;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.project.ProjectAssets;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.LucideIcon;
import org.halalfoursome.kebabeditor.utils.Maybe;

public final class AssetExplorerTab extends JPanel {

    private static final String NO_PROJECT = "No project open";
    private static final String NO_PROJECT_HINT = "Open or create a project to browse its assets.";
    private static final int TREE_ICON_SIZE = 18;
    private static final int LIST_ICON_SIZE = 22;

    private record Level(Path path, Maybe<List<Entry>> entries) {}

    private final Editor editor;
    private final AssetOpenHandler opener;
    private final JTree tree = new JTree(new DefaultMutableTreeNode(NO_PROJECT));
    private final DefaultListModel<Entry> files = new DefaultListModel<>();
    private final JList<Entry> list = new JList<>(files);
    private final JLabel status = new JLabel(NO_PROJECT_HINT);
    private final Runnable projectListener = () -> SwingUtilities.invokeLater(this::onProjectChanged);
    private final List<SwingWorker<?, ?>> workers = new ArrayList<>();
    private final Map<String, Icon> icons = new HashMap<>();
    private final WindowAdapter focusRefresh = new WindowAdapter() {
        @Override
        public void windowGainedFocus(WindowEvent event) {
            if (isShowing()) {
                refresh();
            }
        }
    };

    private AssetService service;
    private AssetTreeModel model;
    private Path selectedDirectory;
    private long revision;
    private long navigation;
    private boolean selectingTree;
    private boolean attached;

    public AssetExplorerTab(Editor editor, AssetOpenHandler opener) {
        super(new BorderLayout(0, 4));
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Create AssetExplorerTab on the EDT");
        }
        this.editor = editor;
        this.opener = opener;
        Font font = KebabStyle.getCurrent().uiFont();
        for (Component component : List.of(tree, list, status)) {
            component.setFont(font);
        }
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tree.setCellRenderer(new TreeRenderer());
        list.setCellRenderer(new ListRenderer());
        installListeners();
        layoutComponents(font);
    }

    private void installListeners() {
        tree.addTreeSelectionListener(event -> {
            if (!selectingTree && tree.getLastSelectedPathComponent() instanceof AssetTreeModel.Directory dir) {
                navigate(dir.path());
            }
        });
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                if (event.getPath().getLastPathComponent() instanceof AssetTreeModel.Directory dir) {
                    expand(dir);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {}
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int index = list.locationToIndex(event.getPoint());
                boolean doubleClick = SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2;
                if (doubleClick && index >= 0 && list.getCellBounds(index, index).contains(event.getPoint())) {
                    activate(files.get(index));
                }
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "openAsset");
        list.getActionMap().put("openAsset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (list.getSelectedValue() != null) {
                    activate(list.getSelectedValue());
                }
            }
        });
    }

    private void layoutComponents(Font font) {
        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(list), BorderLayout.CENTER);
        right.add(status, BorderLayout.SOUTH);
        right.setMinimumSize(new Dimension(140, 60));
        JScrollPane folders = new JScrollPane(tree);
        folders.setMinimumSize(new Dimension(100, 60));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, folders, right);
        split.setResizeWeight(0.25);
        split.setDividerLocation(220);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        attached = true;
        editor.addProjectListener(projectListener);
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.addWindowFocusListener(focusRefresh);
        }
        refresh();
    }

    @Override
    public void removeNotify() {
        attached = false;
        editor.removeProjectListener(projectListener);
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.removeWindowFocusListener(focusRefresh);
        }
        cancelWorkers();
        super.removeNotify();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (icons != null) {
            icons.clear();
        }
    }

    private Icon icon(LucideIcon lucide, int size, Color color) {
        return icons.computeIfAbsent(lucide + ":" + size + ":" + color.getRGB(), key -> lucide.icon(size, color));
    }

    private void cancelWorkers() {
        revision++;
        workers.forEach(worker -> worker.cancel(true));
        workers.clear();
    }

    private void onProjectChanged() {
        Maybe<ProjectAssets> current = editor.projectAssets();
        boolean unchanged = service != null
            && current.map(assets -> assets.equals(service.project())).unwrapOr(false);
        if (!unchanged) {
            refresh();
        }
    }

    public void refresh() {
        if (!attached) {
            return;
        }
        cancelWorkers();
        Path previousRoot = service == null ? null : service.project().root();
        service = editor.projectAssets().map(AssetService::new).unwrapOr(null);
        files.clear();
        if (service == null) {
            showNoProject();
            return;
        }
        Path root = service.project().root();
        model = new AssetTreeModel(root);
        tree.setModel(model);
        boolean sameProject = root.equals(previousRoot) && selectedDirectory != null;
        navigate(sameProject ? selectedDirectory : root);
    }

    private void showNoProject() {
        selectedDirectory = null;
        model = null;
        tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(NO_PROJECT)));
        setStatus(NO_PROJECT_HINT);
    }

    private void navigate(Path target) {
        if (service == null) {
            return;
        }
        AssetService requestService = service;
        AssetTreeModel requestModel = model;
        long generation = revision;
        long request = ++navigation;
        Set<Path> loaded = new HashSet<>();
        requestModel.collectLoaded(loaded);
        files.clear();
        setStatus("Loading…");
        SwingWorker<List<Level>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Level> doInBackground() throws IOException {
                return listChain(requestService, target, loaded);
            }

            @Override
            protected void done() {
                workers.remove(this);
                if (isCancelled() || generation != revision || request != navigation) {
                    return;
                }
                try {
                    showLevels(requestModel, get());
                } catch (CancellationException e) {
                    return;
                } catch (Exception e) {
                    failure(e);
                }
            }
        };
        workers.add(worker);
        worker.execute();
    }

    // A deleted or unreadable directory falls back to the project root
    private static List<Level> listChain(AssetService service, Path target, Set<Path> loaded) throws IOException {
        Path root = service.project().root();
        Path chosen = target;
        try {
            service.list(chosen);
        } catch (IOException e) {
            chosen = root;
        }
        List<Path> chain = new ArrayList<>();
        for (Path dir = chosen; dir != null && dir.startsWith(root); dir = dir.getParent()) {
            chain.addFirst(dir);
        }
        List<Level> levels = new ArrayList<>();
        for (Path dir : chain) {
            boolean needed = dir.equals(chosen) || !loaded.contains(dir);
            levels.add(new Level(dir, needed ? Maybe.some(service.list(dir)) : Maybe.none()));
        }
        return levels;
    }

    private void showLevels(AssetTreeModel treeModel, List<Level> levels) {
        AssetTreeModel.Directory node = treeModel.directoryRoot();
        selectingTree = true;
        try {
            for (Level level : levels) {
                if (!node.path().equals(level.path())) {
                    node = treeModel.child(node, level.path());
                }
                if (node == null) {
                    break;
                }
                final AssetTreeModel.Directory current = node;
                level.entries().ifSome(entries -> treeModel.populate(current, entries));
                tree.expandPath(new TreePath(node.getPath()));
            }
            if (node != null) {
                TreePath path = new TreePath(node.getPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
            }
        } finally {
            selectingTree = false;
        }
        Level last = levels.getLast();
        selectedDirectory = last.path();
        files.addAll(last.entries().unwrapOr(List.of()));
        setStatus(files.isEmpty() ? "This directory is empty." : "");
    }

    private void expand(AssetTreeModel.Directory dir) {
        if (dir.loaded() || service == null) {
            return;
        }
        AssetService requestService = service;
        AssetTreeModel requestModel = model;
        long generation = revision;
        SwingWorker<List<Entry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Entry> doInBackground() throws IOException {
                return requestService.list(dir.path());
            }

            @Override
            protected void done() {
                workers.remove(this);
                if (isCancelled() || revision != generation || model != requestModel) {
                    return;
                }
                try {
                    requestModel.populate(dir, get());
                } catch (CancellationException e) {
                    return;
                } catch (Exception e) {
                    failure(e);
                }
            }
        };
        workers.add(worker);
        worker.execute();
    }

    private void activate(Entry entry) {
        if (entry.directory()) {
            navigate(entry.path());
            return;
        }
        AssetService requestService = service;
        long generation = revision;
        SwingWorker<AssetType, Void> worker = new SwingWorker<>() {
            @Override
            protected AssetType doInBackground() throws IOException {
                return requestService.type(entry.path());
            }

            @Override
            protected void done() {
                workers.remove(this);
                if (isCancelled() || generation != revision) {
                    return;
                }
                try {
                    AssetType type = get();
                    if (type == AssetType.MODEL || type == AssetType.SCENE) {
                        opener.open(type, entry.path());
                    } else {
                        setStatus("No editor for this file type.");
                    }
                } catch (CancellationException e) {
                    return;
                } catch (Exception e) {
                    failure(e);
                }
            }
        };
        workers.add(worker);
        worker.execute();
    }

    private void setStatus(String text) {
        status.setText(text);
        status.setVisible(!text.isEmpty());
    }

    private void failure(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        setStatus("Cannot read assets: " + cause.getMessage());
        LogService.get().error("Asset Explorer: " + cause.getMessage(), cause);
    }

    public Path selectedDirectory() {
        return selectedDirectory;
    }

    public List<Path> displayedPaths() {
        return Collections.list(files.elements()).stream().map(Entry::path).toList();
    }

    private final class TreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean focus
        ) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focus);
            setFont(KebabStyle.getCurrent().uiFont());
            setIcon(icon(expanded ? LucideIcon.FOLDER_OPEN : LucideIcon.FOLDER, TREE_ICON_SIZE, getForeground()));
            return this;
        }
    }

    private final class ListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean selected,
            boolean focus
        ) {
            super.getListCellRendererComponent(list, value, index, selected, focus);
            Entry entry = (Entry) value;
            setFont(KebabStyle.getCurrent().uiFont());
            setIcon(icon(entry.type().icon, LIST_ICON_SIZE, getForeground()));
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            setToolTipText(entry.path().toString());
            return this;
        }
    }
}
