package org.halalfoursome.kebabeditor.render;

/** A grabbable part of the transform gizmo. */
public enum GizmoHandle {
    X(0),
    Y(1),
    Z(2),
    /** Translate within the plane perpendicular to X (the YZ plane); likewise below. */
    PLANE_X(0),
    PLANE_Y(1),
    PLANE_Z(2),
    /** Translate: free move in the view plane. Scale: uniform scale. */
    CENTER(-1);

    private final int axis;

    GizmoHandle(int axis) {
        this.axis = axis;
    }

    /** 0, 1, 2 for X, Y, Z (for a plane: the axis it is perpendicular to), -1 for the center. */
    public int axis() {
        return axis;
    }

    public boolean isPlane() {
        return this == PLANE_X || this == PLANE_Y || this == PLANE_Z;
    }

    public static GizmoHandle axis(int index) {
        return values()[index];
    }

    public static GizmoHandle plane(int index) {
        return values()[3 + index];
    }
}
