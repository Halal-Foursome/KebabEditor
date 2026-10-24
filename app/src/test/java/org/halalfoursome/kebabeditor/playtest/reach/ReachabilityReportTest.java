package org.halalfoursome.kebabeditor.playtest.reach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.ActorSettings;
import org.halalfoursome.kebabeditor.playtest.PhysicsTestSupport;
import org.halalfoursome.kebabeditor.playtest.reach.ReachabilityReport.Status;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A small level for the default actor (jumps 1.5 m high, about 4 m across):
 * a floor with a box on it, a pillar reachable only from the box, a pit to
 * drop into, and an island too far to jump to.
 */
class ReachabilityReportTest {

    private Scene scene;
    private PlatformGraph graph;

    static SceneNode box(String name, Vec3 center, Vec3 size) {
        SceneNode node = new SceneNode(name);
        node.setTransform(new Transform(center, Quat.identity(), 1f));
        new Greybox(GreyboxShape.BOX, size, Color.GRAY).writeTo(node);
        return node;
    }

    @BeforeEach
    void setUp() throws Exception {
        scene = new Scene();
        scene.add(box("floor", new Vec3(0, -0.5f, 0), new Vec3(10, 1, 10)));
        scene.add(box("box", new Vec3(0, 0.5f, 0), new Vec3(1, 1, 1)));
        scene.add(box("pillar", new Vec3(2, 0, 0), new Vec3(1, 4, 1)));
        scene.add(box("pit", new Vec3(8, -5.5f, 0), new Vec3(4, 1, 4)));
        scene.add(box("island", new Vec3(20, -0.5f, 0), new Vec3(2, 1, 2)));
        graph = PlatformGraph.build(scene, PhysicsTestSupport.NO_ASSETS, ActorSettings.DEFAULT);
    }

    private Platform platform(String name) {
        return graph.platforms().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
    }

    private static Map<String, Status> statuses(ReachabilityReport report) {
        return report.marks().stream().collect(Collectors.toMap(m -> m.platform().name(), ReachabilityReport.Mark::status));
    }

    @Test
    void marksEveryPlatformFromTheStart() {
        ReachabilityReport report = ReachabilityReport.fromStart(graph, "start", List.of(platform("floor")), Maybe.none());

        assertEquals(Map.of(
            "floor", Status.START,
            "box", Status.REACHABLE,
            "pillar", Status.REACHABLE,
            "pit", Status.ONE_WAY,
            "island", Status.UNREACHABLE
        ), statuses(report));
        assertTrue(report.summary().getFirst().contains("4 of 5 platform(s) reachable"));
        assertTrue(report.summary().getFirst().contains("'pillar' at 2 jumps"));
    }

    @Test
    void aJumpLimitLeavesFartherPlatformsOut() {
        ReachabilityReport report = ReachabilityReport.fromStart(graph, "start", List.of(platform("floor")), Maybe.some(1));

        assertEquals(Status.UNREACHABLE, statuses(report).get("pillar"));
        assertEquals(Status.ONE_WAY, statuses(report).get("pit"));
        assertTrue(report.summary().getFirst().contains("within 1 jump"));
    }

    @Test
    void findsTheWayWithTheFewestJumps() {
        ReachabilityReport report = ReachabilityReport.between(
            graph, "start", List.of(platform("floor")), "goal", List.of(platform("pillar")), Maybe.none()
        );

        assertEquals(List.of("floor", "box", "pillar"), report.marks().stream().map(m -> m.platform().name()).toList());
        assertEquals(4, report.path().size());
        assertTrue(report.summary().get(0).contains("in 2 jumps: floor -> box -> pillar"));
        assertTrue(report.summary().get(1).contains("Back from 'goal' to 'start': 1 jump"));
    }

    @Test
    void saysWhenThereIsNoWay() {
        ReachabilityReport report = ReachabilityReport.between(
            graph, "pit", List.of(platform("pit")), "floor", List.of(platform("floor")), Maybe.none()
        );

        assertEquals(Map.of("pit", Status.START, "floor", Status.UNREACHABLE), statuses(report));
        assertTrue(report.path().isEmpty());
        assertTrue(report.summary().getFirst().contains("'floor' cannot be reached from 'pit'"));
    }

    @Test
    void walkingToATouchingPlatformCostsNoJump() throws Exception {
        scene.add(box("more floor", new Vec3(10, -0.5f, -10), new Vec3(10, 1, 10)));
        graph = PlatformGraph.build(scene, PhysicsTestSupport.NO_ASSETS, ActorSettings.DEFAULT);

        PlatformGraph.Search search = graph.search(List.of(platform("floor")), Maybe.some(0));

        assertEquals(Maybe.some(0), search.jumpsTo(platform("more floor")));
        assertTrue(search.jumpsTo(platform("box")).isNone());
    }

    @Test
    void anObjectWithoutSolidsStandsOnThePlatformUnderIt() {
        SceneNode coin = new SceneNode("coin");
        coin.setTransform(new Transform(new Vec3(0, 1.2f, 0), Quat.identity(), 1f));

        assertEquals(List.of(platform("box")), graph.platformsAt(coin));
        assertEquals(Maybe.some(platform("floor")), graph.platformUnder(new Vec3(-3, 0, 0)));
        assertTrue(graph.platformUnder(new Vec3(50, 0, 0)).isNone());
    }
}
