package org.halalfoursome.kebabeditor.scene.resolve;

import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;

// Picks the first extractor that understands the file, so formats can be added without touching callers
public final class CompositeModelExtractor implements ModelExtractor {

    private final List<ModelExtractor> extractors;

    public CompositeModelExtractor(List<ModelExtractor> extractors) {
        this.extractors = List.copyOf(extractors);
    }

    @Override
    public boolean supports(Path file) {
        return extractors.stream().anyMatch(extractor -> extractor.supports(file));
    }

    @Override
    public List<ModelPart> extract(Path file, Path assetRoot) throws ModelLoadException {
        for (ModelExtractor extractor : extractors) {
            if (extractor.supports(file)) {
                return extractor.extract(file, assetRoot);
            }
        }

        throw new ModelLoadException("Unsupported model format: " + file.getFileName());
    }
}
