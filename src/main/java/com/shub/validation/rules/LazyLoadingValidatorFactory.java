package com.shub.validation.rules;

import com.shub.validation.core.IValidator;
import com.shub.validation.core.IValidatorFactory;

import java.util.function.BiPredicate;
import java.util.function.Function;

public class LazyLoadingValidatorFactory implements IValidatorFactory {
    @Override
    public <T, R> IValidator<T, R> createValidator(Function<R, T> getter, String fieldPath, BiPredicate<T, R> topLevelCondition, R root) {
        return new LazyLoadingValidator<>(getter, fieldPath, topLevelCondition, root);
    }
}