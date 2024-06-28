package com.shub.validation.user;

import lombok.Data;

import java.util.List;

@Data
public class User {
    private String name;
    private String email;
    private Integer age;
    private List<Address> addresses;

}
