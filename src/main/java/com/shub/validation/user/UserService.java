package com.shub.validation.user;

import com.shub.validation.engine.ValidationFramework;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {


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
                .when((a, u) -> u.getAge() <= 20);
    }

    public User save(User user) {
        ValidationFramework.ValidationBuilder<User> builder = new ValidationFramework.ValidationBuilder<>(user);

        builder.ruleFor(User::getName)
                .notNull()
                .satisfies(name -> name.length() >= 2 && name.length() <= 50, "Name must be between 2 and 50 characters");

        builder.ruleFor(User::getEmail)
                .notNull()
                .matches("^[^@]+@[^@]+\\.[^@]+$");

        builder.ruleFor(User::getAge)
                .notNull()
                .satisfies(age -> age >= 18 && age <= 120, "Age must be between 18 and 120");

        builder.ruleFor(User::getAddresses)
                .notNull()
                .forEach(Address.class, UserService::validateAddress)
                .when((a, u) -> u.getAge() <= 20);
        ;

        builder.ruleFor(User::getMainAddress)
                .notNull()
                .nested(address -> address, UserService::validateAddress);

        ValidationFramework.ValidationResult result = builder.validate();
        return user;
    }
}
