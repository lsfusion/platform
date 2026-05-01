---
title: 'Actions'
---

An *action* is an element of the system that takes a set of objects (*parameters*) and uses them in one way or another to change the system state (that of the system in which the action is executed, as well as the state of any other external system).

The type and behavior of each action is determined by the [operator](Action_operators_paradigm.md) that creates the action.

An action may additionally declare a *result* — a value of a fixed class returned to the caller by the [exit operator](Exit_RETURN.md) inside the action body.

### Language

To add a new action to the system, use the [`ACTION` statement](ACTION_statement.md).

### Examples

```lsf
CLASS Item;
inStock (Item i) = DATA BOOLEAN (Item);

// an action describes an effect — here, changing a stored property
markInStock (Item i)  {
    inStock(i) <- TRUE;
}

// an action can declare a result returned to the caller
getDescription (Item i) ABSTRACT STRING[100];
```
