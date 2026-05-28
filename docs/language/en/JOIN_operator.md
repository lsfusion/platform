---
slug: "/JOIN_operator"
title: 'JOIN operator'
---

The `JOIN` operator creates a [property](../paradigm/Properties.md) that implements a [composition](../paradigm/Composition_JOIN.md).

### Syntax

```
[JOIN] mainProperty(expr1, ..., exprN)
```

Where `mainProperty` is defined as:

```
propertyId

"[" operator "]"

"[" expression "]"
```

Where `"["` and `"]"` are ordinary brackets.

### Description

When the main property is given in brackets — as a [context-independent](Property_operators.md#contextindependent) property operator or an [expression](Expression.md) — the property is built anonymously at the place of use, so an intermediate property need not be declared via the [`=` statement](=_statement.md).

An operator or expression in brackets may reference external parameters; the parameters of the resulting anonymous property follow the same rules as a property defined with `=` without explicit parameters.

### Parameters

- `propertyId`

    [Property ID](IDs.md#propertyid) of an existing property.

- `operator`

    A context-independent property operator in brackets.

- `expression`

    An expression in brackets.

- `expr1, ..., exprN`

    List of expressions supplying the arguments of the main property. The number of expressions must equal the main property's parameter count; the list is empty when the main property has no parameters.

### Examples

```lsf
f = DATA INTEGER (INTEGER, INTEGER, INTEGER);
g = DATA INTEGER (INTEGER, INTEGER);
h = DATA INTEGER (INTEGER, INTEGER);
c(a, b) = f(g(a, b), h(b, 3), a);

count = DATA BPSTRING[255] (INTEGER);
name = DATA BPSTRING[255] (INTEGER);
formatted(INTEGER a, INTEGER b) = [FORMULA BPSTRING[255] ' CAST($1 AS TEXT) || \' / \' || CAST($2 AS TEXT)'](count(a), name(b));
```

Sometimes it’s convenient to define the main property with an expression to simplify the source code and make it more understandable.

```lsf
CLASS Triangle;
cathetus1 = DATA DOUBLE(Triangle);
cathetus2 = DATA DOUBLE(Triangle);

hypotenuseSq(triangle) = cathetus1(triangle)*cathetus1(triangle) + cathetus2(triangle)*cathetus2(triangle);

// a similar property set using composition
hypotenuseSq2(triangle) = [ x*x + y*y](cathetus1(triangle), cathetus2(triangle)); 
```
