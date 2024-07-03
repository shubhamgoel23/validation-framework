package com.shub.validation.core;

import java.util.function.BiPredicate;

public interface IValidationRule<T, R> {
    boolean test(T value, IValidationContext<R> context);
    String getErrorMessage();
    BiPredicate<T, R> getCondition();
}
