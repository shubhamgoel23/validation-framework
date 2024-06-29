package com.shub.validation.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ValidationFramework {

    /**
     * Represents the result of a validation operation.
     */
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

    /**
     * The main validator class that provides various validation methods.
     *
     * @param <T> The type of the value being validated
     * @param <R> The type of the root object
     */
    public static class Validator<T, R> {
        private final Function<R, T> getter;
        private final String fieldName;
        private final List<ValidationRule<T, R>> rules = new ArrayList<>();

        public Validator(Function<R, T> getter, String fieldName) {
            this.getter = getter;
            this.fieldName = fieldName;
        }

        /**
         * Validates that the value is not null.
         *
         * @return The validator instance for method chaining
         */
        public Validator<T, R> notNull() {
            return notNull((t, r) -> true);
        }

        /**
         * Validates that the value is not null when the condition is met.
         *
         * @param condition The condition under which this validation should be applied
         * @return The validator instance for method chaining
         */
        public Validator<T, R> notNull(BiPredicate<T, R> condition) {
            return addRule((value, context) -> value != null, "must not be null", condition);
        }

        /**
         * Validates that the value matches the given regex pattern.
         *
         * @param regex The regex pattern to match against
         * @return The validator instance for method chaining
         */
        public Validator<T, R> matches(String regex) {
            return matches(regex, (t, r) -> true);
        }

        /**
         * Validates that the value matches the given regex pattern when the condition is met.
         *
         * @param regex The regex pattern to match against
         * @param condition The condition under which this validation should be applied
         * @return The validator instance for method chaining
         */
        public Validator<T, R> matches(String regex, BiPredicate<T, R> condition) {
            return addRule((value, context) -> value != null && Pattern.matches(regex, value.toString()),
                    "must match pattern: " + regex, condition);
        }

        /**
         * Validates that the value satisfies the given predicate.
         *
         * @param predicate The predicate to test against
         * @param errorMessage The error message to use if validation fails
         * @return The validator instance for method chaining
         */
        public Validator<T, R> satisfies(Predicate<T> predicate, String errorMessage) {
            return satisfies(predicate, errorMessage, (t, r) -> true);
        }

        /**
         * Validates that the value satisfies the given predicate when the condition is met.
         *
         * @param predicate The predicate to test against
         * @param errorMessage The error message to use if validation fails
         * @param condition The condition under which this validation should be applied
         * @return The validator instance for method chaining
         */
        public Validator<T, R> satisfies(Predicate<T> predicate, String errorMessage, BiPredicate<T, R> condition) {
            return addRule((value, context) -> value == null || predicate.test(value), errorMessage, condition);
        }

        /**
         * Validates that the value is not empty (for strings, collections, and arrays).
         *
         * @return The validator instance for method chaining
         */
        public Validator<T, R> notEmpty() {
            return notEmpty((t, r) -> true);
        }

        /**
         * Validates that the value is not empty when the condition is met.
         *
         * @param condition The condition under which this validation should be applied
         * @return The validator instance for method chaining
         */
        public Validator<T, R> notEmpty(BiPredicate<T, R> condition) {
            return addRule((value, context) -> {
                if (value == null) return false;
                if (value instanceof String) return !((String) value).isEmpty();
                if (value instanceof Collection) return !((Collection<?>) value).isEmpty();
                if (value.getClass().isArray()) return ((Object[]) value).length > 0;
                return true;
            }, "must not be empty", condition);
        }

        /**
         * Validates that the numeric value is greater than the specified value.
         *
         * @param min The minimum value (exclusive)
         * @return The validator instance for method chaining
         */
        public Validator<T, R> greaterThan(Comparable<T> min) {
            return greaterThan(min, (t, r) -> true);
        }

        /**
         * Validates that the numeric value is greater than the specified value when the condition is met.
         *
         * @param min The minimum value (exclusive)
         * @param condition The condition under which this validation should be applied
         * @return The validator instance for method chaining
         */
        public Validator<T, R> greaterThan(Comparable<T> min, BiPredicate<T, R> condition) {
            return addRule((value, context) -> value != null && min.compareTo((T) value) < 0,
                    "must be greater than " + min, condition);
        }

        /**
         * Validates a nested object.
         *
         * @param nestedGetter A function to get the nested object
         * @param validator A consumer to configure the nested validator
         * @return The validator instance for method chaining
         */
        public <U> Validator<T, R> nested(Function<T, U> nestedGetter, Consumer<Validator<U, R>> validator) {
            return nested(nestedGetter, validator, (t, r) -> true);
        }

        public <U> Validator<T, R> nested(Function<T, U> nestedGetter, Consumer<Validator<U, R>> validator, BiPredicate<T, R> condition) {
            Validator<U, R> nestedValidator = new Validator<>(root -> {
                T value = this.getter.apply(root);
                return value != null ? nestedGetter.apply(value) : null;
            }, this.fieldName + "." + nestedGetter);
            validator.accept(nestedValidator);
            return addRule((value, context) -> {
                if (value != null) {
                    nestedValidator.validate(context.getRoot(), context.getResult());
                }
                return true; // Always return true to prevent adding a generic error message
            }, null, condition); // Pass null as errorMessage
        }

        /**
         * Validates each element in a collection.
         *
         * @param clazz The class of the elements in the collection
         * @param validator A consumer to configure the validator for each element
         * @return The validator instance for method chaining
         */
        public <U> Validator<T, R> forEach(Class<U> clazz, Consumer<Validator<U, R>> validator) {
            return forEach(clazz, validator, (t, r) -> true);
        }

        public <U> Validator<T, R> forEach(Class<U> clazz, Consumer<Validator<U, R>> validator, BiPredicate<T, R> condition) {
            return addRule((value, context) -> {
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (clazz.isInstance(item)) {
                            Validator<U, R> itemValidator = new Validator<>(root -> clazz.cast(item), fieldName + "[" + i + "]");
                            validator.accept(itemValidator);
                            itemValidator.validate(context.getRoot(), context.getResult());
                        } else {
                            context.getResult().addError(fieldName + "[" + i + "] is not of type " + clazz.getSimpleName());
                        }
                    }
                }
                return true; // Always return true to prevent adding a generic error message
            }, null, condition); // Pass null as errorMessage
        }

        /**
         * Performs a cross-field validation using the root object.
         *
         * @param predicate A predicate that takes the current value and the root object
         * @param errorMessage The error message to use if validation fails
         * @return The validator instance for method chaining
         */
        public Validator<T, R> crossField(BiPredicate<T, R> predicate, String errorMessage) {
            return crossField(predicate, errorMessage, (t, r) -> true);
        }

        /**
         * Performs a conditional cross-field validation using the root object.
         *
         * @param predicate A predicate that takes the current value and the root object
         * @param errorMessage The error message to use if validation fails
         * @param condition The condition under which this validation should be applied
         * @return The validator instance for method chaining
         */
        public Validator<T, R> crossField(BiPredicate<T, R> predicate, String errorMessage, BiPredicate<T, R> condition) {
            return addRule((value, context) -> predicate.test(value, context.getRoot()), errorMessage, condition);
        }

        /**
         * Applies a composite validator.
         *
         * @param compositeValidator The composite validator to apply
         * @return The validator instance for method chaining
         */
        public Validator<T, R> compose(CompositeValidator<T, R> compositeValidator) {
            compositeValidator.applyTo(this);
            return this;
        }

        /**
         * Applies a composite validator conditionally.
         *
         * @param compositeValidator The composite validator to apply
//         * @param condition The condition under which to apply the composite validator
         * @return The validator instance for method chaining
         */
        public Validator<T, R> composeConditionally(CompositeValidator<T, R> compositeValidator) {
            return composeConditionally(compositeValidator, compositeValidator.getDefaultCondition());
        }

        public Validator<T, R> composeConditionally(CompositeValidator<T, R> compositeValidator, BiPredicate<T, R> overrideCondition) {
            return addRule((value, context) -> {
                if (overrideCondition.test(value, context.getRoot())) {
                    Validator<T, R> conditionalValidator = new Validator<>(t -> value, this.fieldName);
                    compositeValidator.applyTo(conditionalValidator);
                    conditionalValidator.validate(context.getRoot(), context.getResult());
                }
                return true; // Always return true to prevent adding a generic error message
            }, null, (t, r) -> true); // Pass null as errorMessage
        }

        private Validator<T, R> addRule(BiPredicate<T, ValidationContext<R>> rule, String errorMessage, BiPredicate<T, R> condition) {
            rules.add(new ValidationRule<>(rule, errorMessage, condition));
            return this;
        }

        public void validate(R root, ValidationResult result) {
            T value = getter.apply(root);
            ValidationContext<R> context = new ValidationContext<>(root, result);
            for (ValidationRule<T, R> rule : rules) {
                if (rule.getCondition().test(value, root) && !rule.getRule().test(value, context)) {
                    if (rule.getErrorMessage() != null) { // Only add error if errorMessage is not null
                        result.addError(fieldName + " " + rule.getErrorMessage());
                    }
                }
            }
        }
    }

    private static class ValidationRule<T, R> {
        private final BiPredicate<T, ValidationContext<R>> rule;
        private final String errorMessage;
        private final BiPredicate<T, R> condition;

        ValidationRule(BiPredicate<T, ValidationContext<R>> rule, String errorMessage, BiPredicate<T, R> condition) {
            this.rule = rule;
            this.errorMessage = errorMessage;
            this.condition = condition;
        }

        public BiPredicate<T, ValidationContext<R>> getRule() {
            return rule;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public BiPredicate<T, R> getCondition() {
            return condition;
        }
    }

    /**
     * A composite validator that groups multiple validation rules.
     *
     * @param <T> The type of the value being validated
     * @param <R> The type of the root object
     */
    public static class CompositeValidator<T, R> {
        private final List<Consumer<Validator<T, R>>> rules = new ArrayList<>();
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

    /**
     * Provides context for validation operations.
     *
     * @param <R> The type of the root object
     */
    public static class ValidationContext<R> {
        private final R root;
        private final ValidationResult result;

        public ValidationContext(R root, ValidationResult result) {
            this.root = root;
            this.result = result;
        }

        public R getRoot() {
            return root;
        }

        public ValidationResult getResult() {
            return result;
        }
    }

    /**
     * A builder for creating and executing validations on an object.
     *
     * @param <T> The type of the object being validated
     */
    public static class ValidationBuilder<T> {
        private final T object;
        private final List<Validator<?, T>> validators = new ArrayList<>();

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