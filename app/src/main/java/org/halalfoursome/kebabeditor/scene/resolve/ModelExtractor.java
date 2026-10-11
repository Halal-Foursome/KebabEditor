package org.halalfoursome.kebabeditor.scene.resolve;

import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;

public interface ModelExtractor {

    boolean supports(Path file);

    // assetRoot is the folder the model's own files may not escape
    List<ModelPart> extract(Path file, Path assetRoot) throws ModelLoadException;
}
