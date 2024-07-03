package com.shub.validation.rules;

import com.shub.validation.core.ICrossFieldValidator;
import com.shub.validation.core.IValidationResult;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class CrossFieldValidator<T, U, V> implements ICrossFieldValidator<T> {
    private final String field1;
    private final String field2;
    private final Function<T, U> getter1;
    private final Function<T, V> getter2;
    private BiPredicate<U, V> predicate;
    private String errorMessage;
    private Predicate<T> condition = t -> true;
    private boolean negateCondition = false;
    private BiConsumer<IValidationResult<T>, String> errorHandler;

    public CrossFieldValidator(String field1, String field2, Function<T, U> getter1, Function<T, V> getter2) {
        this.field1 = field1;
        this.field2 = field2;
        this.getter1 = getter1;
        this.getter2 = getter2;
        this.errorHandler = (result, message) -> {
            result.addError(field1, message);
            result.addError(field2, message);
        };
    }


    public CrossFieldValidator<T, U, V> satisfies(BiPredicate<U, V> predicate, String errorMessage) {
        this.predicate = predicate;
        this.errorMessage = errorMessage;
        return this;
    }

    public CrossFieldValidator<T, U, V> when(Predicate<T> condition) {
        this.condition = condition;
        this.negateCondition = false;
        return this;
    }

    public CrossFieldValidator<T, U, V> otherwise() {
        this.negateCondition = !this.negateCondition;
        return this;
    }

    public CrossFieldValidator<T, U, V> withErrorHandler(BiConsumer<IValidationResult<T>, String> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public CrossFieldValidator<T, U, V> withMessage(Function<T, String> messageProvider) {
        String originalMessage = this.errorMessage;
        this.errorMessage = null;
        this.errorHandler = (result, message) -> {
            String dynamicMessage = messageProvider.apply(result.getValidatedObject());
            result.addError(field1, dynamicMessage);
            result.addError(field2, dynamicMessage);
        };
        return this;
    }

    @Override
    public void validate(T object, IValidationResult<T> result) {
//        if (groups.isEmpty() || !Collections.disjoint(groups, activeGroups)) {
        if (negateCondition ? !condition.test(object) : condition.test(object)) {
            U value1 = getter1.apply(object);
            V value2 = getter2.apply(object);
            if (!predicate.test(value1, value2)) {
                errorHandler.accept(result, errorMessage);
            }
        }
//    }
}
}