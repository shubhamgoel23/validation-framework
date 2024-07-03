package com.shub.validation.core;

import java.util.function.BiPredicate;
import java.util.function.Function;


public interface IValidationBuilder<T> {
    <U> IValidator<U, T> ruleFor(String fieldName, Function<T, U> getter);
    <U> IValidator<U, T> ruleFor(String fieldName, Function<T, U> getter, BiPredicate<U, T> condition);
    <U> IValidationBuilder<T> registerPlugin(IValidatorPlugin<U, T> plugin);
    IValidationResult validate();
}