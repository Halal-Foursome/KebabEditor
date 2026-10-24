+++
title = "Introduction"
description = "What KebabEditor is and how it thinks about levels."
weight = 10
template = "docs/page.html"

[extra]
lead = 'KebabEditor models gameplay data as typed objects called archetypes, and saves levels as plain glTF.'
toc = true
top = false
+++

## Why archetypes?

Most level pipelines hide gameplay data in loose custom properties such as `type=trigger`. KebabEditor models them as real objects instead. An **archetype** has an id, a display name, an optional icon and a set of **typed parameters** with default values. An object in the scene carries any number of archetype instances.

On save, the instances are written into the standard glTF `extras` field, so a level stays valid JSON that other tools can open.

## What you can do

- Place greybox boxes and spheres, models, lights and empty objects in a 3D viewport.
- Attach archetypes such as colliders, triggers, lights and a player start.
- Define your own archetypes with their own parameters.
- Run the level in a playtest and check jumps, drops and triggers.

## Where to go next

- [Install and run the editor](@/docs/getting-started/installation.md)
- [Build your first level](@/docs/getting-started/quick-tour.md)
