package com.shub.validation.group;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GroupValidator<T> {
    private final Set<Class<?>> activeGroups;

    public GroupValidator(Class<?>... groups) {
        this.activeGroups = new HashSet<>(Arrays.asList(groups));
    }

    public boolean isGroupActive(Class<?> group) {
        return activeGroups.contains(group);
    }
}
