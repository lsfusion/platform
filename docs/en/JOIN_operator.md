---
title: 'JOIN operator'
---

The `JOIN` operator creates a [property](Properties.md) that implements a [composition](Composition_JOIN.md).

### Syntax

    [JOIN] propertyId(expr1, ..., exprN)
     
    [JOIN] "[" operator "]" (expr1, ..., exprN)
     
    [JOIN] "[" expression "]" (expr1, ..., exprN) 

Where `"["` and `"]"` are ordinary brackets.

### Description 

The `JOIN` operator creates a property that implements a composition of properties. The [main property](Composition_JOIN.md) can be defined by one of the three following options:

- an ID of the existing property
- a [context independent](Property_operators.md#contextindependent) [property operator](Property_operators.md) enclosed in brackets.
- an [expression](Expression.md) enclosed in brackets.

The latter two options allow to use as the main property a property without a name which is created right at the place of use. In certain cases, this can make the code more concise and avoids the explicit declaration of an intermediate property using the [`=` statement](=_statement.md) that will not be used anywhere else. An operator or expression enclosed in brackets with an equal sign can use external parameters if necessary. When determining the parameters of the created "anonymous" property, the same rules apply as when creating the property in the [`=` statement](=_statement.md), in the case when the parameters are not defined explicitly.    

Formally, the `JOIN` operator is also responsible for such constructions as `propertyID(a, b)`, i.e. just an existing property with the parameters passed to it. In such cases, when possible, the `JOIN` operator will not create a new anonymous property but return the property with the `propertyID`.

### Parameters

- `propertyId`

    [Property ID](IDs.md#propertyid). 

- `expr1, ..., exprN`

    A list of expressions defining the arguments of the main property. The number of expressions should be equal to the number of parameters of the main property.

- `operator`

    A [context-independent](Property_operators.md) property operator that is used to create the main property.

- `expression`

    An [expression](Expression.md) which is used to define the main property. Cannot be a single parameter.

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
