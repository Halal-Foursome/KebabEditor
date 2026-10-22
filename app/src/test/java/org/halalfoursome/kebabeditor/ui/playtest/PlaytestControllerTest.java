package org.halalfoursome.kebabeditor.ui.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogLevel;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.ActorSettings;
import org.halalfoursome.kebabeditor.playtest.PlaytestSession;
import org.halalfoursome.kebabeditor.project.FileProjectLoader;
import org.halalfoursome.kebabeditor.project.FileProjectWriter;
import org.halalfoursome.kebabeditor.project.ModelImporter;
import org.halalfoursome.kebabeditor.project.NoRecentProjects;
import org.halalfoursome.kebabeditor.project.ProjectManager;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.CompositeModelExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.GltfMeshExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class PlaytestControllerTest {

    @TempDir
    Path projectDir;

    private Editor editor;
    private FakePlaytestViewport viewport;
    private final List<String> notices = new ArrayList<>();
    private final LogService log = new LogService();
    private PlaytestController controller;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        editor = new Editor(
            new ProjectManager(
                new NoRecentProjects(),
                new FileProjectWriter(new GltfSceneWriter(mapper), mapper),
                new FileProjectLoader(new GltfSceneReader(mapper), mapper,
                    new CompositeModelExtractor(List.of(new GltfMeshExtractor())))
            ),
            new ModelImporter(mapper)
        );
        viewport = new FakePlaytestViewport();
        controller = new PlaytestController(editor, viewport, (title, message) -> notices.add(title), log);
    }

    private void openProjectWithFloor() throws Exception {
        editor.createProject(projectDir, "level");
        SceneNode floor = editor.placeGreybox(GreyboxShape.BOX, new Vec3(0, -0.5f, 0));
        new Greybox(GreyboxShape.BOX, new Vec3(20, 1, 20), Greybox.DEFAULT_COLOR).writeTo(floor);
        editor.selection().clear();
    }

    private PlaytestSession running() {
        return viewport.playing.unwrap();
    }

    private boolean logged(LogLevel level, String fragment) {
        return log.entries().stream().anyMatch(e -> e.level() == level && e.message().contains(fragment));
    }

    @Test
    void refusesWithoutAProject() {
        controller.start();

        assertFalse(controller.isRunning());
        assertEquals(List.of("No project open"), notices);
        assertTrue(viewport.playing.isNone());
    }

    @Test
    void refusesALevelWithNothingToStandOn() throws Exception {
        editor.createProject(projectDir, "level");

        controller.start();

        assertFalse(controller.isRunning());
        assertEquals(List.of("Nothing to stand on"), notices);
        assertTrue(logged(LogLevel.WARN, "nothing to stand on"));
    }

    @Test
    void refusesAnInvalidPlayerStart() throws Exception {
        openProjectWithFloor();
        SceneNode start = editor.spawnArchetype(BuiltinArchetypes.PLAYER_START, Vec3.zero());
        ((ArchetypeInstance) start.getArchetypes().getFirst()).set(ActorSettings.GRAVITY, -1f);

        controller.start();

        assertFalse(controller.isRunning());
        assertEquals(List.of("Invalid player start"), notices);
        assertFalse(editor.isPlaytesting());
    }

    @Test
    void startsWhereTheCameraLooksWithoutAPlayerStart() throws Exception {
        openProjectWithFloor();
        viewport.spawnPoint = new Vec3(2, 0, 3);
        int[] changes = { 0 };
        controller.addListener(() -> changes[0]++);

        controller.start();

        assertTrue(controller.isRunning());
        assertTrue(editor.isPlaytesting());
        assertTrue(viewport.navigation.unwrap() instanceof PlaytestNavigation);
        assertTrue(new Vec3(2, 0, 3).approxEquals(running().start().feet(), 1e-5f));
        assertEquals(viewport.cameraYaw, running().start().yaw());
        assertEquals(1, changes[0]);
        assertTrue(notices.isEmpty());
        assertTrue(logged(LogLevel.WARN, "no Player Start"));
        assertTrue(logged(LogLevel.INFO, "Playtest started"));
    }

    @Test
    void startsAtTheSelectedPlayerStart() throws Exception {
        openProjectWithFloor();
        editor.spawnArchetype(BuiltinArchetypes.PLAYER_START, new Vec3(-3, 0, 0));
        SceneNode chosen = editor.spawnArchetype(BuiltinArchetypes.PLAYER_START, new Vec3(3, 0, 0));
        editor.selection().select(chosen);

        controller.start();

        assertEquals(chosen, running().start().node().unwrap());
    }

    @Test
    void stoppingHandsTheViewportBackAndKeepsThePath() throws Exception {
        openProjectWithFloor();
        controller.start();
        for (int frame = 0; frame < 30; frame++) {
            running().advance(0.016, new Vec3(1, 0, 0), false);
        }

        controller.stop();

        assertFalse(controller.isRunning());
        assertFalse(editor.isPlaytesting());
        assertEquals(1, viewport.stops);
        assertEquals(1, viewport.pathRays.recordings().size());
        assertTrue(logged(LogLevel.INFO, "Playtest stopped after"));
    }

    @Test
    void stoppingBeforeMovingKeepsNoPath() throws Exception {
        openProjectWithFloor();
        controller.start();

        controller.stop();

        assertTrue(viewport.pathRays.isEmpty());
        assertTrue(logged(LogLevel.WARN, "no path to draw"));
    }

    @Test
    void toggleStartsAndStops() throws Exception {
        openProjectWithFloor();

        controller.toggle();
        assertTrue(controller.isRunning());
        controller.toggle();
        assertFalse(controller.isRunning());
    }

    @Test
    void closingTheProjectStopsThePlaytestAndDropsItsPaths() throws Exception {
        openProjectWithFloor();
        controller.start();
        running().advance(0.5, new Vec3(1, 0, 0), false);
        controller.stop();
        controller.start();

        editor.closeProject();

        assertFalse(controller.isRunning());
        assertTrue(viewport.pathRays.isEmpty());
    }

    @Test
    void simulationErrorStopsThePlaytestAndIsLogged() throws Exception {
        openProjectWithFloor();
        controller.start();

        controller.failed(new IllegalStateException("boom"));

        assertFalse(controller.isRunning());
        assertTrue(logged(LogLevel.ERROR, "error in the simulation"));
    }

    @Test
    void clearingPathRaysEmptiesThem() throws Exception {
        openProjectWithFloor();
        controller.start();
        running().advance(0.5, new Vec3(1, 0, 0), false);
        controller.stop();

        controller.clearPathRays();

        assertTrue(controller.pathRays().isEmpty());
    }
}
