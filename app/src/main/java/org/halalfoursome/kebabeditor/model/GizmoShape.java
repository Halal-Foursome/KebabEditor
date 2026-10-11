package org.halalfoursome.kebabeditor.model;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape.*;

public sealed interface GizmoShape 
    permits
        GizmoBox,
        GizmoSphere,
        GizmoAabb,
        GizmoLines
{
    record GizmoBox(Vec3 size) implements GizmoShape {}

    record GizmoSphere(float radius) implements GizmoShape {}

    record GizmoAabb(Aabb box) implements GizmoShape {

        public GizmoAabb {
            box = box.copy();
        }

        @Override
        public Aabb box() {
            return box.copy();
        }
    }

    record GizmoLines(MeshData lines) implements GizmoShape {}
}
