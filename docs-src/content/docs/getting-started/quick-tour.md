+++
title = "Quick tour"
description = "Build and play a tiny level in a few minutes."
weight = 30
template = "docs/page.html"

[extra]
lead = 'Create a project, add a floor, a trigger and a player start, then run it.'
toc = true
top = false
+++

## 1. Create a project

Choose **File > Create new project...**, pick a folder and a level name. The editor creates the scene file, an `archetypes.json`, an `assets/` folder and a `kebab-project.json`.

## 2. Add a floor

Press `Ctrl+B` to add a greybox box in front of the camera. Select it and drag the gizmo handles to move it. Press `3` for the scale tool and stretch it into a floor. Greyboxes are solid in a playtest.

## 3. Add a player start

Choose **Edit > Add player start** and drag it above the floor. The player start decides where a playtest begins and the metrics of the actor.

## 4. Add a trigger

Press `Ctrl+B` for another box, then add the **Trigger** archetype in the Inspector with **Add Archetype**. Its `size` starts as the greybox's own size. A greybox that carries a trigger is not solid, so the actor can walk into it.

## 5. Run it

Press `F5`. Walk with `W` `A` `S` `D`, jump with `Space`, look around by dragging, and stop with `Esc`. When the actor is inside the trigger, the trigger lights up green. When you stop, the path of the run stays visible. Use **Playtest > Path thickness** to make it easier to see.

## 6. Save

Press `Ctrl+S`.
