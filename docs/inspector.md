# Inspector pane

The main window has a resizable, scrolling Inspector on the entire right side.
Select an object to edit its name, local position, Y-X-Z Euler rotation in degrees,
uniform scale, and attached archetypes. Text and numeric fields commit on Enter or
focus loss. Checkboxes, node selectors, spinner buttons and colour selection commit
on action. Invalid numbers preserve stored values. Relative-path pickers use the
scene directory as their base. The existing object dialogs remain available.

Each archetype section can collapse and has a confirmed removal action. Add Archetype
uses `Editor.availableArchetypes` and `attachArchetype`, including defaults and single
instance restrictions. Unknown archetypes display their preserved JSON read-only.

## Integration

`MainWindow` nests its existing vertical viewport/tool split inside a horizontal
split, initially reserving 350 px for the Inspector. OpenGL fallback is unchanged.
`InspectorPane` subscribes to Selection and Editor scene notifications and coalesces
refreshes onto the Swing EDT. It detaches listeners when removed or closed.
Only selection or archetype structure/schema changes rebuild the panel. Transform
updates preserve focused input and do not round-trip untouched quaternions.

`Editor.updateTransform`, `setArchetypeParam`, rename/attach/detach methods publish
model changes. `GizmoInteraction` publishes drag and cancel callbacks, connected by
`EditorView` to `Editor.sceneChanged`. Scene structural notifications also clear
selection after direct node removal. New direct model mutation paths should publish
`sceneChanged()`; there is no polling or undo stack.

`TransformEditor` edits copies only for an individual commit, applying the result to
the authoritative node through Editor. It preserves the other transform components.
`Quat.toEuler` inverts the existing `fromEuler` Y * X * Z order. At gimbal lock it uses
zero roll and a combined yaw; displayed angles are cached until the quaternion changes.
`ArchetypeSection` reuses `ParamValueEditor` for all eight ParamType values. Parameter
commits use Editor and are saved by the existing glTF serializer.

## Changed files

Added:
- `ui/inspector/InspectorPane.java`
- `ui/inspector/InspectorSection.java`
- `ui/inspector/TransformEditor.java`
- `ui/inspector/ArchetypeSection.java`
- `ui/inspector/InspectorPaneTest.java` (test sources)
- `math/QuatEulerTest.java` (test sources)
- `docs/inspector.md`

Modified:
- `ui/MainWindow.java`, `ui/EditorView.java`
- `editor/Editor.java`, `editor/Selection.java`
- `scene/model/Scene.java`, `math/Quat.java`
- `render/GizmoInteraction.java` and its test
- `ui/archetype/ParamValueEditor.java`
- `ui/object/ArchetypeInstanceDialog.java`
- `README.md`, `docs/architecture.c4.uk.md`

Java paths above are relative to the existing `org/halalfoursome/kebabeditor` package.
No new dependencies were added.

## Verification

Headless tests cover selection changes, clearing, direct removal and project close;
name and transform edits; invalid and non-finite numbers; quaternion conversion,
including singularities and equivalent signs; gizmo drag/cancel notifications;
all parameter types and save/reload persistence; single instance restrictions and
removal; unknown archetypes; focus protection and stale commits; listener disposal;
and field layouts at 240, 350 and 500 px in both FlatLaf light and dark themes.
Colour selection and file-picker native dialogs require manual interaction. Full
OpenGL window behaviour has not been manually exercised by these headless tests.
The requested dependency DAG drawio XML was not present in the supplied repository.

Run with Java 25:

```sh
JAVA_TOOL_OPTIONS=-Djava.awt.headless=true ./gradlew :app:test :app:build
```

Manual checks: drag each gizmo and press Escape to cancel; change selection with
uncommitted input; resize the sidebar; add/remove built-ins; choose a colour and a
relative file; save/reopen; close the project. The viewport still renders on its
existing frame loop, without an Inspector timer or any Inspector OpenGL calls.
