package com.shub.validation.rules;

import com.shub.validation.core.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Validator<T, R> implements IValidator<T, R> {
    private final Function<R, T> getter;
    private final String fieldPath;
    private final List<IValidationRule<T, R>> rules = new ArrayList<>();
    private final BiPredicate<T, R> topLevelCondition;
    private IMessageSource messageSource;
    private BiPredicate<T, R> currentCondition;
    private R root;  // Store the root object

    public Validator(Function<R, T> getter, String fieldPath, BiPredicate<T, R> topLevelCondition, R root) {
        this.getter = getter;
        this.fieldPath = fieldPath;
        this.topLevelCondition = topLevelCondition;
        this.currentCondition = (t, r) -> true;
        this.root = root;
    }
    @Override
    public IValidator<T, R> notNull() {
        return notNull((t, r) -> true);
    }

    @Override
    public IValidator<T, R> notNull(BiPredicate<T, R> condition) {
        return addRule((value, context) -> value != null, "must not be null", condition);
    }

    @Override
    public IValidator<T, R> notEmpty() {
        return notEmpty((t, r) -> true);
    }

    @Override
    public IValidator<T, R> notEmpty(BiPredicate<T, R> condition) {
        return addRule((value, context) -> {
            if (value == null) return false;
            if (value instanceof String) return !((String) value).isEmpty();
            if (value instanceof Collection) return !((Collection<?>) value).isEmpty();
            if (value.getClass().isArray()) return ((Object[]) value).length > 0;
            return true;
        }, "must not be empty", condition);
    }

    @Override
    public IValidator<T, R> satisfies(Predicate<T> predicate, String errorMessage) {
        return satisfies(predicate, errorMessage, (t, r) -> true);
    }

    @Override
    public IValidator<T, R> satisfies(Predicate<T> predicate, String errorMessage, BiPredicate<T, R> condition) {
        return satisfies((value, root) -> predicate.test(value), errorMessage, condition);
    }

    @Override
    public IValidator<T, R> satisfies(BiPredicate<T, R> predicate, String errorMessage) {
        return satisfies(predicate, errorMessage, (t, r) -> true);
    }

    @Override
    public IValidator<T, R> satisfies(BiPredicate<T, R> predicate, String errorMessage, BiPredicate<T, R> condition) {
        return addRule((value, context) -> value == null || predicate.test(value, context.getRoot()), errorMessage, condition);
    }

    @Override
    public IValidator<T, R> matches(String regex) {
        return matches(regex, (t, r) -> true);
    }

    @Override
    public IValidator<T, R> matches(String regex, BiPredicate<T, R> condition) {
        return addRule((value, context) -> value != null && Pattern.matches(regex, value.toString()),
                "must match pattern: " + regex, condition);
    }

    @Override
    public IValidator<T, R> greaterThan(Comparable<T> min) {
        return greaterThan(min, (t, r) -> true);
    }

    @Override
    public IValidator<T, R> greaterThan(Comparable<T> min, BiPredicate<T, R> condition) {
        return addRule((value, context) -> value != null && min.compareTo((T) value) < 0,
                "must be greater than " + min, condition);
    }

    @Override
    public <U> IValidator<T, R> nested(String fieldName, Function<T, U> nestedGetter, Consumer<IValidator<U, R>> validator) {
        return nested(fieldName, nestedGetter, validator, (t, r) -> true);
    }

    @Override
    public <U> IValidator<T, R> nested(String fieldName, Function<T, U> nestedGetter, Consumer<IValidator<U, R>> validator, BiPredicate<T, R> condition) {
        String nestedPath = fieldPath.isEmpty() ? fieldName : fieldPath + "." + fieldName;
        IValidator<U, R> nestedValidator = new Validator<>(root -> {
            T value = this.getter.apply(root);
            return value != null ? nestedGetter.apply(value) : null;
        }, nestedPath, (u, r) -> true, root);
        validator.accept(nestedValidator);
        return addRule((value, context) -> {
            if (value != null) {
                nestedValidator.validate(context.getRoot(), context.getResult());
            }
            return true;
        }, null, condition);
    }

    @Override
    public <U> IValidator<T, R> forEach(String fieldName, Class<U> clazz, Consumer<IValidator<U, R>> validator) {
        return forEach(fieldName, clazz, validator, (t, r) -> true);
    }

    @Override
    public <U> IValidator<T, R> forEach(String fieldName, Class<U> clazz, Consumer<IValidator<U, R>> validator, BiPredicate<T, R> condition) {
        String listPath = fieldPath.isEmpty() ? fieldName : fieldPath + (fieldName.isEmpty() ? "" : "." + fieldName);
        return addRule((value, context) -> {
            if (value instanceof Collection) {
                Collection<?> collection = (Collection<?>) value;
                int index = 0;
                for (Object item : collection) {
                    if (clazz.isInstance(item)) {
                        String indexedPath = listPath + "[" + index + "]";
                        IValidator<U, R> itemValidator = new Validator<>(root -> clazz.cast(item), indexedPath, (u, r) -> true, root);
                        validator.accept(itemValidator);
                        itemValidator.validate(context.getRoot(), context.getResult());
                    } else {
                        context.getResult().addError(listPath + "[" + index + "]", "is not of type " + clazz.getSimpleName());
                    }
                    index++;
                }
            }
            return true;
        }, null, condition);
    }

    @Override
    public IValidator<T, R> compose(ICompositeValidator<T, R> compositeValidator) {
        return composeConditionally(compositeValidator, compositeValidator.getDefaultCondition());
    }

    @Override
    public IValidator<T, R> composeConditionally(ICompositeValidator<T, R> compositeValidator) {
        return composeConditionally(compositeValidator, compositeValidator.getDefaultCondition());
    }

    @Override
    public IValidator<T, R> composeConditionally(ICompositeValidator<T, R> compositeValidator, BiPredicate<T, R> condition) {
        return addRule((value, context) -> {
            IValidator<T, R> conditionalValidator = new Validator<>(t -> value, this.fieldPath, (t, r) -> true, root);
            compositeValidator.applyTo(conditionalValidator);
            conditionalValidator.validate(context.getRoot(), context.getResult());
            return context.getResult().isValid();
        }, null, condition.and(compositeValidator.getDefaultCondition()));
    }

    @Override
    public IValidator<T, R> when(BiPredicate<T, R> condition) {
        this.currentCondition = condition;
        return this;
    }

    @Override
    public IValidator<T, R> otherwise() {
        this.currentCondition = this.currentCondition.negate();
        return this;
    }

    @Override
    public IValidator<T, R> withMessageSource(IMessageSource messageSource) {
        this.messageSource = messageSource;
        return this;
    }

    protected IValidator<T, R> addRule(BiPredicate<T, IValidationContext<R>> rule, String errorMessage, BiPredicate<T, R> condition) {
        rules.add(new ValidationRule<>(rule, errorMessage, condition.and(this.currentCondition)));
        this.currentCondition = (t, r) -> true;  // Reset the current condition
        return this;
    }


    @Override
    public void validate(R root, IValidationResult<R> result) {
        T value = getter.apply(root);
        if (topLevelCondition.test(value, root)) {
//            boolean shouldValidate = groups.isEmpty() || !Collections.disjoint(groups, activeGroups);
//            if (shouldValidate) {
                for (IValidationRule<T, R> rule : rules) {
                    if (rule.getCondition().test(value, root) && !rule.test(value, new ValidationContext<>(root, result))) {
                        if (rule.getErrorMessage() != null) {
                            String message = messageSource != null
                                    ? messageSource.getMessage(rule.getErrorMessage(), new Object[]{}, Locale.getDefault())
                                    : rule.getErrorMessage();
                            result.addError(fieldPath, message);
                        }
                    }
                }
//            }
        }
    }

    @Override
    public IValidationResult<R> validate() {
        IValidationResult<R> result = new ValidationResult<>(root);
        validate(root, result);
        return result;
    }
}