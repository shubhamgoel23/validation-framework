package com.shub.validation.core;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface IValidator<T, R> {
    IValidator<T, R> notNull();
    IValidator<T, R> notNull(BiPredicate<T, R> condition);
    IValidator<T, R> notEmpty();
    IValidator<T, R> notEmpty(BiPredicate<T, R> condition);
    IValidator<T, R> satisfies(Predicate<T> predicate, String errorMessage);
    IValidator<T, R> satisfies(Predicate<T> predicate, String errorMessage, BiPredicate<T, R> condition);
    IValidator<T, R> satisfies(BiPredicate<T, R> predicate, String errorMessage);
    IValidator<T, R> satisfies(BiPredicate<T, R> predicate, String errorMessage, BiPredicate<T, R> condition);
    IValidator<T, R> matches(String regex);
    IValidator<T, R> matches(String regex, BiPredicate<T, R> condition);
    IValidator<T, R> greaterThan(Comparable<T> min);
    IValidator<T, R> greaterThan(Comparable<T> min, BiPredicate<T, R> condition);
    <U> IValidator<T, R> nested(String fieldName, Function<T, U> nestedGetter, Consumer<IValidator<U, R>> validator);
    <U> IValidator<T, R> nested(String fieldName, Function<T, U> nestedGetter, Consumer<IValidator<U, R>> validator, BiPredicate<T, R> condition);
    <U> IValidator<T, R> forEach(String fieldName, Class<U> clazz, Consumer<IValidator<U, R>> validator);
    <U> IValidator<T, R> forEach(String fieldName, Class<U> clazz, Consumer<IValidator<U, R>> validator, BiPredicate<T, R> condition);
    IValidator<T, R> compose(ICompositeValidator<T, R> compositeValidator);
    IValidator<T, R> composeConditionally(ICompositeValidator<T, R> compositeValidator);
    IValidator<T, R> composeConditionally(ICompositeValidator<T, R> compositeValidator, BiPredicate<T, R> condition);
    IValidator<T, R> when(BiPredicate<T, R> condition);
    IValidator<T, R> otherwise();
    IValidator<T, R> withMessageSource(IMessageSource messageSource);
    // New method for individual validation
    IValidationResult<R> validate();
    void validate(R root, IValidationResult<R> result);
}