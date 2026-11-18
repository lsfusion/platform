---
slug: "/Language"
title: 'Language'
---

*lsFusion* is a programming language that describes lsFusion [modules](../paradigm/Modules.md).

Each module consists of a sequence of [statements](Statements.md) divided into module [*header*](Module_header.md) and *body*. The header may include four special statements: `MODULE`, `REQUIRE`, `PRIORITY` and `NAMESPACE`, which define module parameters. The header is followed by the module body which consists of the remaining statements and describes the logic of this module.

Besides the statements, this section describes the interfaces that client code written in JavaScript works with: the [React views](React_views.md) — what a React component that draws a container, a component or a window of the client receives from the form and calls — the [render functions](Render_functions.md) and [edit functions](Edit_functions.md) of the classic custom views, the [form and navigator controllers](Form_and_navigator_controllers.md) that any view written in JavaScript is given, and the [client JavaScript modules](Client_JavaScript_modules.md) that hold such code.

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

