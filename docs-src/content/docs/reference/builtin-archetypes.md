+++
title = "Built-in archetypes"
description = "The archetypes that ship with the editor."
weight = 20
template = "docs/page.html"

[extra]
lead = 'Ids, parameters and defaults of the built-in archetypes.'
toc = true
top = false
+++

| Id | Single | Parameters (default) |
|---|---|---|
| `model_ref` | yes | `path` |
| `prefab_ref` | yes | `path` |
| `collider_box` | no | `size` (1, 1, 1) |
| `collider_sphere` | no | `radius` (0.5) |
| `trigger` | no | `size` (1, 1, 1) |
| `point_light` | yes | `color` (white), `intensity` (1000), `range` (10), `shadows` (false) |
| `dir_light` | yes | `color` (white), `intensity` (1), `shadows` (false) |
| `spot_light` | yes | `color` (white), `intensity` (1000), `range` (10), `inner_angle` (25), `outer_angle` (35), `shadows` (false) |
| `player_start` | yes | `run_speed` (6), `jump_height` (1.5), `gravity` (25), `radius` (0.35), `height` (1.8) |
| `snap_distance` | yes | `distance` (0.5) |

## Notes

- `model_ref` draws a glTF model in place of the object. `prefab_ref` draws a whole other scene file, and prefabs that contain themselves are rejected.
- Missing models and textures are shown as a visible placeholder.
- `collider_box` and `trigger` are sized in the object's local space and follow its transform.
