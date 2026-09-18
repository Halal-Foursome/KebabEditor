package org.halalfoursome.kebabeditor.utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;

public enum LucideIcon {

    FOLDER_TREE("folder-tree"),
    FILE_SEARCH_CORNER("file-search-corner"),
    COG("cog"),
    BOLT("bolt"),
    COMPONENT("component"),
    FILE("file"),
    FOLDER("folder"),
    PACKAGE("package"),
    PARENTHESES("parentheses"),
    SCROLL_TEXT("scroll-text"),
    VARIABLE("variable"),
    X("x"),
    COFFEE("coffee"),
    CIRCLE_X("circle-x"),
    INFO("info"),
    HAMMER("hammer"),
    PLAY("play"),
    ELLIPSIS_VERTICAL("ellipsis-vertical"),
    SUN_MOON("sun-moon"),
    BOXES("boxes"),
    SEARCH("search");

    private static final Map<String, FlatSVGIcon> BASE_CACHE = new HashMap<>();

    private final String name;

    LucideIcon(String name) {
        this.name = name;
    }

    private FlatSVGIcon base() {
        return BASE_CACHE.computeIfAbsent(name,
            n -> new FlatSVGIcon("img/lucide/" + n + ".svg"));
    }

    public FlatSVGIcon icon(int size, Color tint) {
        FlatSVGIcon icon = base().derive(size, size);
        icon.setColorFilter(new ColorFilter(c -> tint));

        return icon;
    }
}
