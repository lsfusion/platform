---
slug: "/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE"
title: 'Selection (CASE, IF, MULTI, OVERRIDE, EXCLUSIVE)'
---

The *selection* operator creates a property that determines for a set of *conditions* which condition is met, and returns the value of the *result* corresponding to that condition. If several conditions are met at once, the result corresponds to the first of them in the listed order. If none of the conditions is met, then the value of the created property will be `NULL`. 

All conditions and results are defined as some properties and/or parameters. Accordingly, a condition *is met* if the value of the property or parameter by which this condition is set is not equal to `NULL`. 

### Polymorphic form {#poly}

The platform also allows you to define a condition and the corresponding result with one property. In this case, the condition may be either matching the property's signature, or the property itself. We will call this the *polymorphic* form of the operator.


:::info
Note that the [extremum operator](Extremum_MAX_MIN.md) and [logical operators](Logical_operators_AND_OR_NOT_XOR.md) basically are also varieties of the selection operator (and of its polymorphic form, i.e. the conditions and result are defined by one property)
:::

### Mutual exclusion of conditions {#exclusive}

The selection operator lets you specify that all its conditions are *mutually exclusive* — at most one of them is met for any given arguments, so the result no longer depends on their order.

### Implicit definition

This operator supports [implicit definition](Property_extension.md), where its conditions and results are added piecemeal across modules. When such an implicitly defined operator is also declared mutually exclusive, its conditions across all modules must not overlap; the platform verifies this at module finalization. For an operator defined inline, the same requirement is taken on trust.

### Single form {#single}

The *single* form of the selection operator checks exactly one condition. If this condition is met, the value of the specified result is returned. It is also possible to specify an *alternative result* whose value is returned if the condition is not met.

The mutual-exclusion option and implicit definition do not apply to the single form.

### Result class

The result class is the common ancestor ([built-in](Built-in_classes.md#commonparentclass) or [user-defined](User_classes.md#commonparentclass)) of its possible results.

### Language

To create a property implementing the general form of selection — explicit conditions with results — the [`CASE`](../language/CASE_operator.md) operator is used.

In the polymorphic form, [`MULTI`](../language/MULTI_operator.md) selects the operand whose signature matches the parameter classes; [`OVERRIDE`](../language/OVERRIDE_operator.md) returns the first operand whose value is not `NULL`; [`EXCLUSIVE`](../language/EXCLUSIVE_operator.md) does the same as `OVERRIDE` and additionally declares that at most one operand has a non-`NULL` value.

In the single form, the [`IF`](../language/IF_operator.md) operator returns the result when the condition is met; the [`IF ... THEN`](../language/IF_..._THEN_operator.md) operator additionally accepts an alternative result returned when the condition is not met.

### Examples

```lsf
CLASS Color;
id = DATA STRING[100] (Color);

background 'Color' (Color c) = CASE
    WHEN id(c) == 'Black' THEN RGB(0,0,0)
    WHEN id(c) == 'Red' THEN RGB(255,0,0)
    WHEN id(c) == 'Green' THEN RGB(0,255,0)
;

id (TypeExecEnv type) = CASE EXCLUSIVE
    WHEN type == TypeExecEnv.materialize THEN 3
    WHEN type == TypeExecEnv.disablenestloop THEN 2
    WHEN type == TypeExecEnv.none THEN 1
    ELSE 0
;
```

```lsf
nameMulti (Human h) = MULTI 'Male' IF h IS Male, 'Female' IF h IS Female;

CLASS Ledger;
CLASS InLedger : Ledger;
quantity = DATA INTEGER (InLedger);

CLASS OutLedger : Ledger;
quantity = DATA INTEGER (OutLedger);

signedQuantity (Ledger l) = MULTI quantity[InLedger](l), quantity[OutLedger](l);
```


```lsf
CLASS Group;
markup = DATA NUMERIC[8,2] (Group);

markup = DATA NUMERIC[8,2] (Book);
group = DATA Group (Book);
overMarkup (Book b) = OVERRIDE markup(b), markup(group(b));

notNullDate (INTEGER i) = OVERRIDE date(i), 2010_01_01;
```


```lsf
background 'Color' (INTEGER i) = EXCLUSIVE RGB(255,238,165) IF i <= 5,
                                                   RGB(255,160,160) IF i > 5;

CLASS Human;

CLASS Male : Human;
CLASS Female : Human;

name(Human h) = EXCLUSIVE 'Male' IF h IS Male, 'Female' IF h IS Female;
```


```lsf
name = DATA STRING[100] (Book);
hasName (Book b) = TRUE IF name(b);

background (Book b) = RGB(224, 255, 128) IF b IS Book;

countTags (Book b) = GROUP SUM 1 IF in(b, Tag t);
```


```lsf
price1 = DATA NUMERIC[10,2] (Book);
price2 = DATA NUMERIC[10,2] (Book);
maxPrice (Book b) = IF price1(b) > price2(b) THEN price1(b) ELSE price2(b);

// if h is of another class, it will be NULL
sex (Human h) = IF h IS Male THEN 'Male' ELSE ('Female' IF h IS Female); 

isDifferent(a, b) = IF a != b THEN TRUE;
```
