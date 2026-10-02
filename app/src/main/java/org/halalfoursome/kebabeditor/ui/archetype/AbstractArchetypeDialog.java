package org.halalfoursome.kebabeditor.ui.archetype;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.function.Supplier;

import javax.swing.*;

import org.halalfoursome.kebabeditor.ui.validation.Rules;
import org.halalfoursome.kebabeditor.ui.validation.ValidationGroup;
import org.halalfoursome.kebabeditor.utils.KebabStyle;

public abstract class AbstractArchetypeDialog extends JDialog {

    protected final ValidationGroup validation = new ValidationGroup();
    protected final KebabStyle style = KebabStyle.getCurrent();

    private final String confirmText;
    private JButton confirmButton;

    protected AbstractArchetypeDialog(JDialog owner, String title, String confirmText) {
        super(owner, title, true);
        this.confirmText = confirmText;

        addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) {
                validation.hidePopups();
            }

            @Override public void componentResized(ComponentEvent e) {
                validation.hidePopups();
            }
        });
        validation.addListener(this::updateConfirmEnabled);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    // Note: Subclasses must call this at the end of 
    // constructor, when other fields are initialized
    protected final void init(Dimension minimumSize) {
        setMinimumSize(minimumSize);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 16, 4, 16);
        buildForm(form, constraints);

        add(form, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        // Escape closes the dialog
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        pack();
        setLocationRelativeTo(getOwner());
    }

    protected abstract void buildForm(JPanel form, GridBagConstraints constraints);

    protected abstract void onConfirm();

    protected void addLabeled(
        JPanel form, GridBagConstraints constraints, String text, JComponent component
    ) {
        form.add(label(text), constraints);
        form.add(component, constraints);
    }

    protected JTextField addIdField(
        JPanel form, GridBagConstraints constraints, Supplier<Collection<String>> existingIds
    ) {
        JTextField idField = new JTextField();
        idField.setFont(style.uiFont());
        validation.add(idField, Rules.all(
            Rules.identifier("ID"),
            Rules.unique("ID", existingIds)
        ));
        addLabeled(form, constraints, "ID", idField);

        JLabel idNote = label("The ID cannot be changed after the archetype is created.");
        idNote.setFont(style.uiFont().deriveFont(Font.ITALIC, style.uiFont().getSize2D() - 1f));
        form.add(idNote, constraints);

        return idField;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(style.uiFont());
        return label;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        confirmButton = new JButton(confirmText);
        confirmButton.setFont(style.uiFont());
        updateConfirmEnabled();
        confirmButton.addActionListener(e -> {
            onConfirm();
            dispose();
        });
        footer.add(confirmButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(style.uiFont());
        cancelButton.addActionListener(e -> dispose());
        footer.add(cancelButton);

        return footer;
    }

    private void updateConfirmEnabled() {
        if (confirmButton != null) {
            confirmButton.setEnabled(validation.isValid());
        }
    }
}
