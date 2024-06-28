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

    public static class Validator<T> {
        private Function<Object, T> getter;
        private String fieldName;
        private List<BiConsumer<T, ValidationResult>> validations = new ArrayList<>();

        public Validator(Function<Object, T> getter, String fieldName) {
            this.getter = getter;
            this.fieldName = fieldName;
        }

        public Validator<T> notNull() {
            validations.add((value, result) -> {
                if (value == null) {
                    result.addError(fieldName + " cannot be null");
                }
            });
            return this;
        }

        public Validator<T> matches(String regex) {
            validations.add((value, result) -> {
                if (value != null && !Pattern.matches(regex, value.toString())) {
                    result.addError(fieldName + " does not match pattern " + regex);
                }
            });
            return this;
        }

        public Validator<T> satisfies(Predicate<T> predicate, String errorMessage) {
            validations.add((value, result) -> {
                if (value != null && !predicate.test(value)) {
                    result.addError(fieldName + ": " + errorMessage);
                }
            });
            return this;
        }

        public <U> Validator<T> forEach(Class<U> clazz, Consumer<Validator<U>> validator) {
            validations.add((value, result) -> {
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (clazz.isInstance(item)) {
                            Validator<U> itemValidator = new Validator<>(obj -> clazz.cast(item), fieldName + "[" + i + "]");
                            validator.accept(itemValidator);
                            itemValidator.validate(null, result);
                        } else {
                            result.addError(fieldName + "[" + i + "] is not of type " + clazz.getSimpleName());
                        }
                    }
                }
            });
            return this;
        }

        public <U> Validator<T> nested(Function<T, U> getter, Consumer<Validator<U>> validator) {
            validations.add((value, result) -> {
                if (value != null) {
                    U nestedValue = getter.apply(value);
                    Validator<U> nestedValidator = new Validator<>(obj -> nestedValue, fieldName + "." + getter);
                    validator.accept(nestedValidator);
                    nestedValidator.validate(null, result);
                }
            });
            return this;
        }

        public void validate(Object object, ValidationResult result) {
            T value = getter.apply(object);
            for (BiConsumer<T, ValidationResult> validation : validations) {
                validation.accept(value, result);
            }
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