+++
title = "Inspector"
description = "Edit the selected object in the side pane."
weight = 30
template = "docs/page.html"

[extra]
lead = 'The Inspector shows the transform and every archetype of the selected object.'
toc = true
top = false
+++

Select an object and the Inspector on the right shows:

- **Name.** Press Enter or leave the field to rename.
- **Transform.** Position, rotation in degrees and a uniform scale. The rotation fields show Euler angles; an untouched rotation is never rewritten.
- **One section per archetype.** Each parameter has its own editor. Values are applied on Enter, on leaving the field or when you change a control. A field that is still being edited is not overwritten by outside changes.
- **Add Archetype** and **Remove archetype.**

Edits made in the Inspector are undoable steps. The Inspector is disabled while a playtest runs, because the scene being played is read-only.
