package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.instance.BuiltinBehaviors;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.halalfoursome.kebabeditor.scene.resolve.CompositeModelExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.GltfMeshExtractor;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class EditorSessions {

    private EditorSessions() {}

    // Loads CPU-side scene references up front so the viewport does not do file IO on the EDT
    public static Editor open(Path path) throws IOException, SceneException {
        Editor editor = create(new NoRecentProjects());
        editor.openProject(path);
        BehaviorContext context = new BehaviorContext(
            editor.modelResolver().unwrap(),
            editor.prefabResolver().unwrap(),
            path
        );
        for (var node : editor.sceneNodes()) {
            for (var archetype : node.getArchetypes()) {
                if (archetype instanceof ArchetypeInstance instance) {
                    BuiltinBehaviors.behaviorOf(instance.archetypeId())
                        .ifSome(behavior -> behavior.drawableObjects(node, instance, context));
                }
            }
        }
        return editor;
    }

    public static Editor create() {
        return create(new NoRecentProjects());
    }

    public static Editor create(RecentProjects recent) {
        ObjectMapper mapper = new ObjectMapper();
        ProjectManager manager = new ProjectManager(
            recent,
            new FileProjectWriter(new GltfSceneWriter(mapper), mapper),
            new FileProjectLoader(
                new GltfSceneReader(mapper),
                mapper,
                new CompositeModelExtractor(List.of(new GltfMeshExtractor()))
            )
        );
        return new Editor(manager, new ModelImporter(mapper));
    }
}
