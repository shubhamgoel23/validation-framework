package com.shub.validation.rules;

import com.shub.validation.core.ICompositeValidator;
import com.shub.validation.core.IValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class CompositeValidator<T, R> implements ICompositeValidator<T, R> {
    private final List<Consumer<IValidator<T, R>>> rules = new ArrayList<>();
    private BiPredicate<T, R> defaultCondition = (t, r) -> true;

    @Override
    public ICompositeValidator<T, R> add(Consumer<IValidator<T, R>> rule) {
        rules.add(rule);
        return this;
    }

    @Override
    public ICompositeValidator<T, R> withDefaultCondition(BiPredicate<T, R> condition) {
        this.defaultCondition = condition;
        return this;
    }

    @Override
    public void applyTo(IValidator<T, R> validator) {
        rules.forEach(rule -> rule.accept(validator));
    }

    @Override
    public BiPredicate<T, R> getDefaultCondition() {
        return defaultCondition;
    }
}
