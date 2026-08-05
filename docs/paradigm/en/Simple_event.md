---
slug: "/Simple_event"
title: 'Simple event'
---

*Simple* event - an [event](Events.md) that occurs when the value of the specified [property](Properties.md) (which shall be called an event *condition*) changes to non-`NULL`. This event is a kind of extension of a basic event (adding an additional condition), which means that all the same parameters must be set as for an basic event, in particular:

-   event [type](Events.md#type), which determines the point in time when this simple event will occur (including checking the condition).
-   event [handler](Events.md) - an [action](Actions.md) to be executed upon the occurrence of this simple event

Compared to a basic event, a simple event merely runs its handler for every object collection that satisfies the event condition. However, simple events have a number of important additional features:

-   If the condition does not contain the [previous value operator (`PREV`)](Previous_value_PREV.md), the platform itself wraps the specified condition in a [change operator (`SET`)](Change_operators_SET_CHANGED_etc.md). This significantly reduces the risk of creating incorrect handling (which will have consequences if the change session is empty)
-   Such events are more understandable and readable, as they reflect the classic cause-effect relationship (when one thing occurs, another is done)
-   For these events there is the possibility of "recalculation" - that is, executing handlers in a mode where all previous values are `NULL` (as if the database were empty). This is useful when an event is created for an existing database, and the old data must also follow the logic of this event.
-   If the action in a simple event is a single [change](Property_change_CHANGE.md) of a given property, this event is easily made [calculated](Calculated_events.md) and vice versa.

The condition of a simple event is also checked on objects [deleted](Class_change_CHANGECLASS_DELETE.md) in the session: when an object is deleted, its data properties are reset to `NULL`, so a condition that reacts to the property value of an object becoming `NULL` (in particular, one wrapped in a change operator) is satisfied for every deleted object whose previous property value was non-`NULL`, and the handler runs for the already deleted object. If the handler must not run on deletion, a check of the object's class membership is added to the condition: for a deleted object it yields `NULL`, and the event does not occur. A class change that takes the object out of its former class acts in the same way as deletion.

### Language

To create simple events, use the [`WHEN` statement](../language/WHEN_statement.md).

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
