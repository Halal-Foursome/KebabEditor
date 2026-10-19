package org.halalfoursome.kebabeditor.model;

import java.util.Objects;
import java.util.UUID;

import org.halalfoursome.kebabeditor.model.TextureSlot.*;

public sealed interface TextureSlot
    permits
        None,
        Ref,
        Embedded,
        Missing
{
    String MISSING_IMAGE = "textures/missing.png";

    TextureSlot NONE = new None();

    public record None() implements TextureSlot {}

    public record Ref(TextureRef texture) implements TextureSlot {}

    // Image bytes stored inside the model file itself (a GLB's binary chunk or a data URI).
    // The key comes from the content, so identical images share one GL texture and a changed one gets a new texture.
    public record Embedded(String key, byte[] data) implements TextureSlot {

        public Embedded {
            Objects.requireNonNull(key, "key");
            data = data.clone();
        }

        public static Embedded of(byte[] data) {
            return new Embedded("embedded:" + UUID.nameUUIDFromBytes(data), data);
        }

        @Override
        public byte[] data() {
            return data.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Embedded embedded && key.equals(embedded.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    public record Missing(String reason) implements TextureSlot {}
}
