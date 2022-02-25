---
title: 'Events'
---

*Events* are a mechanism that allows to execute certain [actions](Actions.md) at certain points in time (these actions shall be called event *handlers*).

### Event types {#type}

There are two types of events:

-   *Synchronous* - occur immediately after a data change.
-   *Asynchronous* - occur at arbitrary points in time as the server manages to complete execution of all defined handlers, and/or after a certain period of time.

In turn, from the perspective of the scope of changes, events can be divided into:

-   *Local* - occur locally for each change session.
-   *Global* - occur globally for the entire database.

Thus, events can be synchronous local, synchronous global, asynchronous local, and asynchronous global. Note that by default global events are synchronous and local events are asynchronous (as the most commonly used combinations). Also, by default, all events are global.

Advantages of synchronous events:

-   If necessary, you can cancel the changes in the handler if, for example, these changes do not meet the necessary conditions.
-   They guarantee greater integrity, since after the changes have been written the user is guaranteed to be working with the updated data.

Advantages of asynchronous events:

-   You can release the user immediately and perform the handling "in the background". This improves system ergonomics; however, it is possible only when updating the data is not critical for the user's further work (for global events, for example, within the next 5-10 minutes, until the server has time to complete the next handling cycle).
-   Handlings are grouped for a large number of changes, including those made by different users (in the case of global events), and, accordingly, are performed fewer times, thereby improving the overall system performance.

Advantages of local events:

-   The user sees the results of event handling immediately, not only after he has saved to the common database.

Advantages of global events:

-   They provide better performance and integrity, due both to the fact that the handling is performed only after the changes are saved to the common database (that is, significantly less often), and to the use of the numerous DBMS capabilities for working with transactions.

The platform also allows to additionally specify that the event will occur only if the change session belongs to one of the given forms. If this is not done, then it must be kept in mind that most of the described events occur very often, so their handling should not have side effects (for example, showing messages) if there are no changes in the session. Ideally, events should be [simple](Simple_event.md) and should generally be used only to optimize the performance of really complex cases.

### Change operators' event mode {#change}

When handling events, you can use a special mode of the [previous value](Previous_value_PREV.md) operator (it shall be called *event* mode). In this mode, the previous value operator will return the property's values not at the beginning of the session, but at the time of the previous occurrence of this event (or rather, the end of its handling). A similar mode is supported for all [change](Change_operators_SET_CHANGED_etc.md) operators.

Event mode is also supported for the [cancel changes](Cancel_changes_CANCEL.md) operator. In this case, when changes are canceled inside the global event handler, the session is not cleared, but [applying changes](Apply_changes_APPLY.md) which led to the execution of this handler is canceled. The global event must be synchronous, otherwise the platform will throw an error.

By default, the following modes are used in event handling:

-   for the previous value operator: standard mode (value at the beginning of the session)
-   for change operators - event (value at the time the previous event occurred). 
-   for the cancel changes operator - event mode (canceling the application, not clearing the session).


:::info
For change operators and the previous value operator, when executing global synchronous event handlers, these modes (standard and event) coincide
:::

### Language

To create actions that handle events, use the [`ON` statement](ON_statement.md).

### Examples

```lsf
CLASS Sku;
name = DATA STRING[100] (Sku);

ON {
    LOCAL changedName = BOOLEAN (Sku);
    changedName(Sku s) <- CHANGED(name(s));
    IF (GROUP SUM 1 IF changedName(Sku s)) THEN {
        MESSAGE 'Changed ' + (GROUP SUM 1 IF changedName(Sku s)) + ' skus!!!';
    }
}

CLASS Order;

CLASS Customer;
name = DATA STRING[50] (Customer);

customer = DATA Customer (Order);
discount = DATA NUMERIC[6,2] (Order);

ON LOCAL {
    FOR CHANGED(customer(Order o)) AND name(customer(o)) == 'Best customer' DO
        discount(o) <- 50;
}
```

 
