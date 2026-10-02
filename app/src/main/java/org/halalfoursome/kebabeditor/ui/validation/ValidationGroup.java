package org.halalfoursome.kebabeditor.ui.validation;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

public final class ValidationGroup {

    private final List<FieldValidator> validators = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public FieldValidator add(JTextField field, Rule rule) {
        FieldValidator validator = new FieldValidator(field, rule, this::fireChanged);
        validators.add(validator);
        fireChanged();
        return validator;
    }

    public void remove(FieldValidator validator) {
        validator.hidePopup();
        validators.remove(validator);
        fireChanged();
    }

    public boolean isValid() {
        return validators.stream().allMatch(FieldValidator::isValid);
    }

    public void hidePopups() {
        validators.forEach(FieldValidator::hidePopup);
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    // other fields may depend on the changed one, so all of them are re-checked
    private void fireChanged() {
        validators.forEach(FieldValidator::updateVisuals);
        listeners.forEach(Runnable::run);
    }
}
