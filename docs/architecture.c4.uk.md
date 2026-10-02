# C4 · Рівень 4 (Код)

Сірі пунктирні класи — заплановані, решта є в коді.

## 1. Project

```mermaid
classDiagram
    direction LR
    class Editor {
        +createProject(Path, String)
        +openProject(Path)
        +saveProject()
        +currentProjectFile() Maybe~Path~
        +isProjectOpen() boolean
        +archetypes() List~ArchetypeDefinition~
        +addArchetype(ArchetypeDefinition)
        +removeArchetype(ArchetypeDefinition)
        +saveArchetypes()
        +recentProjects() RecentProjects
        +addProjectListener(Runnable)
    }

    class ProjectManager {
        -listeners List~Runnable~
        -repository Maybe~ProjectRepository~
        +create(Path, String)
        +open(Path)
        +close()
        +save()
        +archetypes() List~ArchetypeDefinition~
        +addArchetype(ArchetypeDefinition)
        +removeArchetype(ArchetypeDefinition)
        +saveArchetypes()
        +addListener(Runnable)
    }

    class ProjectRepository {
        <<record>>
        Path filePath
        String currentSceneData
        ArchetypeRegistry archetypeRegistry
    }

    class ProjectLoader {
        +load(Path) ProjectRepository
    }

    class ProjectWriter {
        +createProject(Path, String) Path
        +saveScene(Path, String)
        +saveArchetypes(Path, ArchetypeRegistry)
    }

    class ProjectTemplates {
        +MINIMAL_GLTF$
        +EMPTY_ARCHETYPES$
        +archetypesFileFor(Path)$ Path
    }

    class RecentProjects {
        +paths() List~Path~
        +add(Path)
        +remove(Path)
        +clear()
    }

    class ArchetypeRegistry
    class ArchetypeRegistryJson

    Editor --> ProjectManager
    ProjectManager --> RecentProjects
    ProjectManager --> ProjectLoader
    ProjectManager --> ProjectWriter
    ProjectManager o-- "0..1" ProjectRepository
    ProjectRepository --> ArchetypeRegistry
    ProjectLoader ..> ProjectRepository : створює
    ProjectLoader ..> ProjectTemplates
    ProjectWriter ..> ProjectTemplates
    ProjectLoader ..> ArchetypeRegistryJson
    ProjectWriter ..> ArchetypeRegistryJson
```

## 2. Archetype

```mermaid
classDiagram
    direction LR
    class ArchetypeRegistry {
        -archetypes Set~ArchetypeDefinition~
        +add(ArchetypeDefinition)
        +remove(ArchetypeDefinition)
    }

    class ArchetypeDefinition {
        -id String
        -displayName String
        -icon Maybe~ArchetypeIcon~
        -params Map~String, ParamType~
        +duplicateAs(String) ArchetypeDefinition
    }

    class ArchetypeInstance {
        -values Map~String, Object~
        +get(String) Object
        +set(String, Object)
    }

    class ParamType {
        <<enumeration>>
        INT
        FLOAT
        BOOL
        STRING
        REL_PATH
        VEC3
        NODE_REF
        +javaClass() Class
        +defaultValue() Object
    }

    class ArchetypeIcon {
        <<sealed interface>>
    }

    class Handle {
        <<record>>
        String path
        +loaded() Maybe~Loaded~
    }

    class Loaded {
        <<record>>
        BufferedImage icon
    }

    class ArchetypeRegistryJson {
        <<record>>
        +from(ArchetypeRegistry)$
        +toRegistry() ArchetypeRegistry
    }

    class ArchetypeDefinitionJson {
        <<record>>
        +from(ArchetypeDefinition)$
        +toDefinition() ArchetypeDefinition
    }

    class InvalidParamType

    class UnknownParamException

    class Maybe~T~
    class Vec3

    ArchetypeRegistry o-- "*" ArchetypeDefinition
    ArchetypeDefinition --> "*" ParamType
    ArchetypeDefinition --> ArchetypeIcon
    ArchetypeIcon <|.. Handle
    ArchetypeIcon <|.. Loaded
    Handle ..> Loaded : loaded()
    ArchetypeInstance --> ArchetypeDefinition
    ArchetypeInstance ..> InvalidParamType : кидає
    ArchetypeInstance ..> UnknownParamException : кидає
    ParamType ..> Vec3
    ParamType ..> Maybe : NODE_REF
    ArchetypeRegistryJson o-- ArchetypeDefinitionJson
    ArchetypeDefinitionJson ..> ArchetypeDefinition
```

