# Fluent Validation Framework

## Table of Contents
1. [Introduction](#introduction)
2. [Features](#features)
3. [Benefits](#benefits)
4. [Installation](#installation)
5. [Usage](#usage)
6. [Use Cases and Examples](#use-cases-and-examples)
    - [Basic Validations](#basic-validations)
    - [Nested Object Validation](#nested-object-validation)
    - [Collection Validation](#collection-validation)
    - [Conditional Validation](#conditional-validation)
    - [Cross-Field Validation](#cross-field-validation)
    - [Custom Validation Rules](#custom-validation-rules)
    - [Composite Validators](#composite-validators)
7. [Error Handling](#error-handling)
8. [Contributing](#contributing)
9. [License](#license)

## Introduction

The Fluent Validation Framework is a powerful, flexible, and easy-to-use validation library for Java. It provides a fluent interface to define complex validation rules for your objects, supporting nested validations, collection validations, conditional validations, and more.

## Features

- Fluent and intuitive API
- Support for basic validations (not null, regex matching, custom predicates)
- Nested object validation
- Collection validation
- Conditional validation
- Cross-field validation
- Composite validators for reusable validation rules
- Detailed error messages with exact field paths
- Extensible architecture for custom validation rules

## Benefits

- Improves code readability and maintainability
- Separates validation logic from business logic
- Reduces boilerplate code
- Provides flexibility in defining complex validation rules
- Enables easy unit testing of validation rules
- Supports localization of error messages

## Installation

[Provide installation instructions here, e.g., Maven/Gradle dependencies]

## Usage

Here's a basic example of how to use the Fluent Validation Framework:

```java
ValidationFramework.ValidationBuilder<User> builder = new ValidationFramework.ValidationBuilder<>(user);

builder.ruleFor("name", User::getName)
    .notNull()
    .satisfies(name -> name.length() >= 2, "Name must be at least 2 characters long");

builder.ruleFor("email", User::getEmail)
    .notNull()
    .matches("^[^@]+@[^@]+\\.[^@]+$", "Must be a valid email address");

ValidationFramework.ValidationResult result = builder.validate();

if (!result.isValid()) {
    for (ValidationFramework.ValidationResult.ValidationError error : result.getErrors()) {
        System.out.println(error.getFieldPath() + ": " + error.getErrorMessage());
    }
}
```
### Basic Validations
```java
builder.ruleFor("age", User::getAge)
    .notNull()
    .satisfies(age -> age >= 18, "Must be at least 18 years old");

builder.ruleFor("phoneNumber", User::getPhoneNumber)
    .matches("\\d{10}", "Must be a 10-digit number");
```

### Nested Object Validation
```java
builder.ruleFor("address", User::getAddress)
    .nested("street", Address::getStreet, street -> street
        .notNull()
        .satisfies(s -> s.length() >= 5, "Street must be at least 5 characters long",
        (street, user) -> user.needsPhysicalAddress()))
        .nested("city", Address::getCity, city -> city
        .notNull()
        .satisfies(c -> c.length() >= 2, "City must be at least 2 characters long",
        (city, user) -> user.needsPhysicalAddress()));
```

### Collection Validation
```java
builder.ruleFor("phoneNumbers", User::getPhoneNumbers)
    .notNull()
    .forEach("", String.class, phone -> phone
        .matches("\\d{10}", "Must be a 10-digit number"));
```
### Conditional Validation

In this framework, conditions are integrated directly into the validation methods, eliminating the need for a separate `when` clause. This approach makes the code more concise and less prone to errors.

```java
builder.ruleFor("drivingLicense", User::getDrivingLicense)
    .notNull((license, user) -> user.getAge() >= 18, "Driving license is required for users 18 and older");

        builder.ruleFor("phoneNumber", User::getPhoneNumber)
    .matches("\\d{10}", "Must be a 10-digit number", (phone, user) -> user.wantsToReceiveSMS());

        builder.ruleFor("age", User::getAge)
    .satisfies(age -> age >= 21, "Must be at least 21 years old",
        (age, user) -> user.getCountry().equals("USA") && user.wantsToOrderAlcohol());
```
### Cross-Field Validation
```java
builder.ruleFor("confirmPassword", User::getConfirmPassword)
    .satisfies((confirmPassword, user) -> confirmPassword.equals(user.getPassword()),
        "Passwords must match",
        (confirmPassword, user) -> user.isChangingPassword());
```
### Custom Validation Rules
```java
builder.ruleFor("confirmPassword", User::getConfirmPassword)
    .crossField((confirmPassword, user) -> confirmPassword.equals(user.getPassword()),
                "Passwords must match");
```

### Composite Validators
```java
CompositeValidator<Address, User> addressValidator = new CompositeValidator<Address, User>()
        .add(v -> v.notNull())
        .add(v -> v.nested("street", Address::getStreet, street -> street
                .notNull()
                .satisfies(s -> s.length() >= 5, "Street must be at least 5 characters long")))
        .add(v -> v.nested("city", Address::getCity, city -> city
                .notNull()
                .satisfies(c -> c.length() >= 2, "City must be at least 2 characters long")));

builder.ruleFor("address", User::getAddress)
    .compose(addressValidator);
```

## Error Handling
The framework provides detailed error messages with exact field paths. Here's an example of how to handle and display validation errors:
```java
ValidationFramework.ValidationResult result = builder.validate();

if (!result.isValid()) {
    for (ValidationFramework.ValidationResult.ValidationError error : result.getErrors()) {
        System.out.println(error.getFieldPath() + ": " + error.getErrorMessage());
    }
}
```

This will produce output like:
```text
name: Name must be at least 2 characters long
email: Must be a valid email address
address.street: Street must be at least 5 characters long
phoneNumbers[0]: Must be a 10-digit number
```