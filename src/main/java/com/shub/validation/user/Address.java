package com.shub.validation.user;

import lombok.Data;

@Data
public class Address {
    private String street;
    private String city;
    private AddressType type;
}
