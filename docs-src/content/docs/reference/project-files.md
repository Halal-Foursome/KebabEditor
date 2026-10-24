+++
title = "Project files"
description = "File formats used by a project."
weight = 30
template = "docs/page.html"

[extra]
lead = 'The scene file, archetypes.json and kebab-project.json.'
toc = true
top = false
+++

## Scene file

A standard glTF 2.0 file. Archetype instances live under `extras.kebab` on each node:

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

A greybox is stored as `extras.kebab.greybox` with a `shape`, a `size` and a `color`.

## archetypes.json

Your own archetype definitions, next to the scene. Each has an id, a display name, an optional icon, a `single` flag and typed parameters with defaults.

## kebab-project.json

| Field | Meaning |
|---|---|
| `version` | Always `1` |
| `assetRoot` | The explorer root, relative to the folder containing the file |
| `assetFolders` | Folders shown under the root, relative to `assetRoot` |
| `scenes` | Scene files that open as editable scenes, relative to `assetRoot` |

All paths must stay inside the project.
