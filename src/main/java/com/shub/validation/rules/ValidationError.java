package com.shub.validation.rules;

import com.shub.validation.core.IValidationError;

public class ValidationError implements IValidationError {
    private final String fieldPath;
    private final String errorMessage;

    public ValidationError(String fieldPath, String errorMessage) {
        this.fieldPath = fieldPath;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getFieldPath() {
        return fieldPath;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return fieldPath + ": " + errorMessage;
    }
}
