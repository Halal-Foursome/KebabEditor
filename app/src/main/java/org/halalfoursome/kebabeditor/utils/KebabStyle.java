package org.halalfoursome.kebabeditor.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.UIManager;

import lombok.Setter;

public record KebabStyle(
    Font uiFont,
    Font techFont,
    FavIcon favicon,
    Dimension windowSize
) {
    @Setter
    private static KebabStyle current;

    public static final int SIDEBAR_WIDTH = 150;
    public static final int BOTTOM_BAR_HEIGHT = 150;
    public static final int WINDOW_WIDTH = 1600;
    public static final int WINDOW_HEIGHT = 900;
    public static final int ICON_WIDTH = 18;
    public static final int ICON_HEIGHT = 18;
    public static final String DEFAULT_UI_FONT = "fonts/IBM_Plex_Sans/IBMPlexSans-VariableFont_wdth,wght.ttf";
    public static final String DEFAULT_TECH_FONT = "fonts/IBM_Plex_Mono/IBMPlexMono-Regular.ttf";
    public static final String DEFAULT_ICON = "img/logo.svg";

    public static KebabStyle getCurrent() {
        if (current == null) {
            throw new IllegalStateException("KebabStyle not set. Call KebabStyle.setCurrent() before accessing the current style.");
        }
        
        return current;
    }

    public static KebabStyle defaultStyle() {
        AssetLoader assetLoader = AssetLoader.getInstance();
        FavIcon favicon = assetLoader.loadIcon(DEFAULT_ICON);
        Font uiFont = assetLoader.loadFont(DEFAULT_UI_FONT, 16f, false);
        Font techFont = assetLoader.loadFont(DEFAULT_TECH_FONT, 14f, true);
        Dimension windowSize = new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT);

        return new KebabStyle(
            uiFont, 
            techFont,
            favicon,
            windowSize
        );
    }

    public Color getBackgroundColor() {
        return UIManager.getColor("Panel.background");
    }

    public boolean isDarkTheme() {
        Color bg = getBackgroundColor();

        double brightness = (
            0.299 * bg.getRed() +
            0.587 * bg.getGreen() +
            0.114 * bg.getBlue()
        );

        return brightness < 128;
    }

    public Color shiftAccent(float fraction) {
        Color bg = getBackgroundColor();
        if (isDarkTheme()) {
            return KebabStyle.darken(bg, fraction);
        } else {
            return KebabStyle.lighten(bg, fraction);
        }
    }

    public static Color lighten(Color color, float fraction) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r += (int)((255 - r) * fraction);
        g += (int)((255 - g) * fraction);
        b += (int)((255 - b) * fraction);

        return new Color(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    public static Color darken(Color color, float fraction) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r -= (int)(r * fraction);
        g -= (int)(g * fraction);
        b -= (int)(b * fraction);

        return new Color(Math.max(r, 0), Math.max(g, 0), Math.max(b, 0));
    }
}

