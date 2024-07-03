package com.shub.validation.core;

public interface IValidationContext<R> {
    R getRoot();
    IValidationResult<R> getResult();
}