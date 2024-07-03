package com.shub.validation.rules;

import com.shub.validation.core.IValidationError;
import com.shub.validation.core.IValidationResult;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult<T> implements IValidationResult<T> {
    private final T validatedObject;
    private final List<IValidationError> errors = new ArrayList<>();

    public ValidationResult(T validatedObject) {
        this.validatedObject = validatedObject;
    }

    @Override
    public void addError(String fieldPath, String errorMessage) {
        errors.add(new ValidationError(fieldPath, errorMessage));
    }

    @Override
    public boolean isValid() {
        return errors.isEmpty();
    }

    @Override
    public List<IValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    @Override
    public T getValidatedObject() {
        return validatedObject;
    }
}