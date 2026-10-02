package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.halalfoursome.kebabeditor.archetype.ArchetypeIcon;
import org.halalfoursome.kebabeditor.utils.FileChooser;
import org.halalfoursome.kebabeditor.utils.FileChooser.FileChooserMode;
import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class IconPickerPanel extends JPanel {

    private final JTextField pathField = new JTextField();
    private Maybe<ArchetypeIcon> selected;

    public IconPickerPanel(KebabStyle style) {
        this(style, Maybe.none());
    }

    public IconPickerPanel(KebabStyle style, Maybe<ArchetypeIcon> initial) {
        super(new BorderLayout(8, 0));

        selected = initial;
        if (initial.unwrapOrNull() instanceof ArchetypeIcon.Handle handle) {
            pathField.setText(handle.path());
        }

        pathField.setEditable(false);
        pathField.setFont(style.uiFont());
        add(pathField, BorderLayout.CENTER);

        JButton browse = new JButton("Browse...");
        browse.setFont(style.uiFont());
        browse.addActionListener(e -> {
            FileChooser chooser = new FileChooser(FileChooserMode.FILES)
                .addFilter("Images (png, jpg, gif, svg)", "png", "jpg", "jpeg", "gif", "svg");

            chooser.open(SwingUtilities.getWindowAncestor(this), file -> {
                selected = Maybe.some(new ArchetypeIcon.Handle(file.getAbsolutePath()));
                pathField.setText(file.getAbsolutePath());
            });
        });
        add(browse, BorderLayout.EAST);
    }

    public Maybe<ArchetypeIcon> selectedIcon() {
        return selected;
    }
}
