package org.halalfoursome.kebabeditor.playtest.reach;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.Actor;
import org.halalfoursome.kebabeditor.playtest.ActorSettings;
import org.halalfoursome.kebabeditor.playtest.PlaytestLevel;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsVolume;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Which platforms of a level the actor can move between. Moves are one-way:
 * dropping off a ledge is easy, jumping back up may be impossible, so "B can
 * be reached from A" says nothing about A from B.
 *
 * Walking onto a touching platform is free; every jump or drop counts as one,
 * which is what a jump limit counts.
 */
public final class PlatformGraph {

    private final List<Platform> platforms;
    private final Map<Platform, Integer> indices = new IdentityHashMap<>();
    /** Per platform, the indices of the platforms one move away and whether that move is a jump. */
    private final List<List<Edge>> forward;
    private final List<List<Edge>> backward;

    private record Edge(int to, boolean jump) {}

    public PlatformGraph(List<Platform> platforms, JumpReach reach) {
        this.platforms = List.copyOf(platforms);
        this.forward = new ArrayList<>();
        this.backward = new ArrayList<>();

        for (int i = 0; i < this.platforms.size(); i++) {
            indices.put(this.platforms.get(i), i);
            forward.add(new ArrayList<>());
            backward.add(new ArrayList<>());
        }

        for (int from = 0; from < this.platforms.size(); from++) {
            for (int to = 0; to < this.platforms.size(); to++) {
                if (from == to) {
                    continue;
                }

                int a = from;
                int b = to;
                reach.between(this.platforms.get(a), this.platforms.get(b)).ifSome(move -> {
                    boolean jump = move == JumpReach.Move.JUMP;
                    forward.get(a).add(new Edge(b, jump));
                    backward.get(b).add(new Edge(a, jump));
                });
            }
        }
    }

    /** Every solid of the scene is a platform; triggers are not. */
    public static PlatformGraph build(Scene scene, BehaviorContext context, ActorSettings settings) {
        List<Platform> platforms = new ArrayList<>();

        for (SceneNode node : scene.traverse().toList()) {
            for (PhysicsVolume volume : PlaytestLevel.volumesOf(node, context)) {
                if (volume.role() == PhysicsVolume.Role.SOLID) {
                    volume.convexShape().ifSome(shape -> platforms.add(new Platform(node, volume.name(), shape.bounds())));
                }
            }
        }

        return new PlatformGraph(platforms, new JumpReach(settings));
    }

    public List<Platform> platforms() {
        return platforms;
    }

    /**
     * Where an object stands: its own solids, or else the platform under it
     * (an object without solids, such as a player start, stands on something).
     */
    public List<Platform> platformsAt(SceneNode node) {
        List<Platform> own = platforms.stream().filter(p -> p.node() == node).toList();
        if (!own.isEmpty()) {
            return own;
        }
        return platformUnder(node.worldMatrix().getTranslation()).map(List::of).unwrapOr(List.of());
    }

    /** The highest platform whose top is at or a little above {@code feet}. */
    public Maybe<Platform> platformUnder(Vec3 feet) {
        Platform best = null;

        for (Platform platform : platforms) {
            boolean below = platform.top() <= feet.getY() + Actor.GROUND_SNAP;
            if (below && platform.covers(feet, 0) && (best == null || platform.top() > best.top())) {
                best = platform;
            }
        }

        return Maybe.ofNullable(best);
    }

    /** Where the actor can get from {@code sources}, using at most {@code maxJumps} jumps when given. */
    public Search search(Collection<Platform> sources, Maybe<Integer> maxJumps) {
        return run(sources, maxJumps, forward);
    }

    /** The platforms the actor can get to {@code targets} from, with no jump limit. */
    public Search searchBack(Collection<Platform> targets) {
        return run(targets, Maybe.none(), backward);
    }

    // 0-1 BFS: walks cost nothing and go to the front of the queue, jumps cost one and go to the back.
    private Search run(Collection<Platform> sources, Maybe<Integer> maxJumps, List<List<Edge>> edges) {
        int[] jumps = new int[platforms.size()];
        int[] previous = new int[platforms.size()];
        Arrays.fill(jumps, Search.UNREACHED);
        Arrays.fill(previous, -1);
        int limit = maxJumps.unwrapOr(Integer.MAX_VALUE);
        Deque<Integer> queue = new ArrayDeque<>();

        for (Platform source : sources) {
            int index = indexOf(indices, source);
            jumps[index] = 0;
            queue.add(index);
        }

        while (!queue.isEmpty()) {
            int at = queue.poll();

            for (Edge edge : edges.get(at)) {
                int cost = jumps[at] + (edge.jump() ? 1 : 0);
                boolean better = jumps[edge.to()] == Search.UNREACHED || cost < jumps[edge.to()];

                if (cost <= limit && better) {
                    jumps[edge.to()] = cost;
                    previous[edge.to()] = at;
                    if (edge.jump()) {
                        queue.addLast(edge.to());
                    } else {
                        queue.addFirst(edge.to());
                    }
                }
            }
        }

        return new Search(platforms, indices, jumps, previous);
    }

    private static int indexOf(Map<Platform, Integer> indices, Platform platform) {
        Integer index = indices.get(platform);
        if (index == null) {
            throw new IllegalArgumentException("'" + platform.name() + "' is not a platform of this graph");
        }
        return index;
    }

    /** The result of one search: how many jumps each platform takes, and the way there. */
    public static final class Search {

        static final int UNREACHED = -1;

        private final List<Platform> platforms;
        private final Map<Platform, Integer> indices;
        private final int[] jumps;
        private final int[] previous;

        private Search(List<Platform> platforms, Map<Platform, Integer> indices, int[] jumps, int[] previous) {
            this.platforms = platforms;
            this.indices = indices;
            this.jumps = jumps;
            this.previous = previous;
        }

        public boolean reaches(Platform platform) {
            return jumps[indexOf(platform)] != UNREACHED;
        }

        /** Fewest jumps to get there; none when it is out of reach. */
        public Maybe<Integer> jumpsTo(Platform platform) {
            int count = jumps[indexOf(platform)];
            return count == UNREACHED ? Maybe.none() : Maybe.some(count);
        }

        /**
         * The platforms on a way with the fewest jumps, from a source to {@code platform}.
         * After {@link PlatformGraph#searchBack} the actor moves the other way, from the last platform to the first.
         */
        public Maybe<List<Platform>> pathTo(Platform platform) {
            int at = indexOf(platform);
            if (jumps[at] == UNREACHED) {
                return Maybe.none();
            }

            List<Platform> path = new ArrayList<>();
            for (; at != -1; at = previous[at]) {
                path.add(platforms.get(at));
            }
            return Maybe.some(path.reversed());
        }

        private int indexOf(Platform platform) {
            return PlatformGraph.indexOf(indices, platform);
        }
    }
}
