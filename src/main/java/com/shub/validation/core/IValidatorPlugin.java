package com.shub.validation.core;

public interface IValidatorPlugin<U, T> {
    void apply(IValidator<U, T> validator, IValidationResult<T> result);
}