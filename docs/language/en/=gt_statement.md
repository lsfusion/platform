---
slug: "/=gt_statement"
title: '=> statement'
---

The `=>` statement creates a [consequence](../paradigm/Simple_constraints.md).

### Syntax

```
leftPropertyId(param1, ..., paramN) => [eventClause] rightExpr [RESOLVE [LEFT] [RIGHT]];
```

### Description

The `=>` statement creates a consequence. This operator can declare its own local parameters when specifying the property of the consequence premise. These parameters can then be used in the expression of the consequence.

When creating a consequence a [constraint](../paradigm/Constraints.md) will be created, which is pretty similar to the following statement

```
CONSTRAINT eventClause leftPropertyId(param1, ..., paramN) AND NOT rightExpr MESSAGE 'Consequence violated';
```

but it allows you to automatically resolve situations where this constraint is violated. So using the `LEFT` option of `RESOLVE` is similar to creating [a simple event](../paradigm/Simple_event.md):

```
WHEN eventClause SET(leftPropertyId(param1, ..., paramN)) DO 
    SETACTION(rightExpr);
```

the `RIGHT` option, similarly:

```
WHEN eventClause DROPPED(rightExpr) DO
    DROPACTION(leftPropertyId(param1, ..., paramN));
```

### Parameters

- `leftPropertyId`

    [ID of the property](IDs.md#propertyid) specifying the consequence premise.

- `param1, ..., paramN`

    List of [parameters](IDs.md#paramid) of the property that defines the premise of the consequence. The number of these parameters must be equal to the number of parameters of the property.

- `rightExpr`

    [Expression](Expression.md) whose value determines the consequence.

- `LEFT`

    Turns on [auto resolution](../paradigm/Simple_event.md) of the consequence: if the premise (the left part of the statement) is changed to non-`NULL`, then the consequence changes to non-`NULL`.

- `RIGHT`

    Turns on auto resolution of the consequence: if the consequence (the right part of the statement) changes to `NULL`, then the premise changes to `NULL`.

- `eventClause`

    [Event description block](Event_description_block.md). Describes an [event](../paradigm/Events.md) upon the occurrence of which the created consequence will be checked and automatic resolution operations will be performed. If omitted, the global `APPLY` event is used.

### Examples

```lsf
is(Sku s) = s IS Sku;
// the product must have a barcode and name specified
is(Sku s) => barcode(s);
is(Sku s) => name(s);


CLASS Invoice;
CLASS InvoiceLine;
invoice = DATA Invoice (InvoiceLine);
is(InvoiceLine l) = l IS InvoiceLine;
// for a document line, a document must be specified, and when deleting a document, the lines of this document should be deleted
is(InvoiceLine l) => invoice(l) RESOLVE RIGHT;
// is equivalent to declaring document = DATA Invoice (InvoiceLine) NONULL DELETE;

// aggregation for f(a, b) create an object of class x, whose property a(x) equals a, and property b(x) equals b
CLASS A;
CLASS B;
f = DATA BOOLEAN (A, B);

CLASS X;
a = DATA A(X);
b = DATA B(X);
is (X x) = x IS X;

f(a,b) => [ GROUP AGGR X x WHERE x IS X BY a(x), b(x)](a,b) RESOLVE LEFT;
is(X x) => f(a(x), b(x)) RESOLVE RIGHT;
```

