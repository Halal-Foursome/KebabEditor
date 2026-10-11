package org.halalfoursome.kebabeditor.scene.resolve;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.*;

public sealed interface ModelResult 
    permits 
        ResolvedModel, 
        BrokenReference 
{
    Path path();

    record ResolvedModel(
        Path path, 
        List<ModelPart> parts, 
        Aabb bounds
    ) implements ModelResult {

        public ResolvedModel {
            Objects.requireNonNull(path, "path");
            parts = List.copyOf(parts);
            bounds = bounds.copy();
        }

        public static ResolvedModel of(Path path, List<ModelPart> parts) {
            Aabb bounds = null;

            for (ModelPart part : parts) {
                Aabb partBounds = part.mesh().bounds().transformed(part.transform());
                bounds = bounds == null ? partBounds : bounds.union(partBounds);
            }

            return new ResolvedModel(path, parts, bounds == null ? new Aabb() : bounds);
        }

        @Override
        public Aabb bounds() {
            return bounds.copy();
        }
    }

    record BrokenReference(
        Path path, 
        Reason reason, 
        String detail
    ) implements ModelResult {

        public enum Reason {
            MISSING,
            CYCLE,
            TOO_DEEP,
            UNREADABLE,
        }

        public BrokenReference {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
        }
    }
}
