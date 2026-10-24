+++
title = "Projects and assets"
description = "Project files, the Asset Explorer and the asset scope."
weight = 40
template = "docs/page.html"

[extra]
lead = 'A project is a glTF scene file with an archetypes.json next to it.'
toc = true
top = false
+++

## Project layout

```text
my-level/
├── level.gltf          scene: nodes, transforms and archetype instances
├── archetypes.json     your archetype definitions
├── kebab-project.json  asset scope and editable scene registration
├── assets/             project assets
└── models/             imported models (created when importing)
```

Saving is atomic, so a failed write cannot corrupt a level. Imported external models are copied into `models/`.

## Asset Explorer

The Asset Explorer sits beside Logs. Select a folder in the tree, double-click a folder to enter it, and double-click an asset or press Enter to open it.

- A glTF or GLB file opens as a read-only **model preview**, unless it is registered as a scene.
- A registered scene opens in its own editable window with its own selection and camera. Its **Save scene** action writes only that scene.
- The explorer refreshes when the window regains focus. **File > Refresh assets** (`Ctrl+F5`) refreshes it on demand.

## The asset scope

`kebab-project.json` decides what the explorer shows:

```json
{
  "version": 1,
  "assetRoot": ".",
  "assetFolders": ["assets", "models"],
  "scenes": ["level.gltf"]
}
```

Use **Project > Asset folders...** to change the asset root, the folders and the registered scenes. The root can be any folder that contains the open scene, for example a `Project/` folder above `Scenes/` and `Models/`. Moving the root moves the config file and reloads the scene.

Paths must stay inside the project, and symbolic links are left out of the browser. A project without a config uses the scene's folder as its root.
