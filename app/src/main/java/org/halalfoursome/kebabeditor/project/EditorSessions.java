package org.halalfoursome.kebabeditor.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.halalfoursome.kebabeditor.editor.Editor;
import org.halalfoursome.kebabeditor.scene.io.*;
import org.halalfoursome.kebabeditor.scene.resolve.GltfMeshExtractor;

/** Each document window owns a manager, catalog, resolver caches and selection. */
public final class EditorSessions {
    private EditorSessions() { }
    /** Load CPU-side scene references before creating a viewport on the EDT. */
    public static Editor open(Path path) throws java.io.IOException, org.halalfoursome.kebabeditor.scene.error.SceneException {
        Editor editor = create();
        editor.openProject(path);
        var context = new org.halalfoursome.kebabeditor.behavior.BehaviorContext(
            editor.modelResolver().unwrap(), editor.prefabResolver().unwrap(), path);
        for (var node : editor.sceneNodes()) {
            for (var archetype : node.getArchetypes()) {
                if (archetype instanceof org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance instance) {
                    org.halalfoursome.kebabeditor.behavior.instance.BuiltinBehaviors.behaviorOf(instance.archetypeId())
                        .ifSome(behavior -> behavior.drawableObjects(node, instance, context));
                }
            }
        }
        return editor;
    }

    public static Editor create() {
        ObjectMapper mapper = new ObjectMapper();
        RecentProjects recent = new RecentProjects() {
            private final List<Path> paths = new ArrayList<>();
            public List<Path> paths() { return List.copyOf(paths); }
            public void add(Path file) { paths.remove(file); paths.addFirst(file); }
            public void remove(Path file) { paths.remove(file); }
            public void clear() { paths.clear(); }
        };
        return new Editor(new ProjectManager(recent,
            new FileProjectWriter(new GltfSceneWriter(mapper), mapper),
            new FileProjectLoader(new GltfSceneReader(mapper), mapper, new GltfMeshExtractor())),
            new ModelImporter(mapper));
    }
}
