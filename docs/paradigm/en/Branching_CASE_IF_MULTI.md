---
slug: "/Branching_CASE_IF_MULTI"
title: 'Branching (CASE, IF, MULTI)'
---

*The branching operator* creates an [action](Actions.md) that determines for a set of *conditions* which condition is met, and calls the corresponding action.

All conditions are defined as [properties](Properties.md) and/or parameters. Accordingly, a condition is *met* if the value of the property or parameter by which it is set is not equal to `NULL`.

Conditions are checked in the order written; the first met condition selects the action to be called, and the remaining conditions are not checked. You can also specify an *alternative action* that is called only if none of the conditions is met.

[Interruption](Interruption_BREAK.md), [next iteration](Next_iteration_CONTINUE.md) and [exit](Exit_RETURN.md) signals raised by the called action are passed on to the surrounding action — the branching operator itself does not consume them.

### Polymorphic form {#poly}

This operator also allows defining a condition implicitly — the condition is that the arguments belong to the parameter classes of the action corresponding to that condition. We will call this the *polymorphic* form of the operator. The polymorphic form is the natural way to dispatch on the class of an argument.

### Mutual exclusion of conditions {#exclusive}

The branching operator lets you specify that all its conditions are *mutually exclusive*. If this option is set, and the conditions are not in fact mutually exclusive, the platform will throw the corresponding error.

The general form is non-exclusive by default and may be marked exclusive explicitly. The polymorphic form is mutually exclusive by default — implementations for disjoint argument classes do not overlap. The single form has exactly one condition and does not use mutual exclusion.

### Implicit definition

The general and polymorphic forms support [implicit definition](Action_extension.md) through the technique of [extensions](Extensions.md). The single form has one fixed condition and does not support implicit definition.

### Single form {#single}

The *single* form of the branching operator checks exactly one condition. If this condition is met, the specified action is called. It is also possible to specify an *alternative action* that is called if the condition is not met.

### Language

To declare an action implementing general form of branching, the [`CASE` operator](../language/CASE_action_operator.md) is used. For the single form of branching, the [`IF` operator](../language/IF_..._THEN_action_operator.md) is used, and for the polymorphic form the [`MULTI` operator](../language/MULTI_action_operator.md). 

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
