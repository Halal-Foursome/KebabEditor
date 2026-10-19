package org.halalfoursome.kebabeditor.scene.resolve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.io.SceneReader;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference.Reason;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResult.BrokenPrefab;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResult.LoadedPrefab;
import org.halalfoursome.kebabeditor.scene.resolve.ReferenceLookup.Failed;
import org.halalfoursome.kebabeditor.scene.resolve.ReferenceLookup.Found;
import org.halalfoursome.kebabeditor.utils.Params;

public final class CachingPrefabResolver implements PrefabResolver {

    private record CacheEntry(LoadedPrefab prefab, FileTime modified) {}

    private final SceneReader reader;
    private final ArchetypeCatalog catalog;
    private final Path assetRoot;
    private final Map<Path, CacheEntry> cache = new HashMap<>();

    public CachingPrefabResolver(SceneReader reader, ArchetypeCatalog catalog, Path assetRoot) {
        this.reader = reader;
        this.catalog = catalog;
        this.assetRoot = assetRoot.toAbsolutePath().normalize();
    }

    @Override
    public synchronized PrefabResult resolve(Path scene, String relPath) {
        return switch (ReferenceLookup.locate(scene, relPath, assetRoot, "prefab")) {
            case Failed failed -> new BrokenPrefab(failed.path(), failed.reason(), failed.detail());
            case Found found -> load(found.file());
        };
    }

    @Override
    public synchronized boolean wouldCycle(Path from, Path to) {
        return reaches(normalize(to), normalize(from), new HashSet<>());
    }

    @Override
    public synchronized void invalidateAll() {
        cache.clear();
    }

    private PrefabResult load(Path file) {
        FileTime modified;

        try {
            modified = Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return new BrokenPrefab(file, Reason.UNREADABLE, "Cannot read " + file + ": " + e.getMessage());
        }

        CacheEntry cached = cache.get(file);

        if (cached != null && cached.modified().equals(modified)) {
            return cached.prefab();
        }

        try {
            LoadedPrefab prefab = new LoadedPrefab(file, reader.read(file, catalog));
            cache.put(file, new CacheEntry(prefab, modified));

            return prefab;
        } catch (SceneException | IOException e) {
            cache.remove(file);

            return new BrokenPrefab(file, Reason.UNREADABLE, e.getMessage());
        }
    }

    private boolean reaches(Path node, Path target, Set<Path> seen) {
        if (node.equals(target)) {
            return true;
        }
        if (!seen.add(node)) {
            return false;
        }

        for (Path next : referencesOf(node)) {
            if (reaches(next, target, seen)) {
                return true;
            }
        }

        return false;
    }

    private Set<Path> referencesOf(Path file) {
        Set<Path> targets = new HashSet<>();

        if (!(load(file) instanceof LoadedPrefab prefab)) {
            return targets;
        }

        for (SceneNode node : prefab.scene().traverse().toList()) {
            for (NodeArchetype archetype : node.getArchetypes()) {
                if (archetype instanceof ArchetypeInstance instance
                    && instance.archetypeId().equals(BuiltinArchetypes.PREFAB_REF)
                ) {
                    String path = Params.text(instance, "path").unwrapOr("");

                    if (ReferenceLookup.locate(file, path, assetRoot, "prefab") instanceof Found found) {
                        targets.add(found.file());
                    }
                }
            }
        }

        return targets;
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
