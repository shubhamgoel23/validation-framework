package com.shub.validation.engine;

import java.util.ArrayList;
import java.util.List;
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

    public static class Rule<T> {
        private Function<T, Boolean> validation;
        private String errorMessage;

        public Rule(Function<T, Boolean> validation, String errorMessage) {
            this.validation = validation;
            this.errorMessage = errorMessage;
        }

        public boolean apply(T value) {
            return validation.apply(value);
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class Validator<T> {
        private Function<Object, T> getter;
        private String fieldName;
        private List<Rule<T>> rules = new ArrayList<>();
        private List<BiConsumer<Object, ValidationResult>> nestedValidations = new ArrayList<>();

        public Validator(Function<Object, T> getter, String fieldName) {
            this.getter = getter;
            this.fieldName = fieldName;
        }

        public Validator<T> notNull() {
            rules.add(new Rule<>(value -> value != null, fieldName + " cannot be null"));
            return this;
        }

        public Validator<T> matches(String regex) {
            rules.add(new Rule<>(value -> value != null && Pattern.matches(regex, value.toString()),
                    fieldName + " does not match pattern " + regex));
            return this;
        }

        public Validator<T> satisfies(Predicate<T> predicate, String errorMessage) {
            rules.add(new Rule<>(value -> value == null || predicate.test(value), fieldName + ": " + errorMessage));
            return this;
        }

        public <U> Validator<T> forEach(Class<U> clazz, Consumer<Validator<U>> validator) {
            nestedValidations.add((obj, result) -> {
                T value = getter.apply(obj);
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (clazz.isInstance(item)) {
                            Validator<U> itemValidator = new Validator<>(o -> clazz.cast(item), fieldName + "[" + i + "]");
                            validator.accept(itemValidator);
                            itemValidator.validate(obj, result);
                        } else {
                            result.addError(fieldName + "[" + i + "] is not of type " + clazz.getSimpleName());
                        }
                    }
                }
            });
            return this;
        }

        public <U> Validator<T> nested(Function<T, U> getter, Consumer<Validator<U>> validator) {
            nestedValidations.add((obj, result) -> {
                T value = this.getter.apply(obj);
                if (value != null) {
                    U nestedValue = getter.apply(value);
                    Validator<U> nestedValidator = new Validator<>(o -> nestedValue, fieldName + "." + getter);
                    validator.accept(nestedValidator);
                    nestedValidator.validate(obj, result);
                }
            });
            return this;
        }

        public void validate(Object object, ValidationResult result) {
            T value = getter.apply(object);
            for (Rule<T> rule : rules) {
                if (!rule.apply(value)) {
                    result.addError(rule.getErrorMessage());
                }
            }
            for (BiConsumer<Object, ValidationResult> nestedValidation : nestedValidations) {
                nestedValidation.accept(object, result);
            }
        }
        private void addError(String error) {
            rules.add(new Rule<>(value -> false, error));
        }
    }

    public static class ValidationBuilder<T> {
        private T object;
        private List<Validator<?>> validators = new ArrayList<>();

        public ValidationBuilder(T object) {
            this.object = object;
        }

        public <U> Validator<U> ruleFor(Function<T, U> getter) {
            Validator<U> validator = new Validator<>(obj -> getter.apply((T) obj), getter.toString());
            validators.add(validator);
            return validator;
        }

        public ValidationResult validate() {
            ValidationResult result = new ValidationResult();
            for (Validator<?> validator : validators) {
                validator.validate(object, result);
            }
            return result;
        }
    }
}