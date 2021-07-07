---
title: 'Branching (CASE, IF, MULTI)'
---

*The branching operator* creates an [action](Actions.md) that determines for a set of *conditions* which condition is met, and calls the corresponding action.

All conditions are defined as [properties](Properties.md) and/or parameters. Accordingly, a condition is *met* if the value of the property or parameter by which it is set is not equal to `NULL`.

### Polymorphic form {#poly}

This operator also allows to define a condition not explicitly but by using as a condition the [signature](Property_signature_CLASS.md) of the action corresponding to that condition. We will call this the *polymorphic* form of the operator.

### Mutual exclusion of conditions {#exclusive}

The branching operator lets you specify that all its conditions are *mutually exclusive*. If this option is set, and the conditions are not in fact mutually exclusive, the platform will throw the corresponding error.

It is worth noting that this check is no more than a hint to the platform (for better optimization), and also a kind of self-checking on the part of the developer. However, in many cases it allows you to make the code more transparent and readable (especially with the polymorphic form of the selection operator).

### Implicit definition

This operator has the capability of an [implicit definition](Action_extension.md) using the technique of [extensions](Extensions.md), which allows, in particular, to implement polymorphism in the form that is common practice in OOP.

### Single form {#single}

The *single* form of the branching operator checks exactly one condition. If this condition is met, the specified action is called. It is also possible to specify an *alternative action* that is called if the condition is not met.


:::info
Type of mutual exclusion and implicit definition do not make sense/are not supported for this form of the operator
:::

### Language

To declare an action implementing general form of branching, the [`CASE` operator](CASE_action_operator.md) is used. For the single form of branching, the [`IF` operator](IF_..._THEN_action_operator.md) is used, and for the polymorphic form the [`MULTI` operator](MULTI_action_operator.md). 

### Examples

```lsf
test = DATA INTEGER (INTEGER);
caseActionTest(a)  {
    CASE
        WHEN test(a) > 7 THEN MESSAGE '>7';
        WHEN test(a) > 6 THEN MESSAGE '>6';
        WHEN test(a) > 5 THEN MESSAGE '>5';
}
```


```lsf
// Action that compares the value of the count property to 3 and displays a message to the user
moreThan3(obj)  {
    IF count(obj) > 3 THEN
        MESSAGE '>3';
    ELSE
        MESSAGE '<=3';
}

checkNullName (Store st) {
    IF NOT name(st) THEN
        MESSAGE 'Name is null';
}
```


```lsf
CLASS Shape;

CLASS Square : Shape;
CLASS Circle : Shape;

message (Square s)  { MESSAGE 'Square'; }
message (Circle c)  { MESSAGE 'Circle'; }

message (Shape s) = MULTI message[Square](s), message[Circle](s);
```

  

