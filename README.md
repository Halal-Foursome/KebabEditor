# KebabEditor

A desktop 3D level editor with typed, object-oriented **archetypes**. Place objects in a 3D viewport, attach gameplay archetypes such as colliders, triggers and lights, and save everything as a regular glTF file.

![Java 25](https://img.shields.io/badge/Java-25-orange)
![Build](https://img.shields.io/badge/build-Gradle-02303A)
![UI](https://img.shields.io/badge/UI-Swing%20%2B%20FlatLaf-blue)
![Renderer](https://img.shields.io/badge/renderer-OpenGL%20(JOGL)-lightgrey)

## Why archetypes?

Most level pipelines hide gameplay data in loose custom properties (`type=trigger`, `action=...`). KebabEditor models them as real objects instead: an archetype has an id, a display name, an optional icon and a set of **typed parameters** (int, float, bool, string, path, vec3, color, node reference) with default values. An object in the scene carries any number of archetype instances.

On save, the instances are written into the standard glTF `extras` field, so a level stays valid JSON that other tools can open.

## Features

**Viewport**
- OpenGL 3 viewport with a free-flight camera, a reference grid, lit meshes and textures
- Click to select; transform gizmo for move, rotate and scale, with grid snapping
- Greybox boxes and spheres, plus a placement ray that spawns objects on surfaces
- Billboard icons for every archetype on an object, arranged in a pyramid when there are several. Empty objects get an icon too, so they stay selectable (toggle in *View → Show icons*)

**Archetypes**
- Built-in archetypes: model reference, prefab reference, box and sphere colliders, trigger, point / directional / spot lights, snap distance
- Create, edit, duplicate and delete your own archetypes (**Project → Manage archetypes**, `Ctrl+Shift+A`), including per-parameter defaults and a **single** flag that limits an archetype to one instance per object
- An object dialog (double-click an object, or `Ctrl+E`) to add, edit and remove the archetype instances on it, with a suitable editor for every parameter type
- Invalid setups, such as a bad model path or a duplicate of a single archetype, are reported in the Logs panel or highlighted in the dialog

**Models and prefabs**
- Import glTF / GLB models (embedded textures included); external models are copied into the project's `models/` folder
- A prefab reference draws a whole other scene file in place of its object, with protection against prefabs that contain themselves
- Missing models and textures are shown as a visible placeholder instead of disappearing

**Projects**
- Create, open and save projects; recent projects menu
- Atomic saving, so a failed write cannot corrupt a level
- Asset Explorer beside Logs: lazy directory tree, sorted file browser, refresh, and theme-aware Lucide icons
- Double-click models for a read-only preview; registered scenes for an independent editable scene window
- Optional `kebab-project.json` scopes assets and registers scenes, while legacy scene-file projects still open
- A Logs panel for warnings and errors

### Planned

Playtest mode with simple AABB physics, an Inspector panel, and undo / redo.

## Getting started

### Requirements

- JDK 25 (Gradle can download a matching toolchain)
- A GPU and driver with OpenGL 3.x support

### Run

```sh
./gradlew run
```

### Test

```sh
./gradlew test
```

If you start the application from an IDE instead of Gradle, copy the JVM arguments from `applicationDefaultJvmArgs` in [`app/build.gradle.kts`](app/build.gradle.kts). JOGL needs them to reach native libraries and AWT internals.

## Using the editor

Create a project with **File → Create new project…**, then add objects from the **Edit** menu.

| Action | How |
|---|---|
| Look around | Drag with the mouse |
| Move the camera | `W` `A` `S` `D`, `Space` up, `Ctrl` down, `Shift` for speed, mouse wheel to dolly |
| Select | Click an object or its icon |
| Gizmo mode | `1` move, `2` rotate, `3` scale |
| Edit the selected object | Double-click, or `Ctrl+E` |
| Add greybox box / sphere | `Ctrl+B` / `Ctrl+Shift+B` |
| Import a glTF model | `Ctrl+I` |
| Placement ray | `Ctrl+Shift+P` (wheel changes distance, `Alt` ignores surfaces, `Esc` leaves) |
| Snap to grid | `Ctrl+Shift+G` |
| Manage archetypes | `Ctrl+Shift+A` |
| New / open / save project | `Ctrl+N` / `Ctrl+O` / `Ctrl+S` |

## Project files

A project is a glTF scene file with an `archetypes.json` next to it. The `archetypes.json` file holds your own archetype definitions, and the built-in ones are part of the application.

```text
my-level/
├── level.gltf          scene: nodes, transforms and archetype instances
├── archetypes.json     your archetype definitions
├── kebab-project.json  asset scope and editable scene registration
├── assets/             project assets
└── models/             imported models (created when importing)
```

New projects create `assets/` and `kebab-project.json` with version 1, `assetRoot: "."`,
`assetFolders: ["assets", "models"]`, and the new scene in `scenes`. Existing projects
without a config use their scene directory as the browser root. Paths must stay inside
the project; symbolic links are omitted from the browser. Keep imported models in scope
if you edit `assetFolders`.

Select a directory in Asset Explorer, double-click a folder to navigate, and double-click
an asset (or press Enter) to open it. A glTF/GLB file is a model unless registered in
`scenes`. **File → Close project** clears the browser. **File → Refresh assets** (`F5`) lists filesystem changes, and the explorer also refreshes when the window regains focus;
use **Project → Asset folders…** to edit the asset root, folders and registered scenes of the open project. The root can be any folder that contains the open scene, for example a `Project/` folder above `Scenes/` and `Models/`. Moving it moves `kebab-project.json` and reloads the scene. Reopen the project after editing `kebab-project.json` by hand.

Scene windows have independent scenes, selections, cameras and resolver caches. Their
Save action writes only their own scene; use the main editor to manage shared archetype
definitions. Saving refuses to overwrite a scene changed on disk since it was opened
or last saved. See [Asset Explorer implementation and manual verification](docs/asset-explorer.md)
for configuration rules, limitations and the changed-file inventory.

Archetype instances live under `extras.kebab` on each node:

```json
{
  "name": "Lamp",
  "translation": [0, 3, 0],
  "extras": {
    "kebab": {
      "archetypes": [
        { "id": "point_light",
          "params": { "color": "(1.0, 0.5, 0.0)", "intensity": 1000.0, "range": 6.0 } }
      ]
    }
  }
}
```

Unknown archetypes and parameters are kept as they are when a scene is loaded and saved again, so opening a level in a project that lacks an archetype does not destroy its data.

## Architecture

The code is split so that the scene and archetype model has no dependency on the user interface or on OpenGL.

```text
app/src/main/java/org/halalfoursome/kebabeditor/
├── math/        vectors, matrices, quaternions, boxes, rays
├── scene/       scene graph, glTF reading and writing, model and prefab resolvers
├── archetype/   definitions, typed parameters, instances, the catalog
├── behavior/    what an archetype does: drawables, bounds, selection shapes, validation
├── model/       meshes, materials, textures: plain data with no GL types
├── project/     loading, saving and importing projects
├── editor/      the facade the UI talks to (selection, spawning, snapping)
├── render/      OpenGL passes, shaders, GPU caches, picking, icons
├── ui/          Swing windows, menus, dialogs and the viewport
├── logging/     in-memory log shown in the Logs panel
└── utils/       shared helpers (Maybe, file and path handling, styles)
```

Key ideas:
- **Behaviors produce data, not draw calls.** An archetype behavior returns `Drawable`s that the renderer turns into GL calls. Colliders, lights and prefab references all work this way.
- **Loading is pluggable.** Scene readers and writers, project loaders, model extractors and resolvers are interfaces with glTF-based implementations, so another format can be added without touching the editor.
- **Failures degrade, they don't crash.** A missing file, an unknown archetype or a bad default becomes a placeholder or a log entry.

## Tech stack

- Java 25
- Swing and [FlatLaf](https://www.formdev.com/flatlaf/) for the user interface
- [JOGL](https://jogamp.org/jogl/www/) for the OpenGL viewport
- glTF 2.0 as the level format
- [Jackson](https://github.com/FasterXML/jackson) for JSON, Guava, Lombok
- Gradle (Kotlin DSL) and JUnit 5

## Contributing

- Work on a feature branch named in Title-Case with hyphens (for example `Asset-Explorer`) and open a pull request against `main`.
- Run `./gradlew test` before pushing.

## License

Licensed under MIT license.

The archetype icons are by pixelbazaar, licensed under CC BY-SA 4.0.

The UI icons are by https://lucide.dev/