## 3. Common (утиліти, math)

```mermaid
classDiagram
    direction LR
    class Maybe~T~ {
        <<sealed interface>>
    }
    class Vec3
    class Aabb
    class KebabStyle {
        <<record>>
    }
    class AssetLoader
    class FavIcon
    class FileChooser
    class ErrorDialogs
    class LucideIcon {
        <<enumeration>>
    }
    AssetLoader ..> FavIcon
```

## 4. UI: вікно, меню, навігація

```mermaid
classDiagram
    direction TB
    class KebabApplication {
        +main(String[])$
    }

    class MainWindow {
        -frame JFrame
        +show()
    }

    class EditorView {
        <<JPanel>>
        +setNavigationMode(NavigationMode)
    }

    class Camera {
        +x, y, z
        +yaw, pitch, fov
    }

    class NavigationMode {
        <<interface>>
        +attach(EditorView)
        +detach()
        +update(double)
    }

    class FreeFlightNavigation

    class MenuBar {
        <<JMenuBar>>
        +addMenu(Menu)
    }

    class MenuItem {
        <<interface>>
        +component() JComponent
    }

    class Menu

    class SingleItem

    class SeparatorItem

    class MenuButton

    class FileMenu

    class ProjectMenu

    class ViewMenu

    class OptionsMenu

    class CreateProjectDialog

    class OptionsDialog

    class Editor
    class ArchetypeManagerDialog
    class FileChooser
    class ErrorDialogs

    KebabApplication ..> MainWindow : створює
    KebabApplication ..> Editor : зв'язує
    MainWindow --> Editor
    MainWindow --> MenuBar
    MainWindow ..> EditorView : закоментовано
    EditorView *-- Camera
    EditorView --> NavigationMode
    NavigationMode <|.. FreeFlightNavigation
    MenuBar o-- Menu
    MenuItem <|.. Menu
    MenuItem <|.. SingleItem
    MenuItem <|.. SeparatorItem
    Menu <|-- FileMenu
    Menu <|-- ProjectMenu
    Menu <|-- ViewMenu
    Menu <|-- OptionsMenu
    Menu o-- MenuItem
    FileMenu --> Editor
    ProjectMenu --> Editor
    FileMenu ..> CreateProjectDialog
    FileMenu ..> FileChooser
    FileMenu ..> ErrorDialogs
    OptionsMenu ..> OptionsDialog
    ProjectMenu ..> ArchetypeManagerDialog
    CreateProjectDialog --> Editor
```

## 5. UI: діалоги архетипів і валідація

```mermaid
classDiagram
    direction TB
    class ArchetypeManagerDialog

    class AbstractArchetypeDialog {
        <<abstract>>
        #validation ValidationGroup
        #style KebabStyle
        #buildForm(JPanel, GridBagConstraints)*
        #onConfirm()*
        #addLabeled(...)
        #addIdField(...) JTextField
    }

    class CreateArchetypeDialog

    class EditArchetypeDialog

    class DuplicateAsDialog

    class IconPickerPanel {
        +selectedIcon() Maybe~ArchetypeIcon~
    }

    class ParamListPanel {
        +params() Map~String, ParamType~
    }

    class ParamRowPanel

    class ParamTypeRenderer

    class ValidationGroup {
        +add(JTextField, Rule)
        +isValid() boolean
        +addListener(Runnable)
    }

    class FieldValidator

    class Rule {
        <<interface>>
    }

    class Rules {
        +notEmpty(String)$
        +identifier(String)$
        +unique(String, Supplier)$
        +all(Rule...)$
    }

    class Editor
    class FileChooser

    ArchetypeManagerDialog --> Editor
    ArchetypeManagerDialog ..> CreateArchetypeDialog
    ArchetypeManagerDialog ..> EditArchetypeDialog
    ArchetypeManagerDialog ..> DuplicateAsDialog
    AbstractArchetypeDialog <|-- CreateArchetypeDialog
    AbstractArchetypeDialog <|-- EditArchetypeDialog
    AbstractArchetypeDialog <|-- DuplicateAsDialog
    AbstractArchetypeDialog *-- ValidationGroup
    CreateArchetypeDialog *-- IconPickerPanel
    CreateArchetypeDialog *-- ParamListPanel
    EditArchetypeDialog *-- IconPickerPanel
    EditArchetypeDialog *-- ParamListPanel
    ParamListPanel o-- ParamRowPanel
    ParamRowPanel ..> ParamTypeRenderer
    IconPickerPanel ..> FileChooser
    ValidationGroup o-- FieldValidator
    FieldValidator --> Rule
    Rules ..> Rule : builds
```

