package org.halalfoursome.kebabeditor.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.playtest.PathRecording;
import org.halalfoursome.kebabeditor.project.ModelImporter;
import org.halalfoursome.kebabeditor.project.NoRecentProjects;
import org.halalfoursome.kebabeditor.project.ProjectManager;
import org.halalfoursome.kebabeditor.ui.playtest.FakePlaytestViewport;
import org.halalfoursome.kebabeditor.ui.playtest.PlaytestController;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class PlaytestMenuTest {

    private static final PathRecording RECORDING = new PathRecording(List.of(), List.of(), List.of(), 1f);

    private FakePlaytestViewport viewport;
    private PlaytestController controller;

    @BeforeEach
    void setUp() {
        KebabStyle.setCurrent(KebabStyle.defaultStyle());
        ObjectMapper mapper = new ObjectMapper();
        // No project is ever opened here, so no loader or writer is needed.
        Editor editor = new Editor(new ProjectManager(new NoRecentProjects(), null, null), new ModelImporter(mapper));
        viewport = new FakePlaytestViewport();
        controller = new PlaytestController(editor, viewport, (title, message) -> {}, new LogService());
    }

    private static JMenu menu(PlaytestMenu menu) {
        return (JMenu) menu.component();
    }

    @Test
    void withoutAViewportTheMenuIsDisabled() {
        assertFalse(menu(new PlaytestMenu(Maybe.none())).isEnabled());
    }

    @Test
    void runItemUsesF5() {
        JMenuItem run = menu(new PlaytestMenu(Maybe.some(controller))).getItem(0);

        assertEquals(KeyStroke.getKeyStroke("F5"), run.getAccelerator());
    }

    @Test
    void itemsFollowThePathRays() {
        JMenu menu = menu(new PlaytestMenu(Maybe.some(controller)));
        JCheckBoxMenuItem show = (JCheckBoxMenuItem) menu.getItem(2);
        JMenuItem clear = menu.getItem(3);

        assertTrue(show.isSelected());
        assertFalse(clear.isEnabled(), "nothing to clear yet");

        viewport.pathRays.add(RECORDING);
        assertTrue(clear.isEnabled());

        viewport.pathRays.setVisible(false);
        assertFalse(show.isSelected());
    }

    @Test
    void itemsChangeThePathRays() {
        JMenu menu = menu(new PlaytestMenu(Maybe.some(controller)));
        viewport.pathRays.add(RECORDING);

        ((JCheckBoxMenuItem) menu.getItem(2)).doClick();
        assertFalse(viewport.pathRays.isVisible());

        menu.getItem(3).doClick();
        assertTrue(viewport.pathRays.isEmpty());
    }
}
