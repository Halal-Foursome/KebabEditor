package org.halalfoursome.kebabeditor.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.utils.Maybe;

public final class CollapsiblePanel {

    private final JSplitPane split;
    private final JComponent panel;

    private Maybe<Integer> savedLocation = Maybe.none();
    private Dimension savedMinimum;

    // The panel must be the right or bottom component of the split
    public CollapsiblePanel(JSplitPane split, JComponent panel) {
        this.split = split;
        this.panel = panel;
    }

    public void toggle() {
        if (isCollapsed()) {
            expand();
        } else {
            collapse();
        }
    }

    public boolean isShown() {
        boolean vertical = split.getOrientation() == JSplitPane.VERTICAL_SPLIT;

        return (vertical ? panel.getHeight() : panel.getWidth()) > 1;
    }

    // Also runs when the divider is dragged by hand
    public void addListener(Runnable listener) {
        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                listener.run();
            }
        });
    }

    private boolean isCollapsed() {
        return savedLocation.isSome() && !isShown();
    }

    private void collapse() {
        if (savedMinimum != null) {
            panel.setMinimumSize(savedMinimum);
        }
        savedMinimum = panel.getMinimumSize();
        savedLocation = Maybe.some(split.getDividerLocation());

        releaseFocus();
        // The divider cannot pass the panel's minimum size, so that is lifted while it is hidden
        panel.setMinimumSize(new Dimension(0, 0));
        split.setDividerLocation(split.getMaximumDividerLocation());
    }

    private void expand() {
        panel.setMinimumSize(savedMinimum);
        split.setDividerLocation(savedLocation.unwrap());
        savedLocation = Maybe.none();
        savedMinimum = null;
    }

    private void releaseFocus() {
        KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Component owner = focus.getFocusOwner();
        if (owner != null && SwingUtilities.isDescendingFrom(owner, panel)) {
            focus.clearFocusOwner();
        }
    }
}
