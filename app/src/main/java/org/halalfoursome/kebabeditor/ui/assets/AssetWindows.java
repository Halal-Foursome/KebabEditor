package org.halalfoursome.kebabeditor.ui.assets;

import java.nio.file.Path;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import javax.swing.*;
import org.halalfoursome.kebabeditor.assets.AssetOpenHandler;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.project.*;
import org.halalfoursome.kebabeditor.scene.resolve.*;
import org.halalfoursome.kebabeditor.logging.LogService;

/** Owns background open requests and child-window lifetimes. All state is confined to the EDT. */
public final class AssetWindows implements AutoCloseable {
    private final JFrame owner;
    private final Editor editor;
    private final Map<Path, JFrame> windows = new HashMap<>();
    private final Map<Path, SwingWorker<?, ?>> loading = new HashMap<>();
    private boolean closed;

    public AssetWindows(JFrame owner, Editor editor) { this.owner = owner; this.editor = editor; }
    public AssetOpenHandler handler() { return new AssetOpenHandler(this::model, this::scene); }

    private void model(Path path) {
        ProjectAssets assets = editor.projectAssets().unwrap();
        open(path, () -> {
            try {
                ProjectAssets.requireInside(assets.root(), path);
                ModelResult result = new CachingModelResolver(new GltfMeshExtractor(), assets.root())
                    .resolve(path, path.getFileName().toString());
                if (result instanceof ModelResult.BrokenReference broken) throw new IOException(broken.detail());
                ModelResult.ResolvedModel model = (ModelResult.ResolvedModel) result;
                if (model.parts().isEmpty()) throw new IOException("This model has no drawable meshes");
                return () -> new ModelPreviewWindow(model);
            } catch (IOException e) { throw new java.io.UncheckedIOException(e); }
        });
    }
    private void scene(Path path) {
        open(path, () -> {
            try {
                Editor session = EditorSessions.open(path);
                return () -> new SceneEditorWindow(session);
            } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
        });
    }
    private void open(Path path, Supplier<Supplier<JFrame>> load) {
        if (closed || loading.containsKey(path)) return;
        JFrame existing = windows.get(path);
        if (existing != null && existing.isDisplayable()) { existing.toFront(); existing.requestFocus(); return; }
        SwingWorker<Supplier<JFrame>, Void> worker = new SwingWorker<>() {
            @Override protected Supplier<JFrame> doInBackground() { return load.get(); }
            @Override protected void done() {
                loading.remove(path);
                if (closed || isCancelled()) return;
                try {
                    JFrame window = get().get();
                    windows.put(path, window);
                    window.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override public void windowClosed(java.awt.event.WindowEvent e) { windows.remove(path, window); }
                    });
                    window.setVisible(true);
                } catch (CancellationException e) { /* Main window closed while loading. */ }
                catch (Exception | LinkageError e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    LogService.get().error("Cannot open asset " + path, cause);
                    JOptionPane.showMessageDialog(owner, "Cannot open " + path.getFileName() + ": " + cause.getMessage(),
                        "Asset loading failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        loading.put(path, worker);
        worker.execute();
    }
    @Override public void close() {
        closed = true;
        loading.values().forEach(worker -> worker.cancel(true));
        loading.clear();
        List.copyOf(windows.values()).forEach(JFrame::dispose);
        windows.clear();
    }
}
