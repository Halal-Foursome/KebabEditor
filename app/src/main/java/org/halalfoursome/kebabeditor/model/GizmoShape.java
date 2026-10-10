package org.halalfoursome.kebabeditor.model;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape.*;

public sealed interface GizmoShape 
    permits
        GizmoBox,
        GizmoSphere,
        GizmoLines
{
    record GizmoBox(Vec3 size) implements GizmoShape {}

    record GizmoSphere(float radius) implements GizmoShape {}

    record GizmoLines(MeshData lines) implements GizmoShape {}
}
