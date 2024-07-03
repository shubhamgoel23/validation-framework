package com.shub.validation.rules;

import com.shub.validation.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class ValidationBuilder<T> implements IValidationBuilder<T> {
    private final T object;
    private final List<IValidator<?, T>> validators = new ArrayList<>();
    private final List<IValidatorPlugin<?, T>> plugins = new ArrayList<>();
    private final IValidatorFactory validatorFactory;
    private final List<ICrossFieldValidator<T>> crossFieldValidators = new ArrayList<>();

    public ValidationBuilder(T object) {
        this(object, new DefaultValidatorFactory());
    }

    public ValidationBuilder(T object, IValidatorFactory validatorFactory) {
        this.object = object;
        this.validatorFactory = validatorFactory;
    }

    @Override
    public <U> IValidator<U, T> ruleFor(String fieldName, Function<T, U> getter) {
        return ruleFor(fieldName, getter, (u, t) -> true);
    }

    @Override
    public <U> IValidator<U, T> ruleFor(String fieldName, Function<T, U> getter, BiPredicate<U, T> condition) {
        IValidator<U, T> validator = validatorFactory.createValidator(getter, fieldName, condition, object);
        validators.add(validator);
        return validator;
    }

    @Override
    public <U> IValidationBuilder<T> registerPlugin(IValidatorPlugin<U, T> plugin) {
        plugins.add((IValidatorPlugin<?, T>) plugin);
        return this;
    }

    public <U, V> CrossFieldValidator<T, U, V> ruleForCombination(
            String field1, String field2,
            Function<T, U> getter1, Function<T, V> getter2) {
        CrossFieldValidator<T, U, V> validator = new CrossFieldValidator<>(field1, field2, getter1, getter2);
        crossFieldValidators.add(validator);
        return validator;
    }

    public <U, V> CrossFieldValidator<T, U, V> ruleForCombination(
            String field1, String field2,
            Function<T, U> getter1, Function<T, V> getter2,
            Predicate<T> condition) {
        CrossFieldValidator<T, U, V> validator = new CrossFieldValidator<>(field1, field2, getter1, getter2);
        validator.when(condition);
        crossFieldValidators.add(validator);
        return validator;
    }

    @Override
    public IValidationResult<T> validate() {
        IValidationResult<T> result = new ValidationResult<>(object);
        for (IValidator<?, T> validator : validators) {
            validator.validate(object, result);
        }
        for (ICrossFieldValidator<T> validator : crossFieldValidators) {
            validator.validate(object, result);
        }
        applyPlugins(result);
        return result;
    }

    private void applyPlugins(IValidationResult<T> result) {
        for (IValidatorPlugin<?, T> plugin : plugins) {
            for (IValidator<?, T> validator : validators) {
                applyPluginToValidator(plugin, validator, result);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <U> void applyPluginToValidator(IValidatorPlugin<?, T> plugin, IValidator<?, T> validator, IValidationResult<T> result) {
        try {
            ((IValidatorPlugin<U, T>) plugin).apply((IValidator<U, T>) validator, result);
        } catch (ClassCastException e) {
            System.out.println("Warning: Plugin " + plugin.getClass().getSimpleName() +
                    " is not compatible with validator " + validator.getClass().getSimpleName());
        }
    }
}