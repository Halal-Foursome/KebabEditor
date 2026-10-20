package org.halalfoursome.kebabeditor.ui.assets;

import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.halalfoursome.kebabeditor.assets.AssetOpenHandler;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.project.EditorSessions;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AssetExplorerTabTest {
    @TempDir Path dir;
    @Test void respondsToOpenSwitchRefreshCloseAndDetachesListeners() throws Exception {
        Editor editor = EditorSessions.create();
        editor.createProject(dir.resolve("one"), "first");
        AtomicReference<AssetExplorerTab> tab = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            KebabStyle.setCurrent(KebabStyle.defaultStyle());
            tab.set(new AssetExplorerTab(editor, new AssetOpenHandler(p -> {}, p -> {})));
            tab.get().addNotify();
        });
        try {
            waitFor(() -> dir.resolve("one").equals(tab.get().selectedDirectory()));
            editor.createProject(dir.resolve("two"), "second");
            waitFor(() -> dir.resolve("two").equals(tab.get().selectedDirectory()));
            Path asset = Files.writeString(dir.resolve("two/new.txt"), "hello");
            SwingUtilities.invokeAndWait(() -> tab.get().refresh());
            waitFor(() -> tab.get().displayedPaths().contains(asset));
            // Rapid project changes cannot let an older worker replace the last scene's listing.
            for (int i = 0; i < 4; i++) {
                editor.openProject(dir.resolve("one/first.gltf"));
                editor.openProject(dir.resolve("two/second.gltf"));
            }
            waitFor(() -> tab.get().displayedPaths().contains(asset));
            // Editor's lifecycle exposes close for the explorer integration.
            editor.closeProject();
            waitFor(() -> tab.get().selectedDirectory() == null && tab.get().displayedPaths().isEmpty());
        } finally { SwingUtilities.invokeAndWait(() -> tab.get().removeNotify()); }
        editor.openProject(dir.resolve("one/first.gltf"));
        SwingUtilities.invokeAndWait(() -> assertNull(tab.get().selectedDirectory()));
    }
    @Test void supportsBothFlatLafThemesAndStyleFont() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            javax.swing.LookAndFeel previous = javax.swing.UIManager.getLookAndFeel();
            try {
                KebabStyle.setCurrent(KebabStyle.defaultStyle());
                for (javax.swing.LookAndFeel theme : List.of(new com.formdev.flatlaf.FlatLightLaf(), new com.formdev.flatlaf.FlatDarkLaf())) {
                    javax.swing.UIManager.setLookAndFeel(theme);
                    AssetExplorerTab tab = new AssetExplorerTab(EditorSessions.create(), new AssetOpenHandler(p -> {}, p -> {}));
                    assertEquals(KebabStyle.getCurrent().uiFont(), ((javax.swing.JLabel) ((javax.swing.JPanel) ((javax.swing.JSplitPane) tab.getComponent(0)).getRightComponent()).getComponent(1)).getFont());
                    assertNotNull(javax.swing.UIManager.getColor("List.selectionBackground"));
                    assertNotNull(javax.swing.UIManager.getColor("Tree.selectionForeground"));
                    assertTrue(org.halalfoursome.kebabeditor.utils.LucideIcon.IMAGE.icon(22, java.awt.Color.GRAY).hasFound());
                    assertTrue(org.halalfoursome.kebabeditor.utils.LucideIcon.FOLDER_OPEN.icon(22, java.awt.Color.GRAY).hasFound());
                }
            } catch (javax.swing.UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
            finally {
                try { javax.swing.UIManager.setLookAndFeel(previous); }
                catch (javax.swing.UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
            }
        });
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            boolean[] done = {false};
            while (!done[0]) {
                SwingUtilities.invokeAndWait(() -> done[0] = condition.getAsBoolean());
                if (!done[0]) Thread.sleep(25);
            }
        });
    }
}
