package org.halalfoursome.kebabeditor.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.error.InvalidParamType;
import org.halalfoursome.kebabeditor.archetype.error.UnknownParamException;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.instance.SnapDistanceBehavior;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.project.ModelImporter;
import org.halalfoursome.kebabeditor.project.ProjectManager;
import org.halalfoursome.kebabeditor.project.RecentProjects;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResolver;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class Editor {

    private final ProjectManager projectManager;
    private final ModelImporter modelImporter;
    private final Selection selection = new Selection();
    private boolean snapping = true;

    public Editor(ProjectManager projectManager, ModelImporter modelImporter) {
        this.projectManager = projectManager;
        this.modelImporter = modelImporter;

        // Selected nodes belong to the scene that was just replaced or closed.
        projectManager.addListener(selection::clear);
    }

    public void createProject(Path dir, String name) throws SceneException, IOException {
        projectManager.create(dir, name);
    }

    public void openProject(Path file) throws SceneException, IOException {
        projectManager.open(file);
    }

    public void saveProject() throws SceneFormatException, IOException {
        projectManager.save();
    }

    public Maybe<Path> currentProjectFile() {
        return projectManager.currentFile();
    }

    public Maybe<Scene> scene() {
        return projectManager.scene();
    }

    public Selection selection() {
        return selection;
    }

    public boolean isProjectOpen() {
        return projectManager.isOpen();
    }

    public List<ArchetypeDefinition> archetypes() {
        return projectManager.archetypes();
    }

    public Maybe<ModelResolver> modelResolver() {
        return projectManager.modelResolver();
    }

    public Maybe<PrefabResolver> prefabResolver() {
        return projectManager.prefabResolver();
    }

    public boolean isBuiltinArchetype(String archetypeId) {
        return projectManager.isBuiltin(archetypeId);
    }

    public void addArchetype(ArchetypeDefinition definition) throws IOException {
        projectManager.addArchetype(definition);
    }

    public void removeArchetype(ArchetypeDefinition definition) throws IOException {
        projectManager.removeArchetype(definition);
    }

    public void saveArchetypes() throws IOException {
        projectManager.saveArchetypes();
    }

    public RecentProjects recentProjects() {
        return projectManager.recentProjects();
    }

    public void addProjectListener(Runnable listener) {
        projectManager.addListener(listener);
    }

    public SceneNode spawnGreybox(GreyboxShape shape, Vec3 base) {
        float halfHeight = Greybox.unit(shape).size().getY() / 2f;
        return placeGreybox(shape, base.add(new Vec3(0, halfHeight, 0)));
    }

    public SceneNode placeGreybox(GreyboxShape shape, Vec3 center) {
        SceneNode node = new SceneNode(shape.displayName());
        node.getTransform().setTranslation(center.copy());
        Greybox.unit(shape).writeTo(node);

        addToScene(node);
        LogService.get().info("Spawned greybox " + shape.jsonName() + " at " + center);
        return node;
    }

    public SceneNode importModel(Path modelFile, Vec3 base) throws IOException {
        Path sceneFile = projectManager.currentFile().expect("No project is open").toAbsolutePath();
        Path sceneDir = sceneFile.getParent();
        Path inside = modelImporter.importInto(sceneDir, modelFile);
        String reference = sceneDir.relativize(inside).toString().replace('\\', '/');

        ArchetypeInstance modelRef = newInstance(BuiltinArchetypes.MODEL_REF);
        setParam(modelRef, "path", reference);

        String fileName = inside.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        SceneNode node = new SceneNode(dot > 0 ? fileName.substring(0, dot) : fileName);
        node.getTransform().setTranslation(base.copy());
        node.addArchetype(modelRef);

        addToScene(node);
        LogService.get().info("Imported model " + reference + " at " + base);
        return node;
    }

    public boolean isSnapping() {
        return snapping;
    }

    public void setSnapping(boolean snapping) {
        this.snapping = snapping;
    }

    public Maybe<Float> snapStep() {
        return snapping 
            ? Maybe.some(snapDistance()) 
            : Maybe.none();
    }

    public float snapDistance() {
        return snapDistanceInstance()
            .andThen(SnapDistanceBehavior::distance)
            .unwrapOr(BuiltinArchetypes.DEFAULT_SNAP_DISTANCE);
    }

    public void setSnapDistance(float distance) {
        if (!(distance > 0) || !Float.isFinite(distance)) {
            throw new IllegalArgumentException("Snap distance must be a positive number, got " + distance);
        }

        ArchetypeInstance instance = snapDistanceInstance().unwrapOrElse(() -> {
            ArchetypeInstance created = newInstance(BuiltinArchetypes.SNAP_DISTANCE);
            SceneNode node = new SceneNode("Snap Distance");
            node.addArchetype(created);
            addToScene(node);
            return created;
        });

        setParam(instance, "distance", distance);
        LogService.get().info("Snap distance set to " + distance);
    }

    private Maybe<ArchetypeInstance> snapDistanceInstance() {
        return projectManager.scene().andThen(scene -> Maybe.fromOptional(scene.traverse()
            .flatMap(node -> node.getArchetypes().stream())
            .filter(a -> a instanceof ArchetypeInstance i && i.archetypeId().equals(BuiltinArchetypes.SNAP_DISTANCE))
            .map(ArchetypeInstance.class::cast)
            .findFirst()));
    }

    private ArchetypeInstance newInstance(String archetypeId) {
        ArchetypeCatalog catalog = projectManager.catalog().expect("No project is open");
        return new ArchetypeInstance(catalog.find(archetypeId)
            .expect("Builtin archetype '" + archetypeId + "' is not in the catalog"));
    }

    private static void setParam(ArchetypeInstance instance, String name, Object value) {
        try {
            instance.set(name, value);
        } catch (UnknownParamException | InvalidParamType e) {
            // Builtin definitions are fixed, so a mismatch here is a programming error.
            throw new IllegalStateException("Cannot set " + instance.archetypeId() + "." + name, e);
        }
    }

    private void addToScene(SceneNode node) {
        Scene scene = projectManager.scene().expect("No project is open");

        try {
            scene.add(node);
        } catch (SceneException e) {
            // A freshly created node has a unique id and no parent, so this cannot happen.
            throw new IllegalStateException("Cannot add a new node to the scene", e);
        }

        selection.select(node);
    }
}
