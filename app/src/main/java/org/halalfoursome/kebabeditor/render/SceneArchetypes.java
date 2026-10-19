package org.halalfoursome.kebabeditor.render;

import java.util.stream.Stream;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.ArchetypeBehavior;
import org.halalfoursome.kebabeditor.behavior.instance.BuiltinBehaviors;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;

/**
 * Every archetype instance in a scene that has a behavior, i.e. that knows how
 * to draw itself. Shared by drawing and picking so they always agree.
 */
final class SceneArchetypes {

    record Entry(SceneNode node, ArchetypeInstance archetype, ArchetypeBehavior behavior) {}

    private SceneArchetypes() {}

    static Stream<Entry> of(Scene scene) {
        return scene.traverse().flatMap(node -> node.getArchetypes().stream()
            .filter(ArchetypeInstance.class::isInstance)
            .map(ArchetypeInstance.class::cast)
            .flatMap(archetype -> BuiltinBehaviors.behaviorOf(archetype.archetypeId())
                .mapOr(Stream.<Entry>empty(), behavior -> Stream.of(new Entry(node, archetype, behavior)))));
    }
}
