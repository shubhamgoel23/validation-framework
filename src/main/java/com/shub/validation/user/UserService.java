package com.shub.validation.user;

import com.shub.validation.engine.ValidationFramework;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class UserService {


    ValidationFramework.CompositeValidator<String, User> nameValidator = new ValidationFramework.CompositeValidator<String, User>()
            .add(ValidationFramework.Validator::notNull)
            .add(v -> v.satisfies(s -> s.length() >= 2, "Name must be at least 2 characters long"))
            .add(v -> v.satisfies(s -> s.length() <= 50, "Name must not exceed 50 characters"));

    ValidationFramework.CompositeValidator<Address, User> primaryAddressValidator = new ValidationFramework.CompositeValidator<Address, User>()
            .add(ValidationFramework.Validator::notNull)
            .add(v -> v.nested(Address::getStreet, street -> street.notNull().satisfies(s -> s.length() >= 5, "Street must be at least 5 characters")))
            .add(v -> v.nested(Address::getCity, city -> city.notNull().satisfies(c -> c.length() >= 2, "City must be at least 2 characters")))
            .withDefaultCondition((address, _) -> address.getType() == AddressType.PRIMARY);

    ValidationFramework.CompositeValidator<Address, User> secondaryAddressValidator = new ValidationFramework.CompositeValidator<Address, User>()
            .add(ValidationFramework.Validator::notNull)
            .add(v -> v.nested(Address::getStreet, street -> street.notNull().satisfies(s -> s.length() >= 7, "Street must be at least 7 characters")))
            .add(v -> v.nested(Address::getCity, city -> city.notNull().satisfies(c -> c.length() >= 7, "City must be at least 7 characters")))
            .withDefaultCondition((address, _) -> address.getType() == AddressType.SECONDARY);

    private static void validateAddress(ValidationFramework.Validator<Address, User> addressValidator) {
        addressValidator
                .notNull()
                .nested(Address::getStreet, street -> street
                        .notNull()
                        .satisfies(s -> s.length() >= 5, "Street must be at least 5 characters"))
                .when((a, u) -> u.getAge() > 20)
                .nested(Address::getCity, city -> city
                        .notNull()
                        .satisfies(c -> c.length() >= 2, "City must be at least 2 characters"))
                .when((a, u) -> (u.getAge() <= 20 && !ObjectUtils.isEmpty(a.getCity())));
    }

    public User save(User user) {
        ValidationFramework.ValidationBuilder<User> builder = new ValidationFramework.ValidationBuilder<>(user);

        builder.ruleFor(User::getName)
                .notNull()
                .satisfies(name -> name.length() >= 2 && name.length() <= 50, "Name must be between 2 and 50 characters")
                .when((n, u) -> true)
                .satisfies(name -> name.length() >= 2 && name.length() <= 50, "random test")
                .when((n, u) -> true);

        builder.ruleFor(User::getEmail)
                .notNull()
                .matches("^[^@]+@[^@]+\\.[^@]+$");

        builder.ruleFor(User::getAge)
                .notNull()
                .satisfies(age -> age >= 18 && age <= 120, "Age must be between 18 and 120");

//        builder.ruleFor(User::getAddresses)
//                .notNull()
//                .forEach(Address.class, av->validateAddress(av))
//                .when((a, u) -> u.getAge() <= 20);

//        builder.ruleFor(User::getMainAddress)
//                .notNull()
//                .nested(address -> address, UserService::validateAddress)
//                .crossField((m, u) -> !ObjectUtils.isEmpty(m.getCity()) && !ObjectUtils.isEmpty(u.getAge()), "custom error");
//
//        builder.ruleFor(User::getName).compose(nameValidator);
//        builder.ruleFor(User::getMainAddress).compose(primaryAddressValidator);

        builder.ruleFor(User::getAddresses)
                .forEach(Address.class, av ->
                        av

                                .composeConditionally(primaryAddressValidator, primaryAddressValidator.getDefaultCondition())

                                .composeConditionally(secondaryAddressValidator, secondaryAddressValidator.getDefaultCondition())
                );

        ValidationFramework.ValidationResult result = builder.validate();
        return user;
    }
}
