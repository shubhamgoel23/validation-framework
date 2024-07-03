package com.shub.validation.core;

import java.util.Locale;

public interface IMessageSource {
    String getMessage(String key, Object[] args, Locale locale);
}