package com.shub.validation.core;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

public interface ICompositeValidator<T, R> {
    ICompositeValidator<T, R> add(Consumer<IValidator<T, R>> rule);
    ICompositeValidator<T, R> withDefaultCondition(BiPredicate<T, R> condition);
    void applyTo(IValidator<T, R> validator);
    BiPredicate<T, R> getDefaultCondition();
}
