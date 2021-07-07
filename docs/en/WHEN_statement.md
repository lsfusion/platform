---
title: 'WHEN statement'
---

The `WHEN` statement adds a [simple event](Simple_event.md) handler.

### Syntax 

    WHEN eventClause eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;

### Description

The `WHEN` statement adds a simple event handler. In a condition expression you can implicitly declare local parameters that can then be used in the event handler.

Also, the `ORDER` block can be used to define the order in which the handler will be called for an object collection for which the condition on the simple event has been met. 


:::info
Using the `WHEN` statement is much like the following statement:

    ON eventClause FOR eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;

but it also has [a number of advantages](Simple_event.md).
:::

### Parameters

- `eventClause`

    [Event description block](Event_description_block.md). Describes the [base event](Events.md) for the created handler.

- `eventExpr`

    An [expression](Expression.md) whose value is used as a condition for the created simple event. If the obtained property does not contain the [`PREV`](Previous_value_PREV.md) operator, the platform automatically wraps it into the [`CHANGE`](Property_change_CHANGE.md) operator.

- `eventAction`

    A [context-dependent operator](Action_operators.md#contextdependent) that describes an action to be added as an event handler.

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections. 

- `orderExpr1, ..., orderExprM`

    A list of expressions that defines the order in which handlers will be called for object collections for which an event condition has been met. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc. 

### Examples

```lsf
CLASS Stock;
name = DATA STRING[50] (Stock);

balance = DATA INTEGER (Sku, Stock);

// send an email when the balance is less than 0 as a result of applying session changes
WHEN balance(Sku s, Stock st) < 0 DO
      EMAIL SUBJECT 'The balance has become negative for the item ' + name(s) + ' in the warehouse ' + name(st);

CLASS OrderDetail;
order = DATA Order (OrderDetail) NONULL DELETE;
discount = DATA NUMERIC[6,2] (OrderDetail);

WHEN LOCAL CHANGED(customer(Order o)) AND name(customer(o)) == 'Best customer' DO
    discount(OrderDetail d) <- 50 WHERE order(d) == o;
```

