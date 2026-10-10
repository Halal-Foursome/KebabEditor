package org.halalfoursome.kebabeditor.model;

public enum PrimitiveType {
    TRIANGLES(3),
    LINES(2);

    private final int indicesPerPrimitive;

    PrimitiveType(int indicesPerPrimitive) {
        this.indicesPerPrimitive = indicesPerPrimitive;
    }

    public int indicesPerPrimitive() {
        return indicesPerPrimitive;
    }
}
