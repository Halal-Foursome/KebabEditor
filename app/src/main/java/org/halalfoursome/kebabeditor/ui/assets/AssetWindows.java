package org.halalfoursome.kebabeditor.ui.assets;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.halalfoursome.kebabeditor.assets.AssetOpenHandler;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.project.EditorSessions;
import org.halalfoursome.kebabeditor.project.ProjectAssets;
import org.halalfoursome.kebabeditor.scene.resolve.CachingModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.GltfMeshExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.ResolvedModel;

public final class AssetWindows implements AutoCloseable {

    @FunctionalInterface
    private interface Loader {
        Supplier<JFrame> load() throws Exception;
    }

    private final JFrame owner;
    private final Editor editor;
    private final Map<Path, JFrame> windows = new HashMap<>();
    private final Map<Path, SwingWorker<?, ?>> loading = new HashMap<>();
    private boolean closed;

    public AssetWindows(JFrame owner, Editor editor) {
        this.owner = owner;
        this.editor = editor;
    }

    public AssetOpenHandler handler() {
        return new AssetOpenHandler(this::model, this::scene);
    }

    private void model(Path path) {
        ProjectAssets assets = editor.projectAssets().unwrap();
        open(path, () -> {
            ProjectAssets.requireInside(assets.root(), path);
            ResolvedModel model = resolveModel(assets, path);
            return () -> new ModelPreviewWindow(model);
        });
    }

    private void scene(Path path) {
        boolean isMainScene = editor.currentProjectFile().map(path::equals).unwrapOr(false);
        if (isMainScene) {
            owner.toFront();
            return;
        }
        open(path, () -> {
            Editor session = EditorSessions.open(path);
            return () -> new SceneEditorWindow(session);
        });
    }

    private static ResolvedModel resolveModel(ProjectAssets assets, Path path) throws IOException {
        ModelResult result = new CachingModelResolver(new GltfMeshExtractor(), assets.root())
            .resolve(path, path.getFileName().toString());
        if (!(result instanceof ResolvedModel model)) {
            throw new IOException(((ModelResult.BrokenReference) result).detail());
        }
        if (model.parts().isEmpty()) {
            throw new IOException("This model has no drawable meshes");
        }
        return model;
    }

    private void open(Path path, Loader loader) {
        if (closed || loading.containsKey(path)) {
            return;
        }
        JFrame existing = windows.get(path);
        if (existing != null && existing.isDisplayable()) {
            existing.toFront();
            existing.requestFocus();
            return;
        }
        SwingWorker<Supplier<JFrame>, Void> worker = new SwingWorker<>() {
            @Override
            protected Supplier<JFrame> doInBackground() throws Exception {
                return loader.load();
            }

            @Override
            protected void done() {
                loading.remove(path);
                if (closed || isCancelled()) {
                    return;
                }
                try {
                    show(path, get().get());
                } catch (CancellationException e) {
                    return;
                } catch (Exception | LinkageError e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    report(path, cause);
                }
            }
        };
        loading.put(path, worker);
        worker.execute();
    }

    private void show(Path path, JFrame window) {
        windows.put(path, window);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                windows.remove(path, window);
            }
        });
        window.setVisible(true);
    }

    private void report(Path path, Throwable cause) {
        LogService.get().error("Cannot open asset " + path, cause);
        JOptionPane.showMessageDialog(
            owner,
            "Cannot open " + path.getFileName() + ": " + cause.getMessage(),
            "Asset loading failed",
            JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void close() {
        closed = true;
        loading.values().forEach(worker -> worker.cancel(true));
        loading.clear();
        List.copyOf(windows.values()).forEach(JFrame::dispose);
        windows.clear();
    }
}
