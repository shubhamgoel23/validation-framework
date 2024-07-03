package com.shub.validation.core;

public interface ICrossFieldValidator<T> {
    void validate(T object, IValidationResult<T> result);
}