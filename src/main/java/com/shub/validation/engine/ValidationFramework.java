package com.shub.validation.engine;

import java.util.ArrayList;
import java.util.List;
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
        private T value;
        private String fieldName;
        private ValidationResult result;

        public Validator(T value, String fieldName, ValidationResult result) {
            this.value = value;
            this.fieldName = fieldName;
            this.result = result;
        }

        public Validator<T> notNull() {
            if (value == null) {
                result.addError(fieldName + " cannot be null");
            }
            return this;
        }

        public Validator<T> matches(String regex) {
            if (value != null && !Pattern.matches(regex, value.toString())) {
                result.addError(fieldName + " does not match pattern " + regex);
            }
            return this;
        }

        public Validator<T> satisfies(Predicate<T> predicate, String errorMessage) {
            if (value != null && !predicate.test(value)) {
                result.addError(fieldName + ": " + errorMessage);
            }
            return this;
        }

        public <U> Validator<T> forEach(Class<U> clazz, Consumer<Validator<U>> validator) {
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (clazz.isInstance(item)) {
                        validator.accept(new Validator<>((U) item, fieldName + "[" + i + "]", result));
                    } else {
                        result.addError(fieldName + "[" + i + "] is not of type " + clazz.getSimpleName());
                    }
                }
            }
            return this;
        }

        public <U> Validator<T> nested(Function<T, U> getter, Consumer<Validator<U>> validator) {
            try {
                U nestedValue = getter.apply(value);
                validator.accept(new Validator<>(nestedValue, fieldName + "." + getter.toString(), result));
            } catch (Exception e) {
                result.addError("Error accessing " + fieldName + "." + getter);
            }
            return this;
        }
    }

    public static class ValidationBuilder<T> {
        private T object;
        private ValidationResult result = new ValidationResult();

        public ValidationBuilder(T object) {
            this.object = object;
        }

        public <U> Validator<U> ruleFor(Function<T, U> getter) {
            try {
                U value = getter.apply(object);
                return new Validator<>(value, getter.toString(), result);
            } catch (Exception e) {
                result.addError("Error accessing " + getter);
                return new Validator<>(null, getter.toString(), result);
            }
        }

        public ValidationResult validate() {
            return result;
        }
    }
}