## 6. Scene: модель і glTF I/O (заплановано)

```mermaid
classDiagram
    direction LR

    class Scene:::planned {
        -roots List~SceneNode~
        -byId Map~NodeId, SceneNode~
        -document JsonNode
        +roots() List~SceneNode~
        +find(NodeId) Maybe~SceneNode~
        +add(SceneNode, SceneNode parent)
        +remove(SceneNode)
        +traverse() Stream~SceneNode~
    }
    class SceneNode:::planned {
        -id NodeId
        -name String
        -transform Transform
        -parent Maybe~SceneNode~
        -children List~SceneNode~
        -rawNode JsonNode
        +archetypes() List~NodeArchetype~
        +worldMatrix() Mat4
    }
    class NodeId:::planned {
        <<record>>
        UUID value
    }
    class Transform:::planned {
        Vec3 translation
        Quat rotation
        Vec3 scale
        +toMatrix() Mat4
    }
    class Quat:::planned
    class Mat4:::planned

    class NodeArchetype:::planned {
        <<sealed interface>>
        +archetypeId() String
    }
    class ArchetypeInstance
    class UnresolvedArchetype:::planned {
        <<record>>
        String id
        JsonNode raw
    }

    class GltfSceneReader:::planned {
        +read(Path, ArchetypeCatalog) Scene
    }
    class GltfSceneWriter:::planned {
        +write(Scene, Path)
    }
    class ArchetypeInstanceJson:::planned {
        +toJson(ArchetypeInstance) JsonNode
        +fromJson(JsonNode, ArchetypeCatalog) NodeArchetype
    }
    class ParamJson:::planned {
        +encode(ParamType, Object) JsonNode
        +decode(ParamType, JsonNode) Object
    }
    class SceneLoadReport:::planned {
        warnings List~String~
        unresolved List~UnresolvedArchetype~
    }

    class ProjectRepository {
        <<record>>
        Path filePath
        Scene scene
        ArchetypeRegistry archetypeRegistry
    }

    class Command:::planned {
        <<interface>>
        +apply(Scene)
        +undo(Scene)
    }
    class CommandStack:::planned {
        +execute(Command)
        +undo()
        +redo()
        +isDirty() boolean
    }
    class MoveNodeCommand:::planned
    class SetParamCommand:::planned
    class AddNodeCommand:::planned
    class RemoveNodeCommand:::planned

    Scene *-- "*" SceneNode
    SceneNode --> NodeId
    SceneNode *-- Transform
    SceneNode o-- "*" NodeArchetype
    Transform --> Quat
    Transform ..> Mat4
    NodeArchetype <|.. ArchetypeInstance
    NodeArchetype <|.. UnresolvedArchetype
    GltfSceneReader ..> Scene : builds
    GltfSceneReader ..> ArchetypeInstanceJson
    GltfSceneReader ..> SceneLoadReport
    GltfSceneWriter ..> Scene
    GltfSceneWriter ..> ArchetypeInstanceJson
    ArchetypeInstanceJson ..> ParamJson
    ProjectRepository --> Scene : замінює currentSceneData
    Command <|.. MoveNodeCommand
    Command <|.. SetParamCommand
    Command <|.. AddNodeCommand
    Command <|.. RemoveNodeCommand
    CommandStack o-- Command
    CommandStack ..> Scene

    classDef planned fill:#eee,stroke:#888,stroke-dasharray: 4 3,color:#333
```

```json
"extras": { "kebab": { "id": "7f3a...", "archetypes": [
  { "id": "model_ref", "params": { "path": "Models/tree.gltf" } },
  { "id": "collider_box", "params": { "size": [1, 2, 1], "offset": [0, 1, 0] } }
] } }
```

## 7. Вбудовані архетипи та model_ref (заплановано)

