+++
title = "Archetypes"
description = "Built-in and custom archetypes, parameters and defaults."
weight = 20
template = "docs/page.html"

[extra]
lead = 'An archetype is a named set of typed parameters that you attach to objects.'
toc = true
top = false
+++

## Attaching archetypes

Select an object and use **Add Archetype** in the Inspector, or open the object dialog with `Ctrl+E` (or by double-clicking the object). Every parameter type has a suitable editor.

Invalid setups, such as a bad model path or a second copy of a single archetype, are reported in the Logs panel or highlighted in the dialog.

## Your own archetypes

Open **Project > Manage archetypes** (`Ctrl+Shift+A`) to create, edit, duplicate and delete archetypes. Each parameter has a type and a default value. The **single** flag limits an archetype to one instance per object.

Your definitions are stored in `archetypes.json` next to the scene. The built-in ones are part of the application.

## Parameter types

`int`, `float`, `bool`, `string`, relative path, `vec3`, color and node reference.

## Defaults for new instances

A new instance starts with the parameter defaults. A **Box Collider** or **Trigger** added to a greybox starts with the greybox's own size.

## Unknown archetypes

Unknown archetypes and parameters are kept as they are when a scene is loaded and saved again, so opening a level in a project that lacks an archetype does not destroy its data.

See the [built-in archetypes](@/docs/reference/builtin-archetypes.md).
