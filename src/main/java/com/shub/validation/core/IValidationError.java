package com.shub.validation.core;

public interface IValidationError {
    String getFieldPath();
    String getErrorMessage();
}
