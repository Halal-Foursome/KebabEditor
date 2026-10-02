package org.halalfoursome.kebabeditor.ui.validation;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.halalfoursome.kebabeditor.utils.KebabStyle;
import org.halalfoursome.kebabeditor.utils.Maybe;

public final class FieldValidator {

    private static final Color ERROR_COLOR = new Color(0xB00020);
    private static final Border ERROR_BORDER = BorderFactory.createLineBorder(ERROR_COLOR);

    private final JTextField field;
    private final Rule rule;
    private final JPopupMenu popup = new JPopupMenu();
    private final JLabel message = new JLabel();
    private boolean touched;

    public FieldValidator(JTextField field, Rule rule, Runnable onChange) {
        this.field = field;
        this.rule = rule;

        message.setFont(KebabStyle.getCurrent().uiFont());
        message.setForeground(ERROR_COLOR);
        message.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        popup.setFocusable(false);
        popup.add(message);

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override 
            public void insertUpdate(DocumentEvent e) { 
                touched = true; 
                onChange.run(); 
            }

            @Override public void removeUpdate(DocumentEvent e) { 
                touched = true; 
                onChange.run(); 
            }

            @Override public void changedUpdate(DocumentEvent e) {}
        });
        
        field.addFocusListener(new FocusAdapter() {
            @Override public void 
            focusGained(FocusEvent e) { 
                updateVisuals();
            }

            @Override public void focusLost(FocusEvent e) { 
                hidePopup();
            }
        });
    }

    public boolean isValid() {
        return rule.check(field.getText()).isNone();
    }

    public void hidePopup() {
        popup.setVisible(false);
    }

    public void updateVisuals() {
        Maybe<String> error = rule.check(field.getText());
        field.setBorder(error.isSome() && touched ? ERROR_BORDER : UIManager.getBorder("TextField.border"));

        if (error.isNone() || !touched || !field.isShowing() || !field.hasFocus()) {
            hidePopup();
            return;
        }
        
        message.setText(error.unwrap());
        popup.pack();
        popup.show(field, 0, field.getHeight());
    }
}
