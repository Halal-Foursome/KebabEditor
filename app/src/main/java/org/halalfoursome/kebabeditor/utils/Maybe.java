package org.halalfoursome.kebabeditor.utils;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.halalfoursome.kebabeditor.utils.Maybe.*;

public sealed interface Maybe<T> extends Serializable
    permits
        Some,
        None
{
    public record Some<T>(T value) implements Maybe<T> {
        public Some {
            Objects.requireNonNull(value, "Some can't hold null, use Maybe.none()");
        }
    }

    public record None<T>() implements Maybe<T> {}

    // Constructors

    static <T> Maybe<T> some(T value) {
        return new Some<>(value);
    }

    static <T> Maybe<T> none() {
        return new None<>();
    }

    static <T> Maybe<T> ofNullable(T value) {
        return value == null ? none() : some(value);
    }

    static <T> Maybe<T> fromOptional(Optional<T> optional) {
        return optional.map(Maybe::some).orElseGet(Maybe::none);
    }

    // Querying

    default boolean isSome() {
        return this instanceof Some;
    }

    default boolean isNone() {
        return this instanceof None;
    }

    default boolean isSomeAnd(Predicate<? super T> predicate) {
        return switch (this) {
            case Some<T>(var value) -> predicate.test(value);
            case None<T> _ -> false;
        };
    }

    // Extracting

    default T unwrap() {
        return expect("called `Maybe.unwrap()` on a `None` value");
    }

    default T expect(String message) {
        return switch (this) {
            case Some<T>(var value) -> value;
            case None<T> _ -> throw new NoSuchElementException(message);
        };
    }

    default T unwrapOr(T fallback) {
        return switch (this) {
            case Some<T>(var value) -> value;
            case None<T> _ -> fallback;
        };
    }

    default T unwrapOrElse(Supplier<? extends T> fallback) {
        return switch (this) {
            case Some<T>(var value) -> value;
            case None<T> _ -> fallback.get();
        };
    }

    default T unwrapOrNull() {
        return unwrapOr(null);
    }

    // Transforming

    default <U> Maybe<U> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Some<T>(var value) -> some(mapper.apply(value));
            case None<T> _ -> none();
        };
    }

    default <U> U mapOr(U fallback, Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Some<T>(var value) -> mapper.apply(value);
            case None<T> _ -> fallback;
        };
    }

    default <U> U mapOrElse(Supplier<? extends U> fallback, Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Some<T>(var value) -> mapper.apply(value);
            case None<T> _ -> fallback.get();
        };
    }

    default <U> Maybe<U> andThen(Function<? super T, Maybe<U>> mapper) {
        return switch (this) {
            case Some<T>(var value) -> mapper.apply(value);
            case None<T> _ -> none();
        };
    }

    default Maybe<T> filter(Predicate<? super T> predicate) {
        return switch (this) {
            case Some<T>(var value) when predicate.test(value) -> this;
            default -> none();
        };
    }

    // Combining

    default <U> Maybe<U> and(Maybe<U> other) {
        return isSome() ? other : none();
    }

    default Maybe<T> or(Maybe<T> other) {
        return isSome() ? this : other;
    }

    default Maybe<T> orElse(Supplier<Maybe<T>> other) {
        return isSome() ? this : other.get();
    }

    // Side effects

    default Maybe<T> inspect(Consumer<? super T> action) {
        if (this instanceof Some<T>(var value)) {
            action.accept(value);
        }
        return this;
    }

    default void ifSome(Consumer<? super T> action) {
        inspect(action);
    }

    default void ifSomeOrElse(Consumer<? super T> action, Runnable otherwise) {
        switch (this) {
            case Some<T>(var value) -> action.accept(value);
            case None<T> _ -> otherwise.run();
        }
    }

    // Interop

    default Optional<T> toOptional() {
        return switch (this) {
            case Some<T>(var value) -> Optional.of(value);
            case None<T> _ -> Optional.empty();
        };
    }
}