```mermaid
classDiagram
    direction LR

    class ArchetypeRegistry
    class ArchetypeDefinition
    class BuiltinArchetypes:::planned {
        +MODEL_REF$
        +COLLIDER_BOX$
        +COLLIDER_SPHERE$
        +TRIGGER$
        +all()$ List~ArchetypeDefinition~
    }
    class ArchetypeCatalog:::planned {
        +find(String id) Maybe~ArchetypeDefinition~
        +isBuiltin(String id) boolean
        +all() List~ArchetypeDefinition~
        +behaviorOf(String id) Maybe~ArchetypeBehavior~
    }
    class ArchetypeBehavior:::planned {
        <<interface>>
        +renderables(SceneNode, ArchetypeInstance, ModelResolver) List~Renderable~
        +gizmo(SceneNode, ArchetypeInstance) Maybe~GizmoShape~
        +bounds(SceneNode, ArchetypeInstance) Maybe~Aabb~
        +validate(SceneNode, ArchetypeInstance) List~String~
    }
    class ModelRefBehavior:::planned
    class ColliderBoxBehavior:::planned
    class ColliderSphereBehavior:::planned
    class TriggerBehavior:::planned

    class ModelResolver:::planned {
        -cache Map~Path, ResolvedModel~
        -resolving Deque~Path~
        +resolve(Path scene, String relPath) ModelResult
        +wouldCycle(Path from, Path to) boolean
    }
    class ModelResult:::planned {
        <<sealed interface>>
    }
    class ResolvedModel:::planned {
        Path canonicalPath
        Scene scene
        List~MeshData~ meshes
    }
    class BrokenReference:::planned {
        <<record>>
        Reason reason
        Path path
    }
    class Reason:::planned {
        <<enumeration>>
        MISSING
        CYCLE
        TOO_DEEP
        UNREADABLE
    }
    class MeshData:::planned {
        float[] positions
        float[] normals
        float[] uvs
        int[] indices
        Aabb bounds
    }
    class GltfMeshExtractor:::planned {
        +extract(JsonNode doc, Path dir) List~MeshData~
    }
    class Renderable:::planned {
        <<record>>
        MeshData mesh
        Mat4 world
        Color tint
    }

    ArchetypeCatalog o-- ArchetypeRegistry : шар проєкту, редагований
    ArchetypeCatalog o-- BuiltinArchetypes : вбудований шар, лише читання
    BuiltinArchetypes ..> ArchetypeDefinition : створює
    ArchetypeCatalog o-- "*" ArchetypeBehavior
    ArchetypeBehavior <|.. ModelRefBehavior
    ArchetypeBehavior <|.. ColliderBoxBehavior
    ArchetypeBehavior <|.. ColliderSphereBehavior
    ArchetypeBehavior <|.. TriggerBehavior
    ModelRefBehavior --> ModelResolver
    ModelResolver ..> ModelResult
    ModelResult <|.. ResolvedModel
    ModelResult <|.. BrokenReference
    BrokenReference --> Reason
    ResolvedModel o-- "*" MeshData
    ModelResolver ..> GltfMeshExtractor
    ArchetypeBehavior ..> Renderable : створює

    classDef planned fill:#eee,stroke:#888,stroke-dasharray: 4 3,color:#333
```

## 8. Rendering: JOGL (заплановано)

```mermaid
classDiagram
    direction TB

    class EditorViewport:::planned {
        <<GLJPanel, GLEventListener>>
        -camera Camera
        -cameraController CameraController
        -passes List~RenderPass~
        -picker Picker
        -scene Scene
        -selection Selection
        +init(GLAutoDrawable)
        +display(GLAutoDrawable)
        +reshape(...)
        +dispose(GLAutoDrawable)
    }
    class RenderContext:::planned {
        <<record>>
        Scene scene
        Camera camera
        Selection selection
        ArchetypeCatalog catalog
        int width
        int height
    }
    class RenderPass:::planned {
        <<interface>>
        +init(GL3)
        +render(GL3, RenderContext)
        +dispose(GL3)
    }
    class SceneRenderer:::planned
    class GridRenderer:::planned
    class SelectionRenderer:::planned
    class GizmoRenderer:::planned
    class OverlayRenderer:::planned
    class ArchetypeIconRenderer:::planned
    class GreyboxRenderer:::planned
    class PathRayRenderer:::planned

    class Camera:::planned {
        position Vec3
        yaw, pitch, fov
        +view() Mat4
        +projection(aspect) Mat4
        +ray(x, y) Ray
    }
    class CameraController:::planned {
        <<interface>>
        +attach(Component)
        +detach()
        +update(double dt, Camera)
    }
    class FreeFlightController:::planned
    class Picker:::planned {
        +pick(Scene, Camera, x, y) Maybe~SceneNode~
    }
    class Ray:::planned
    class Selection:::planned {
        +selected() List~SceneNode~
        +select(SceneNode)
        +clear()
        +addListener(Runnable)
    }
    class GizmoMode:::planned {
        <<enumeration>>
        TRANSLATE
        ROTATE
        SCALE
    }
    class GizmoInteraction:::planned {
        +press(Ray)
        +drag(Ray)
        +release()
    }
    class GpuMeshCache:::planned {
        +get(MeshData) GpuMesh
        +dispose(GL3)
    }
    class GpuMesh:::planned {
        vao, vbo, ibo
        +draw(GL3)
    }
    class ShaderProgram:::planned {
        +use(GL3)
        +setUniform(...)
    }
    class IconTextureCache:::planned

    EditorViewport --> RenderContext : будується щокадру
    EditorViewport o-- "*" RenderPass : упорядковані
    EditorViewport *-- Camera
    EditorViewport *-- CameraController
    EditorViewport *-- Picker
    EditorViewport --> Scene
    EditorViewport --> Selection
    RenderPass <|.. SceneRenderer
    RenderPass <|.. GridRenderer
    RenderPass <|.. SelectionRenderer
    RenderPass <|.. GizmoRenderer
    RenderPass <|.. OverlayRenderer
    RenderPass <|.. ArchetypeIconRenderer
    RenderPass <|.. GreyboxRenderer
    RenderPass <|.. PathRayRenderer
    CameraController <|.. FreeFlightController
    CameraController ..> Camera
    Camera ..> Ray
    Picker ..> Ray
    Picker ..> Scene
    GizmoRenderer --> GizmoMode
    GizmoRenderer --> GizmoInteraction
    SceneRenderer --> GpuMeshCache
    SceneRenderer --> ShaderProgram
    SceneRenderer ..> Renderable : з ArchetypeBehavior або вбудованого mesh
    GreyboxRenderer ..> Renderable
    GpuMeshCache o-- GpuMesh
    ArchetypeIconRenderer --> IconTextureCache
    IconTextureCache ..> ArchetypeIcon
    GizmoInteraction ..> CommandStack : MoveNodeCommand

    classDef planned fill:#eee,stroke:#888,stroke-dasharray: 4 3,color:#333
```

