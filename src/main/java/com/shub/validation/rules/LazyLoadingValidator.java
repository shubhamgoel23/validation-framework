package com.shub.validation.rules;

import com.shub.validation.core.IValidationContext;
import com.shub.validation.core.IValidator;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class LazyLoadingValidator<T, R> extends Validator<T, R> {

    public LazyLoadingValidator(Function<R, T> getter, String fieldPath, BiPredicate<T, R> topLevelCondition, R root) {
        super(getter, fieldPath, topLevelCondition, root);
    }

    @Override
    protected IValidator<T, R> addRule(BiPredicate<T, IValidationContext<R>> rule, String errorMessage, BiPredicate<T, R> condition) {
        Supplier<BiPredicate<T, IValidationContext<R>>> lazyRule = () -> rule;
        Supplier<BiPredicate<T, R>> lazyCondition = () -> condition;
        return super.addRule(
                (value, context) -> lazyRule.get().test(value, context),
                errorMessage,
                (value, root) -> lazyCondition.get().test(value, root)
        );
    }
}
