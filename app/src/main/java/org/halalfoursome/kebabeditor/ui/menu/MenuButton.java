package org.halalfoursome.kebabeditor.ui.menu;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.UIManager;

import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.LucideIcon;

public class MenuButton extends JButton {
    public static final Color PLAY_BUTTON_COLOR = new Color(0x87e449);

    public MenuButton(LucideIcon icon, String tooltip, Color color, Insets insets, Runnable action) {
        super(icon.icon(18, color));

        setMargin(insets);
        setBackground(KebabStyle.darken(UIManager.getColor("TitlePane.background"), 0.1f));
        setToolTipText(tooltip);
    }

    public MenuButton(LucideIcon icon, String tooltip, Color color, Runnable runnable) {
        this(icon, tooltip, color, new Insets(8, 8, 8, 8), runnable);
    }

    public MenuButton(LucideIcon icon, String tooltip, Insets insets, Runnable runnable) {
        this(icon, tooltip, Color.LIGHT_GRAY, insets, runnable);
    }

    public MenuButton(LucideIcon icon, String tooltip, Runnable runnable) {
        this(icon, tooltip, Color.LIGHT_GRAY, new Insets(8, 8, 8, 8), runnable);
    }
}