## 9. Editor panes (заплановано)

```mermaid
classDiagram
    direction LR
    class InspectorPane:::planned {
        +show(SceneNode)
    }

    class ParamEditorFactory:::planned {
        +create(ParamType, Object, Consumer) JComponent
    }

    class AssetExplorerTab:::planned

    class AssetTreeModel:::planned

    class LogsTab:::planned

    class LogService:::planned {
        +info(String)
        +warn(String)
        +error(String, Throwable)
        +addListener(Consumer)
    }

    class SceneHierarchyPane:::planned

    class HelpMenu:::planned

    class MainWindow
    class Editor
    class EditorViewport:::planned

    MainWindow *-- InspectorPane
    MainWindow *-- AssetExplorerTab
    MainWindow *-- LogsTab
    MainWindow *-- SceneHierarchyPane
    MainWindow --> HelpMenu
    InspectorPane --> ParamEditorFactory
    InspectorPane ..> Editor : редагує через CommandStack
    AssetExplorerTab --> AssetTreeModel
    LogsTab --> LogService

    classDef planned fill:#eee,stroke:#888,stroke-dasharray: 4 3,color:#333
```

## 10. Playtest і аналіз (заплановано)

```mermaid
classDiagram
    direction LR
    class PlaytestSession:::planned {
        +start(Scene)
        +stop()
        +tick(double)
    }

    class Actor:::planned {
        position Vec3
        velocity Vec3
        bounds Aabb
    }

    class ActorController:::planned {
        +step(Actor, Input, double)
    }

    class PhysicsWorld:::planned {
        +add(Collider)
        +move(Actor, Vec3 delta) CollisionResult
        +overlaps(Aabb) List~Collider~
    }

    class Collider:::planned {
        Aabb bounds
        boolean trigger
    }

    class ReplayRecorder:::planned {
        +record(InputFrame)
        +frames() List~InputFrame~
    }

    class PathTrace:::planned {
        points List~Vec3~
    }

    class ReachabilityAnalyzer:::planned {
        +analyze(List~Collider~, JumpModel) UnionFind
        +canReach(Collider, Collider) boolean
    }

    class UnionFind:::planned

    class EditorViewport:::planned
    class Scene

    PlaytestSession *-- Actor
    PlaytestSession *-- ActorController
    PlaytestSession *-- PhysicsWorld
    PlaytestSession *-- ReplayRecorder
    PlaytestSession ..> PathTrace : створює
    PhysicsWorld o-- Collider
    Collider ..> Scene : будується з архетипів-колайдерів
    ReachabilityAnalyzer --> UnionFind
    ReachabilityAnalyzer ..> Collider
    PathTrace ..> EditorViewport : малює PathRayRenderer

    classDef planned fill:#eee,stroke:#888,stroke-dasharray: 4 3,color:#333
```

