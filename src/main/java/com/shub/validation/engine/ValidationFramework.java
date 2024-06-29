package com.shub.validation.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ValidationFramework {

    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public static class CompositeValidator<T, R> {
        private List<Consumer<Validator<T, R>>> rules = new ArrayList<>();
        private BiPredicate<T, R> defaultCondition = (t, r) -> true;

        public CompositeValidator<T, R> add(Consumer<Validator<T, R>> rule) {
            rules.add(rule);
            return this;
        }

        public CompositeValidator<T, R> withDefaultCondition(BiPredicate<T, R> condition) {
            this.defaultCondition = condition;
            return this;
        }

        public void applyTo(Validator<T, R> validator) {
            rules.forEach(rule -> rule.accept(validator));
        }

        public BiPredicate<T, R> getDefaultCondition() {
            return defaultCondition;
        }
    }


    public static class Validator<T, R> {
        private Function<R, T> getter;
        private String fieldName;
        private List<ConditionalValidation<T, R>> validations = new ArrayList<>();

        public Validator(Function<R, T> getter, String fieldName) {
            this.getter = getter;
            this.fieldName = fieldName;
        }

        private Validator<T, R> addValidation(TriConsumer<T, ValidationResult, R> validation) {
            validations.add(new ConditionalValidation<>(validation, (t, r) -> true));
            return this;
        }

        public Validator<T, R> notNull() {
            return addValidation((value, result, root) -> {
                if (value == null) {
                    result.addError(fieldName + " cannot be null");
                }
            });
        }

        public Validator<T, R> matches(String regex) {
            return addValidation((value, result, root) -> {
                if (value != null && !Pattern.matches(regex, value.toString())) {
                    result.addError(fieldName + " does not match pattern " + regex);
                }
            });
        }

        public Validator<T, R> satisfies(Predicate<T> predicate, String errorMessage) {
            return addValidation((value, result, root) -> {
                if (value != null && !predicate.test(value)) {
                    result.addError(fieldName + ": " + errorMessage);
                }
            });
        }

        public <U> Validator<T, R> forEach(Class<U> clazz, Consumer<Validator<U, R>> validator) {
            return addValidation((value, result, root) -> {
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (clazz.isInstance(item)) {
                            Validator<U, R> itemValidator = new Validator<>(r -> clazz.cast(item), fieldName + "[" + i + "]");
                            validator.accept(itemValidator);
                            itemValidator.validate(root, result);
                        } else {
                            result.addError(fieldName + "[" + i + "] is not of type " + clazz.getSimpleName());
                        }
                    }
                }
            });
        }

        public <U> Validator<T, R> nested(Function<T, U> getter, Consumer<Validator<U, R>> validator) {
            Validator<U, R> nestedValidator = new Validator<>(root -> {
                T value = this.getter.apply(root);
                return value != null ? getter.apply(value) : null;
            }, this.fieldName + "." + getter);
            validator.accept(nestedValidator);
            return addValidation((value, result, root) -> {
                if (value != null) {
                    nestedValidator.validate(root, result);
                }
            });
        }

        public Validator<T, R> crossField(BiPredicate<T, R> predicate, String errorMessage) {
            return addValidation((value, result, root) -> {
                if (!predicate.test(value, root)) {
                    result.addError(fieldName + ": " + errorMessage);
                }
            });
        }

        public Validator<T, R> when(BiPredicate<T, R> condition) {
            if (!validations.isEmpty()) {
                ConditionalValidation<T, R> lastValidation = validations.get(validations.size() - 1);
                lastValidation.condition = condition;
            }
            return this;
        }

        public void validate(R root, ValidationResult result) {
            T value = getter.apply(root);
            for (ConditionalValidation<T, R> validation : validations) {
                if (validation.condition.test(value, root)) {
                    validation.validation.accept(value, result, root);
                }
            }
        }

        public Validator<T, R> compose(CompositeValidator<T, R> compositeValidator) {
            compositeValidator.applyTo(this);
            return this;
        }

        public Validator<T, R> composeConditionally(CompositeValidator<T, R> compositeValidator, BiPredicate<T, R> condition) {
            return addValidation((value, result, root) -> {
                if (condition.test(value, root)) {
                    Validator<T, R> conditionalValidator = new Validator<>(t -> value, this.fieldName);
                    compositeValidator.applyTo(conditionalValidator);
                    conditionalValidator.validate(root, result);
                }
            });
        }

        private static class ConditionalValidation<T, R> {
            TriConsumer<T, ValidationResult, R> validation;
            BiPredicate<T, R> condition;

            ConditionalValidation(TriConsumer<T, ValidationResult, R> validation, BiPredicate<T, R> condition) {
                this.validation = validation;
                this.condition = condition;
            }
        }
    }

    public static class ValidationBuilder<T> {
        private T object;
        private List<Validator<?, T>> validators = new ArrayList<>();

        public ValidationBuilder(T object) {
            this.object = object;
        }

        public <U> Validator<U, T> ruleFor(Function<T, U> getter) {
            Validator<U, T> validator = new Validator<>(getter, getter.toString());
            validators.add(validator);
            return validator;
        }

        public ValidationResult validate() {
            ValidationResult result = new ValidationResult();
            for (Validator<?, T> validator : validators) {
                validator.validate(object, result);
            }
            return result;
        }
    }
}