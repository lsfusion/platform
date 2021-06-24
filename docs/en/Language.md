---
title: 'Language'
sidebar_label: Overview
---

*lsFusion* is a programming language that describes lsFusion [modules](Modules.md).

Each module consists of a sequence of [statements](Statements.md) divided into module [*header*](Module_header.md) and *body*. The header may include four special statements: `MODULE`, `REQUIRE`, `PRIORITY` and `NAMESPACE`, which define module parameters. The header is followed by the module body which consists of the remaining statements and describes the logic of this module.

### Example

```lsf
// Module header
MODULE LanguageExample;

REQUIRE System;

NAMESPACE Example;

// Module body

// Classes declaration
CLASS Employee;
CLASS Company;

// Properties declaration
name(employee) = DATA BPSTRING[100](Employee);
age(employee) = DATA INTEGER(Employee);
company(employee) = DATA Company(Employee);

name(company) = DATA BPSTRING[100](Company);

// Form declaration
FORM employeeForm
    OBJECTS e = Employee
    PROPERTIES(e) name, age, company
;

// Adding a form to the navigator
NAVIGATOR {
    NEW employeeForm;
}
```

