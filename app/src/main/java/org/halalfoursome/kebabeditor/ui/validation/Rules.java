package org.halalfoursome.kebabeditor.ui.validation;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.halalfoursome.kebabeditor.utils.Maybe;

public final class Rules {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]+");

    private Rules() {}

    public static Rule notEmpty(String name) {
        return text -> text.isBlank() 
            ? Maybe.some(name + " must not be empty") 
            : Maybe.none();
    }

    public static Rule identifier(String name) {
        return text -> {
            if (text.isEmpty()) {
                return Maybe.some(name + " must not be empty");
            }
            
            if (!IDENTIFIER.matcher(text).matches()) {
                return Maybe.some(name + " should only contain A-Z, a-z, _ and be `snake_case`");
            }

            return Maybe.none();
        };
    }

    /** Fails if another field already holds the same text, ignoring case. */
    public static Rule unique(String name, Supplier<Collection<String>> others) {
        return text -> others.get().stream().anyMatch(other -> other.equalsIgnoreCase(text.trim()))
            ? Maybe.some(name + " must be unique")
            : Maybe.none();
    }

    /** Applies the rules in order and reports the first error. */
    public static Rule all(Rule... rules) {
        return text -> {
            for (Rule rule : rules) {
                Maybe<String> error = rule.check(text);
                if (error.isSome()) {
                    return error;
                }
            }
            return Maybe.none();
        };
    }
}
