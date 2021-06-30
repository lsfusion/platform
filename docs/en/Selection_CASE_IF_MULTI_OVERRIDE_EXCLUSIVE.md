---
title: 'Selection (CASE, IF, MULTI, OVERRIDE, EXCLUSIVE)'
---

The *selection* operator creates a property that determines for a set of *conditions* which condition is met, and returns the value of the *result* corresponding to that condition. If none of the conditions is met, then the value of the created property will be `NULL`. 

All conditions and results are defined as some properties and/or parameters. Accordingly, a condition *is satisfied* if the value of the property or parameter by which this condition is set is not equal to `NULL`. 

### Polymorphic form {#poly}

The platform also allows you to define a condition and the corresponding result with one property. In this case, the condition may be either matching the property's [signature](Property_signature_CLASS.md), or the property itself. We will call this the *polymorphic* form of the operator.


:::info
Note that the [extremum operator](Extremum_MAX_MIN.md) and [logical operators](Logical_operators_AND_OR_NOT_XOR.md) basically are also varieties of the selection operator (and of its polymorphic form, i.e. the conditions and result are defined by one property)
:::

### Mutual exclusion of conditions {#exclusive}

The selection operator lets you specify that all its conditions are *mutually exclusive*. If this option is specified, and the conditions are not in fact mutually exclusive, the platform will throw the corresponding error.

It is worth noting that this check is no more than a hint to the platform (for better optimization), and also a kind of self-checking on the part of the developer. However, in many cases it allows you to make the code more transparent and readable (especially with the polymorphic form of the selection operator).

### Implicit definition

This operator supports [implicit definition](Property_extension.md) using the technique of extensions, which allows, in particular, to implement polymorphism in the form that is common practice in OOP.

### Single form {#single}

The *single* form of the selection operator checks exactly one condition. If this condition is met, the value of the specified result is returned. It is also possible to specify an *alternative result* which value is returned if the condition is not met.


:::info
Type of mutual exclusion and implicit definition do not make sense/are not supported for this form of the operator
:::

### Determining the result class

The result class of the selection operator is the common ancestor (builtin or [user-defined](User_classes.md#commonparentclass)) of its operands.

### Language

To create a property implementing a general form of selection, the [`CASE`](CASE_operator.md) operator is used. The polymorphic form of the selection operator is implemented using the [`MULTI`](MULTI_operator.md), [`OVERRIDE`](OVERRIDE_operator.md) and [`EXCLUSIVE`](EXCLUSIVE_operator.md), operators; the single form using the [`IF`](IF_operator.md) and [`IF ... THEN`](IF_..._THEN_operator.md) operator (the only operator that allows the specification of an alternative result).

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

sex (Human h) = IF h IS Male THEN 'Male' ELSE ('Female' IF h IS Female); // if h is of another class, it will be NULL

isDifferent(a, b) = IF a != b THEN TRUE;
```
