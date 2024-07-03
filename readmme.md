1. When:
    - Purpose: To conditionally apply a set of validation rules.
    - Scope: Affects all subsequent rules until another 'when' or 'otherwise' is encountered.
    - Example:
      validator.ruleFor("email", User::getEmail)
      .when(user -> user.isRegistered())
      .notEmpty()
      .matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

2. Conditions:
    - Purpose: To conditionally apply a single validation rule.
    - Scope: Affects only the rule it's directly attached to.
    - Example:
      validator.ruleFor("age", User::getAge)
      .satisfies(age -> age >= 18, "Must be an adult", (age, user) -> user.getCountry().equals("US"));

3. Groups (if implemented):
    - Purpose: To categorize validation rules and selectively apply them based on context.
    - Scope: Affects the rule it's attached to, but multiple rules can be part of the same group.
    - Example (hypothetical implementation):
      validator.ruleFor("taxId", User::getTaxId)
      .notEmpty().group(EmploymentValidation.class)
      .matches("^\\d{9}$").group(TaxValidation.class);

Sequence and Interaction:

1. Groups (outermost)
2. When clauses (middle layer)
3. Conditions (innermost, rule-specific)

validator.ruleFor("salary", Employee::getSalary)
.group(FinancialValidation.class)  // Group (applied first)
.when(employee -> employee.isFullTime())  // When (applied second)
.greaterThan(0).satisfies(salary -> salary >= minimumWage, "Must be at least minimum wage",
(salary, employee) -> employee.getCountry().equals("US"))  // Condition (applied last)

Execution Flow:
1. Check if FinancialValidation group is active
2. If yes, check if employee is full-time
3. If yes, apply the greaterThan(0) rule
4. Then, only for US employees, check if salary is at least minimum wage