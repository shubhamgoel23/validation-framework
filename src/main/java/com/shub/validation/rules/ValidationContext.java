package com.shub.validation.rules;

import com.shub.validation.core.IValidationContext;
import com.shub.validation.core.IValidationResult;

public class ValidationContext<R> implements IValidationContext<R> {
    private final R root;
    private final IValidationResult result;

    public ValidationContext(R root, IValidationResult result) {
        this.root = root;
        this.result = result;
    }

    @Override
    public R getRoot() {
        return root;
    }

    @Override
    public IValidationResult getResult() {
        return result;
    }
}