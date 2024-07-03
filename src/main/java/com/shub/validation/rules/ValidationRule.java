package com.shub.validation.rules;

import com.shub.validation.core.IValidationContext;
import com.shub.validation.core.IValidationRule;

import java.util.function.BiPredicate;

public class ValidationRule<T, R> implements IValidationRule<T, R> {
    private final BiPredicate<T, IValidationContext<R>> rule;
    private final String errorMessage;
    private final BiPredicate<T, R> condition;

    public ValidationRule(BiPredicate<T, IValidationContext<R>> rule, String errorMessage, BiPredicate<T, R> condition) {
        this.rule = rule;
        this.errorMessage = errorMessage;
        this.condition = condition;
    }

    @Override
    public boolean test(T value, IValidationContext<R> context) {
        return rule.test(value, context);
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public BiPredicate<T, R> getCondition() {
        return condition;
    }
}
