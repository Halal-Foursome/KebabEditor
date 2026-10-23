package org.halalfoursome.kebabeditor.playtest;

public enum PathThickness {
    THIN("Thin", 1f),
    MEDIUM("Medium", 2f),
    THICK("Thick", 4f),
    EXTRA_THICK("Extra thick", 6f);

    private final String label;
    private final float width;

    PathThickness(String label, float width) {
        this.label = label;
        this.width = width;
    }

    public String label() {
        return label;
    }

    // In pixels; the renderer caps it at what the driver supports
    public float width() {
        return width;
    }
}
