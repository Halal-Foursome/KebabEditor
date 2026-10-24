package org.halalfoursome.kebabeditor.ui.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.logging.LogLevel;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.reach.ReachabilityReport;
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
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class ReachabilityCheckTest {

    @TempDir
    Path projectDir;

    private Editor editor;
    private FakePlaytestViewport viewport;
    private final LogService log = new LogService();
    private ReachabilityCheck check;

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
        // Updates run at once instead of after a pause.
        check = new ReachabilityCheck(editor, viewport, log, update -> update);
    }

    private SceneNode box(Vec3 center, Vec3 size) {
        SceneNode node = editor.placeGreybox(GreyboxShape.BOX, center);
        new Greybox(GreyboxShape.BOX, size, Greybox.DEFAULT_COLOR).writeTo(node);
        return node;
    }

    // A floor with a ledge too high to jump onto.
    private SceneNode openLevel() throws Exception {
        editor.createProject(projectDir, "level");
        box(new Vec3(0, -0.5f, 0), new Vec3(20, 1, 20));
        SceneNode ledge = box(new Vec3(5, 2, 0), new Vec3(2, 4, 2));
        ledge.setName("ledge");
        editor.spawnArchetype(BuiltinArchetypes.PLAYER_START, Vec3.zero());
        editor.selection().clear();
        return ledge;
    }

    private ReachabilityReport report() {
        return viewport.reachabilityMarks.report().unwrap();
    }

    private long logged(LogLevel level, String fragment) {
        return log.entries().stream().filter(e -> e.level() == level && e.message().contains(fragment)).count();
    }

    private ReachabilityReport.Status statusOf(String name) {
        return report().marks().stream()
            .filter(m -> m.platform().name().equals(name))
            .findFirst()
            .orElseThrow()
            .status();
    }

    @Test
    void isOffUntilEnabled() throws Exception {
        openLevel();
        editor.sceneChanged();

        assertFalse(check.isEnabled());
        assertTrue(viewport.reachabilityMarks.isEmpty());
    }

    @Test
    void showsNothingWithoutAProject() {
        check.setEnabled(true);

        assertTrue(viewport.reachabilityMarks.isEmpty());
    }

    @Test
    void checksTheLevelFromThePlayerStartOnceEnabled() throws Exception {
        openLevel();

        check.setEnabled(true);

        assertEquals(ReachabilityReport.Status.UNREACHABLE, statusOf("ledge"));
        assertEquals(1, logged(LogLevel.INFO, "Unreachable (red): 'ledge'"));
        assertEquals(1, logged(LogLevel.INFO, "walls and ceilings in the way are not checked"));
    }

    @Test
    void followsSceneEdits() throws Exception {
        SceneNode ledge = openLevel();
        check.setEnabled(true);

        // Lowered until its top is 1 m above the floor.
        ledge.getTransform().setTranslation(new Vec3(5, -1, 0));
        editor.sceneChanged();

        assertEquals(ReachabilityReport.Status.REACHABLE, statusOf("ledge"));
    }

    @Test
    void logsOnlyWhenTheOutcomeChanges() throws Exception {
        openLevel();
        check.setEnabled(true);

        editor.sceneChanged();
        editor.sceneChanged();

        assertEquals(1, logged(LogLevel.INFO, "Unreachable (red): 'ledge'"));
    }

    @Test
    void warnsOnceAboutALevelWithoutAPlayerStart() throws Exception {
        editor.createProject(projectDir, "level");
        box(new Vec3(0, -0.5f, 0), new Vec3(20, 1, 20));
        check.setEnabled(true);

        editor.sceneChanged();

        assertTrue(viewport.reachabilityMarks.isEmpty());
        assertEquals(1, logged(LogLevel.WARN, "no player start"));
    }

    @Test
    void twoSelectedObjectsShowTheWayBetweenThem() throws Exception {
        SceneNode ledge = openLevel();
        SceneNode start = editor.nodesUsing(BuiltinArchetypes.PLAYER_START).getFirst();
        check.setEnabled(true);

        editor.selection().set(List.of(ledge, start));

        assertTrue(report().summary().getFirst().contains("'" + start.getName() + "' can be reached from 'ledge'"));
        assertFalse(report().path().isEmpty());

        editor.selection().clear();

        assertTrue(report().path().isEmpty());
        assertEquals(ReachabilityReport.Status.UNREACHABLE, statusOf("ledge"));
    }

    @Test
    void aNewJumpLimitUpdatesTheCheck() throws Exception {
        openLevel();
        box(new Vec3(-5, 0.5f, 0), new Vec3(2, 1, 2)).setName("step");
        editor.selection().clear();
        check.setEnabled(true);
        assertEquals(ReachabilityReport.Status.REACHABLE, statusOf("step"));

        check.setJumpLimit(Maybe.some(0));

        assertEquals(ReachabilityReport.Status.UNREACHABLE, statusOf("step"));
    }

    @Test
    void disablingClearsTheMarks() throws Exception {
        openLevel();
        check.setEnabled(true);

        check.setEnabled(false);
        editor.sceneChanged();

        assertTrue(viewport.reachabilityMarks.isEmpty());
    }
}
