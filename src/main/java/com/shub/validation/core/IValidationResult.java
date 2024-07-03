package com.shub.validation.core;

import java.util.List;

public interface IValidationResult<T> {
    void addError(String fieldPath, String errorMessage);
    boolean isValid();
    List<IValidationError> getErrors();
    T getValidatedObject(); // New method
}