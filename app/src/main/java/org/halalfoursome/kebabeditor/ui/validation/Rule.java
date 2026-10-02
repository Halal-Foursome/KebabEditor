package org.halalfoursome.kebabeditor.ui.validation;

import org.halalfoursome.kebabeditor.utils.Maybe;

@FunctionalInterface
public interface Rule {

    /** 
     * Returns an error message, or None if the text is valid. 
     */
    Maybe<String> check(String text);
}
