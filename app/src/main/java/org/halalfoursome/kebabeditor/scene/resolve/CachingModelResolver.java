package org.halalfoursome.kebabeditor.scene.resolve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference.Reason;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.ResolvedModel;
import org.halalfoursome.kebabeditor.scene.resolve.ReferenceLookup.Failed;
import org.halalfoursome.kebabeditor.scene.resolve.ReferenceLookup.Found;

public final class CachingModelResolver implements ModelResolver {

    static final int MAX_DEPTH = 16;

    private record CacheEntry(ResolvedModel model, FileTime modified) {}

    private final ModelExtractor extractor;
    private final Path assetRoot;
    private final Map<Path, CacheEntry> cache = new HashMap<>();
    private final Deque<Path> resolving = new ArrayDeque<>();
    private final Map<Path, Set<Path>> references = new HashMap<>();

    public CachingModelResolver(ModelExtractor extractor, Path assetRoot) {
        this.extractor = extractor;
        this.assetRoot = assetRoot.toAbsolutePath().normalize();
    }

    @Override
    public synchronized ModelResult resolve(Path scene, String relPath) {
        Path file;

        switch (ReferenceLookup.locate(scene, relPath, assetRoot, "model")) {
            case Failed failed -> {
                return new BrokenReference(failed.path(), failed.reason(), failed.detail());
            }
            case Found found -> file = found.file();
        }

        if (!resolving.isEmpty()) {
            references.computeIfAbsent(resolving.peek(), key -> new HashSet<>()).add(file);
        }

        if (resolving.contains(file)) {
            return new BrokenReference(file, Reason.CYCLE, "Model refers back to itself: " + file);
        }

        if (resolving.size() >= MAX_DEPTH) {
            return new BrokenReference(file, Reason.TOO_DEEP, "References are nested deeper than " + MAX_DEPTH);
        }

        return load(file);
    }

    @Override
    public synchronized boolean wouldCycle(Path from, Path to) {
        return reaches(normalize(to), normalize(from), new HashSet<>());
    }

    private ModelResult load(Path file) {
        FileTime modified;

        try {
            modified = Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return new BrokenReference(file, Reason.UNREADABLE, "Cannot read " + file + ": " + e.getMessage());
        }

        CacheEntry cached = cache.get(file);

        if (cached != null && cached.modified().equals(modified)) {
            return cached.model();
        }

        references.remove(file);
        resolving.push(file);

        try {
            List<ModelPart> parts = extractor.extract(file, assetRoot);
            ResolvedModel model = ResolvedModel.of(file, parts);
            cache.put(file, new CacheEntry(model, modified));

            return model;
        } catch (ModelLoadException e) {
            cache.remove(file);

            return new BrokenReference(file, Reason.UNREADABLE, e.getMessage());
        } finally {
            resolving.pop();
        }
    }

    private boolean reaches(
        Path node, 
        Path target, 
        Set<Path> seen
    ) {
        if (node.equals(target)) {
            return true;
        }
        if (!seen.add(node)) {
            return false;
        }

        for (Path next : references.getOrDefault(node, Set.of())) {
            if (reaches(next, target, seen)) {
                return true;
            }
        }

        return false;
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
