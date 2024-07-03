package com.shub.validation.core;

import java.util.function.BiPredicate;
import java.util.function.Function;

public interface IValidatorFactory {
    <T, R> IValidator<T, R> createValidator(Function<R, T> getter, String fieldPath, BiPredicate<T, R> topLevelCondition, R root);
}
