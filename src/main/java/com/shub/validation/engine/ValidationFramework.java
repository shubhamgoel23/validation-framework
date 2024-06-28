package com.shub.validation.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
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

    public static class Validator<T, R> {
        private Function<R, T> getter;
        private String fieldName;
        private List<ConditionalValidation<T, R>> validations = new ArrayList<>();

        public Validator(Function<R, T> getter, String fieldName) {
            this.getter = getter;
            this.fieldName = fieldName;
        }

        private static class ConditionalValidation<T, R> {
            BiConsumer<T, ValidationResult> validation;
            Predicate<R> condition;

            ConditionalValidation(BiConsumer<T, ValidationResult> validation, Predicate<R> condition) {
                this.validation = validation;
                this.condition = condition;
            }
        }

        public Validator<T, R> notNull() {
            return addValidation((value, result) -> {
                if (value == null) {
                    result.addError(fieldName + " cannot be null");
                }
            });
        }

        public Validator<T, R> matches(String regex) {
            return addValidation((value, result) -> {
                if (value != null && !Pattern.matches(regex, value.toString())) {
                    result.addError(fieldName + " does not match pattern " + regex);
                }
            });
        }

        public Validator<T, R> satisfies(Predicate<T> predicate, String errorMessage) {
            return addValidation((value, result) -> {
                if (value != null && !predicate.test(value)) {
                    result.addError(fieldName + ": " + errorMessage);
                }
            });
        }

        public <U> Validator<T, R> forEach(Class<U> clazz, Consumer<Validator<U, R>> validator) {
            return addValidation((value, result) -> {
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (clazz.isInstance(item)) {
                            Validator<U, R> itemValidator = new Validator<>(root -> clazz.cast(item), fieldName + "[" + i + "]");
                            validator.accept(itemValidator);
                            itemValidator.validate(null, result);
                        } else {
                            result.addError(fieldName + "[" + i + "] is not of type " + clazz.getSimpleName());
                        }
                    }
                }
            });
        }

        public <U> Validator<T, R> nested(Function<T, U> getter, Consumer<Validator<U, R>> validator) {
            return addValidation((value, result) -> {
                if (value != null) {
                    U nestedValue = getter.apply(value);
                    Validator<U, R> nestedValidator = new Validator<>(root -> nestedValue, fieldName + "." + getter);
                    validator.accept(nestedValidator);
                    nestedValidator.validate(null, result);
                }
            });
        }

        private Validator<T, R> addValidation(BiConsumer<T, ValidationResult> validation) {
            validations.add(new ConditionalValidation<>(validation, r -> true));
            return this;
        }

        public Validator<T, R> when(Predicate<R> condition) {
            if (!validations.isEmpty()) {
                ConditionalValidation<T, R> lastValidation = validations.get(validations.size() - 1);
                lastValidation.condition = condition;
            }
            return this;
        }

        public void validate(R root, ValidationResult result) {
            T value = getter.apply(root);
            for (ConditionalValidation<T, R> validation : validations) {
                if (validation.condition.test(root)) {
                    validation.validation.accept(value, result);
                }
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