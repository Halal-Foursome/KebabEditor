package org.halalfoursome.kebabeditor.playtest.reach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * The outcome of one reachability check, as the viewport shows it and the log tells it.
 *
 * @param marks   platforms to highlight
 * @param path    the way between two objects, as take-off and landing points; empty when there is none
 * @param summary lines for the log
 */
public record ReachabilityReport(List<Mark> marks, List<Vec3> path, List<String> summary) {

    /** Lists of names are cut short after this many. */
    static final int MAX_NAMES = 10;

    public enum Status {
        /** Where the check starts from. */
        START,
        /** Reachable, and the actor can get back. */
        REACHABLE,
        /** Reachable, but there is no way back: a player who goes there is stuck. */
        ONE_WAY,
        UNREACHABLE,
    }

    public record Mark(Platform platform, Status status) {

        public Mark {
            Objects.requireNonNull(platform, "platform");
            Objects.requireNonNull(status, "status");
        }
    }

    public ReachabilityReport {
        marks = List.copyOf(marks);
        path = path.stream().map(Vec3::copy).toList();
        summary = List.copyOf(summary);
    }

    @Override
    public List<Vec3> path() {
        return path.stream().map(Vec3::copy).toList();
    }

    /** Every platform of the level, seen from where the actor starts. */
    public static ReachabilityReport fromStart(
        PlatformGraph graph,
        String startName,
        Collection<Platform> start,
        Maybe<Integer> maxJumps
    ) {
        Set<Platform> starts = identitySet(start);
        PlatformGraph.Search out = graph.search(start, maxJumps);
        PlatformGraph.Search back = graph.searchBack(start);

        List<Mark> marks = new ArrayList<>();
        List<Platform> unreachable = new ArrayList<>();
        List<Platform> oneWay = new ArrayList<>();
        Maybe<Platform> farthest = Maybe.none();

        for (Platform platform : graph.platforms()) {
            Status status;
            if (starts.contains(platform)) {
                status = Status.START;
            } else if (!out.reaches(platform)) {
                status = Status.UNREACHABLE;
                unreachable.add(platform);
            } else if (back.reaches(platform)) {
                status = Status.REACHABLE;
            } else {
                status = Status.ONE_WAY;
                oneWay.add(platform);
            }
            marks.add(new Mark(platform, status));

            if (out.reaches(platform)
                && farthest.mapOr(true, f -> out.jumpsTo(platform).unwrap() > out.jumpsTo(f).unwrap())) {
                farthest = Maybe.some(platform);
            }
        }

        List<String> summary = new ArrayList<>();
        int reached = graph.platforms().size() - unreachable.size();
        String line = "Reachability from '" + startName + "'" + within(maxJumps) + ": "
            + reached + " of " + graph.platforms().size() + " platform(s) reachable";
        if (farthest.isSomeAnd(f -> out.jumpsTo(f).unwrap() > 0)) {
            Platform f = farthest.unwrap();
            line += ", the farthest is '" + f.name() + "' at " + jumps(out.jumpsTo(f).unwrap());
        }
        summary.add(line + ".");

        if (!unreachable.isEmpty()) {
            summary.add("Unreachable (red): " + names(unreachable) + ".");
        }
        if (!oneWay.isEmpty()) {
            summary.add("One-way (yellow), no way back to '" + startName + "': " + names(oneWay) + ".");
        }

        return new ReachabilityReport(marks, List.of(), summary);
    }

    /** The way with the fewest jumps from one object to another, and whether the actor can get back. */
    public static ReachabilityReport between(
        PlatformGraph graph,
        String fromName,
        Collection<Platform> from,
        String toName,
        Collection<Platform> to,
        Maybe<Integer> maxJumps
    ) {
        PlatformGraph.Search out = graph.search(from, maxJumps);
        Maybe<Platform> target = nearest(out, to);
        List<String> summary = new ArrayList<>();
        List<Mark> marks = new ArrayList<>();

        if (target.isNone()) {
            Set<Platform> starts = identitySet(from);
            Set<Platform> targets = identitySet(to);
            int reached = 0;

            for (Platform platform : graph.platforms()) {
                if (starts.contains(platform)) {
                    marks.add(new Mark(platform, Status.START));
                } else if (targets.contains(platform)) {
                    marks.add(new Mark(platform, Status.UNREACHABLE));
                } else if (out.reaches(platform)) {
                    marks.add(new Mark(platform, Status.REACHABLE));
                    reached++;
                }
            }

            summary.add("'" + toName + "' cannot be reached from '" + fromName + "'" + within(maxJumps) + ". "
                + "The " + reached + " other platform(s) the actor can get to are green.");
            return new ReachabilityReport(marks, List.of(), summary);
        }

        List<Platform> way = out.pathTo(target.unwrap()).unwrap();
        int jumpCount = out.jumpsTo(target.unwrap()).unwrap();

        for (int i = 0; i < way.size(); i++) {
            marks.add(new Mark(way.get(i), i == 0 ? Status.START : Status.REACHABLE));
        }

        String route = way.stream().map(Platform::name).collect(Collectors.joining(" -> "));
        summary.add(jumpCount == 0
            ? "'" + toName + "' can be reached from '" + fromName + "' without jumping: " + route + "."
            : "'" + toName + "' can be reached from '" + fromName + "' in " + jumps(jumpCount) + ": " + route + ".");

        PlatformGraph.Search back = graph.search(to, Maybe.none());
        Maybe<Platform> home = nearest(back, from);
        summary.add(home.isSome()
            ? "Back from '" + toName + "' to '" + fromName + "': " + jumps(back.jumpsTo(home.unwrap()).unwrap()) + "."
            : "There is no way back from '" + toName + "' to '" + fromName + "'.");

        return new ReachabilityReport(marks, takeoffsAndLandings(way), summary);
    }

    // The reached platform that takes the fewest jumps.
    private static Maybe<Platform> nearest(PlatformGraph.Search search, Collection<Platform> candidates) {
        Maybe<Platform> best = Maybe.none();

        for (Platform platform : candidates) {
            if (search.reaches(platform)
                && best.mapOr(true, b -> search.jumpsTo(platform).unwrap() < search.jumpsTo(b).unwrap())) {
                best = Maybe.some(platform);
            }
        }

        return best;
    }

    // Each move leaves from the edge nearest the next platform and lands on the nearest point of it.
    private static List<Vec3> takeoffsAndLandings(List<Platform> way) {
        List<Vec3> points = new ArrayList<>();

        for (int i = 0; i + 1 < way.size(); i++) {
            Vec3 takeoff = way.get(i).topPointNear(way.get(i + 1).topCenter());
            points.add(takeoff);
            points.add(way.get(i + 1).topPointNear(takeoff));
        }

        return points;
    }

    private static Set<Platform> identitySet(Collection<Platform> platforms) {
        Set<Platform> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(platforms);
        return set;
    }

    private static String within(Maybe<Integer> maxJumps) {
        return maxJumps.mapOr("", max -> " within " + jumps(max));
    }

    static String jumps(int count) {
        return count == 1 ? "1 jump" : count + " jumps";
    }

    static String names(List<Platform> platforms) {
        String shown = platforms.stream()
            .limit(MAX_NAMES)
            .map(p -> "'" + p.name() + "'")
            .collect(Collectors.joining(", "));
        int rest = platforms.size() - MAX_NAMES;
        return rest > 0 ? shown + " and " + rest + " more" : shown;
    }
}
