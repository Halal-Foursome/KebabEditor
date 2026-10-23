package org.halalfoursome.kebabeditor.ui.menu.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.UIManager;

import org.halalfoursome.kebabeditor.utils.LucideIcon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MenuButtonTest {

    @BeforeEach
    void titleBarColor() {
        // Normally set by the FlatLaf theme the main window installs.
        UIManager.put("TitlePane.background", Color.DARK_GRAY);
    }

    @Test
    void clickRunsTheAction() {
        int[] runs = { 0 };
        MenuButton button = new MenuButton(LucideIcon.PLAY, "Run", MenuButton.PLAY_BUTTON_COLOR, () -> runs[0]++);

        button.doClick();
        button.doClick();

        assertEquals(2, runs[0]);
    }

    @Test
    void appearanceSwapsIconAndTooltip() {
        MenuButton button = new MenuButton(LucideIcon.PLAY, "Run", MenuButton.PLAY_BUTTON_COLOR, () -> {});
        Icon before = button.getIcon();

        button.setAppearance(LucideIcon.SQUARE, MenuButton.STOP_BUTTON_COLOR, "Stop");

        assertEquals("Stop", button.getToolTipText());
        assertNotSame(before, button.getIcon());
    }
}
