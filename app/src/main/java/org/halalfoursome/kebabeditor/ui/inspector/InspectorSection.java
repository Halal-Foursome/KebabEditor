package org.halalfoursome.kebabeditor.ui.inspector;

import java.awt.BorderLayout;
import javax.swing.*;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

/** Standard Swing disclosure section; no custom theme or painting. */
class InspectorSection extends JPanel {
    final JPanel body = new JPanel();
    final JToggleButton heading;

    InspectorSection(String title, Icon icon) {
        super(new BorderLayout(0, 6));
        heading = new JToggleButton(title, UIManager.getIcon("Tree.expandedIcon"), true);
        heading.setHorizontalAlignment(SwingConstants.LEADING);
        heading.setFont(KebabStyle.getCurrent().uiFont());
        heading.addActionListener(e -> {
            body.setVisible(heading.isSelected());
            heading.setIcon(UIManager.getIcon(heading.isSelected() ? "Tree.expandedIcon" : "Tree.collapsedIcon"));
            revalidate();
        });
        JPanel header = new JPanel(new BorderLayout(4, 0));
        header.add(heading, BorderLayout.CENTER);
        if (icon != null) header.add(new JLabel(icon), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
    }
}
