+++
title = "Playtest"
description = "Run the level and read the results."
weight = 50
template = "docs/page.html"

[extra]
lead = 'A playtest runs a capsule actor through the level with simple physics.'
toc = true
top = false
+++

## Starting

Press `F5`, use **Playtest > Run / stop level**, or use the run button. `Esc` stops it. A playtest needs something solid to stand on and a **Player Start**. If several exist, the selected one is used, otherwise the first.

During a playtest the scene is read-only: editing, undo and the Inspector are disabled.

## Controls

`W` `A` `S` `D` or the arrow keys run, `Space` jumps and dragging looks around.

## What is solid

- Greyboxes are solid, except a greybox that carries a **Trigger**.
- Box and sphere colliders are solid.
- Triggers are not solid. They report the actor entering them, and a trigger the actor is inside lights up green.

## The actor

The Player Start archetype sets the actor: `run_speed`, `jump_height`, `gravity`, `radius` and `height`. The height must be at least twice the radius.

## What it reports

Jumps, drops of more than one metre and trigger entries are written to the Logs panel. An actor that falls far below the level respawns at the start.

## Path rays

After each run its path stays in the viewport, with the newest brightest and the last eight kept. Ground and air segments have different colors, and jumps and trigger hits are marked. Use **Playtest > Show path rays**, **Clear path rays** and **Path thickness**.
