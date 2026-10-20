# Asset Explorer implementation

The bottom tool pane now contains Asset Explorer and the existing Logs tab. The explorer
has a resizable directory tree and compact file list with KebabStyle fonts and Lucide
icons tinted by the current FlatLaf selection/foreground colors. Directory expansion,
selection, double-click navigation, Enter activation, refresh and empty/error states
are supported. Project close clears the browser. Project changes cancel pending workers;
generation counters also reject stale directory results. Component removal unregisters
its project listener. Child-window closure stops navigation/render timers and releases
OpenGL resources.

## Configuration and compatibility

The project entry point remains a `.gltf` scene. A new project creates `assets/` and:

```json
{
  "version": 1,
  "assetRoot": ".",
  "assetFolders": ["assets", "models"],
  "scenes": ["level.gltf"]
}
```

`models` preserves visibility of the existing model import destination. Missing folders
are allowed. The nearest `kebab-project.json` in the scene directory or an ancestor is
used, which supports registered scenes in subdirectories. Without configuration, the
scene directory and all its subdirectories remain browsable, with only the active scene
classified as a scene. Configuration changes take effect when reopening the project.

`assetRoot` is relative to the configuration directory and must remain within it. Asset
folders and scenes are relative to assetRoot. Invalid JSON, field types, version, paths,
and existing non-directory asset roots/folders are reported as I/O errors. Paths are
normalized; lexical escapes and symlink escapes (including missing descendants of an
existing link) are rejected. The browser omits symbolic links entirely to prevent cycles
and aliases. The root exposes its direct files; subdirectories are limited to configured
asset folders, their descendants, and paths needed to reach configured scenes.

Creation uses CREATE_NEW and does not replace scenes, archetypes or configuration. If a
configuration already exists, the requested new scene must already be registered in it;
otherwise creation is rejected before writing the scene. This deliberately requires an
explicit configuration edit rather than silently rewriting an existing config.

Scene and archetype serialization formats are unchanged. Archetypes retain their existing
scene-sibling location. The main editor's save still saves scene and archetypes. Secondary
scene windows save only the scene, so a stale catalog cannot overwrite shared definitions.
All scene saves check the disk content hash against the opened/last-saved version. If
another writer changed the scene, saving fails with a reopen message. This is an optimistic
conflict check, not an interprocess file lock.

## Opening and session isolation

`AssetType` classifies registered scene paths first. Unregistered glTF/GLB files are
models. Images, JSON and other files display suitable icons; external opening is not
implemented. `AssetOpenHandler` centralizes dispatch independently of the Swing widgets.

`AssetWindows` loads models and scenes in SwingWorkers, creates windows on the EDT, and
focuses an existing child window if the same path is opened again. A model preview uses
the existing extractor, scoped model resolver, mesh renderer and free-flight navigation.
It frames the model bounds and has no editing or save actions. Missing/broken/empty models
produce an error instead of a misleading preview.

Every scene window has a separate Editor, ProjectManager, Scene, Selection, catalog and
resolver caches. CPU-side scene/model references are warmed before displaying the window.
The existing Edit/View menus, object dialog and transform gizmos provide actual editing.
The scene window is bound to its original file; it cannot switch projects. Model/scene
windows remain independent when the main project switches, and close when the main editor
exits. Unsaved scene windows ask before closing.

## Remaining limitations

- No filesystem watcher; use Refresh. Reopen after editing configuration.
- No new undo/redo stack: the repository does not yet implement undo/redo.
- Shared archetype definitions are edited in the main window. Secondary windows use a
  snapshot; reopen them after changing definitions.
- The legacy renderer still resolves newly edited model references synchronously; opening
  an asset preloads its initial references in the background. GPU upload occurs during
  rendering, as in the existing viewport.
- Editable scenes use the existing JSON glTF scene reader/writer; binary GLB is supported
  for models, not for editable scenes. Do not register a GLB as an editable scene.
- Textures and other non-model files are visible but have no open action.
- The browser uses a compact list rather than an icon grid.

## Verification

`./gradlew test` and `./gradlew build` pass. Tests cover valid/legacy/malformed configs,
relative paths and missing roots, lexical/symlink escapes, file listing and sorting,
scene/model classification and dispatch, scene session/save isolation, same-file save
conflicts, project switching/closing/refresh, listener detach, both FlatLaf themes and
new icon availability. Existing tests remain passing.

A temporary smoke application was launched successfully with an example project. Native
desktop automation enumerated the Java process but rejected attaching to its app ID/name,
so visual interaction and OpenGL rendering have not been manually verified here.

Manual verification:

1. Run `./gradlew run`; create a project and confirm assets/ and kebab-project.json exist.
2. Confirm Asset Explorer and Logs both work. Drag both split dividers.
3. Add assets/Models with a glTF/GLB model and assets/Empty. Click Refresh; expand folders,
   select directories and double-click them. Verify directories precede files and names
   sort without regard to case. Verify the empty-directory message.
4. Double-click the model: check camera navigation (drag, WASD, wheel), model framing,
   absence of edit controls, and that the main scene is unchanged. Close and reopen it.
5. Copy/create another JSON glTF scene beside the first; add its relative name to scenes,
   then reopen the project. Double-click it, add a greybox with Ctrl+B, edit its transform,
   and save with Ctrl+S. Check only that scene changed and archetypes.json did not.
6. Open the active scene in a child window, save a change in the main window, then try
   saving the child. Confirm a conflict error instead of overwrite.
7. Switch/close projects during navigation. Check no old files reappear. Open a legacy
   scene with no configuration. Test malformed configs, missing directories and broken
   models; verify helpful errors and working Logs.
8. Check both FlatLaf themes in a theme-configured harness (the application currently
   chooses its dark theme at startup). Check selected row text/icons and folder expansion.
9. Close all preview/scene windows and exit; verify no render timers retain the process.

## Created files

Paths below are relative to the repository root.

- `app/src/main/java/org/halalfoursome/kebabeditor/assets/AssetType.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/assets/AssetService.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/assets/AssetOpenHandler.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/project/ProjectConfig.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/project/ProjectConfigStore.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/project/ProjectAssets.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/project/EditorSessions.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/ui/assets/AssetExplorerTab.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/ui/assets/AssetTreeModel.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/ui/assets/AssetWindows.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/ui/assets/ModelPreviewWindow.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/ui/assets/SceneEditorWindow.java`
- `app/src/main/resources/img/lucide/folder-open.svg`
- `app/src/main/resources/img/lucide/image.svg`
- `app/src/test/java/org/halalfoursome/kebabeditor/project/ProjectConfigStoreTest.java`
- `app/src/test/java/org/halalfoursome/kebabeditor/project/EditorSessionsTest.java`
- `app/src/test/java/org/halalfoursome/kebabeditor/assets/AssetServiceTest.java`
- `app/src/test/java/org/halalfoursome/kebabeditor/ui/assets/AssetExplorerTabTest.java`
- `docs/asset-explorer.md`

The two added icons are upstream Lucide SVG assets from
https://github.com/lucide-icons/lucide/tree/main/icons, covered by the existing bundled
`app/src/main/resources/img/lucide/LICENSE`.

## Modified files

- `app/src/main/java/org/halalfoursome/kebabeditor/editor/Editor.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/project/FileProjectLoader.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/project/FileProjectWriter.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/project/ProjectManager.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/project/ProjectRepository.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/ui/EditorView.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/ui/MainWindow.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/ui/menu/FileMenu.java`
- `app/src/main/java/org/halalfoursome/kebabeditor/utils/LucideIcon.java`
- `README.md`
- `docs/architecture.c4.uk.md`
