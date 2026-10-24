+++
title = "Viewport and objects"
description = "Move the camera, select, transform and place objects."
weight = 10
template = "docs/page.html"

[extra]
lead = 'The viewport is where you look at and edit the scene.'
toc = true
top = false
+++

## Camera

| Action | How |
|---|---|
| Look around | Drag with the mouse |
| Move | `W` `A` `S` `D` |
| Up and down | `Space` and `Ctrl` |
| Speed up | Hold `Shift` |
| Dolly | Mouse wheel |

## Selecting and transforming

Click an object or its icon to select it. The gizmo has three modes: `1` move, `2` rotate and `3` scale. Press `Ctrl+Shift+G` to snap to the grid. The grid step comes from a **Snap Distance** object in the scene, or defaults to 0.5.

## Adding objects

| Object | Shortcut |
|---|---|
| Greybox box | `Ctrl+B` |
| Greybox sphere | `Ctrl+Shift+B` |
| Import a glTF model | `Ctrl+I` |
| Empty, player start, lights | **Edit** menu |

## Placement ray

`Ctrl+Shift+P` starts a placement ray that spawns objects on surfaces. The mouse wheel changes the distance, `Alt` ignores surfaces and `Esc` leaves.

## Icons

Every archetype on an object is drawn as a billboard icon. Several icons are arranged in a pyramid. Empty objects get an icon too, so they stay selectable. Toggle them in **View > Show icons**.

## Undo and redo

`Ctrl+Z` and `Ctrl+Y` undo and redo scene edits while the viewport has focus. Undo is disabled during a playtest.
