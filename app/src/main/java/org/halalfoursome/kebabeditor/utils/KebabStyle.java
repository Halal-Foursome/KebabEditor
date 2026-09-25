package org.halalfoursome.kebabeditor.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.SwingConstants;
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

    public void styleScrollbarButton(JButton button) {
        button.setFont(uiFont);

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        button.putClientProperty("selected", false);

        Color normal = getBackgroundColor();
        Color hover = shiftAccent(0.08f);
        Color selected = shiftAccent(0.40f);

        button.setBackground(normal);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!(Boolean) button.getClientProperty("selected")) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if ((Boolean) button.getClientProperty("selected")) {
                    button.setBackground(selected);
                } else {
                    button.setBackground(normal);
                }
            }
        });
    }

    public void setScrollbarButtonSelected(JButton button, boolean selected) {
        button.putClientProperty("selected", selected);

        button.setBackground(
            selected
                ? shiftAccent(0.40f)
                : getBackgroundColor()
        );
    }
}

