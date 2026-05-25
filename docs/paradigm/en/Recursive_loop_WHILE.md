---
slug: "/Recursive_loop_WHILE"
title: 'Recursive loop (WHILE)'
---

The *recursive loop operator* is similar to a regular [loop](Loop_FOR.md) operator, with the only difference being that iteration is performed recursively until at a certain point the set of objects collections satisfying the condition becomes empty. This operator does not allow an *alternative* action.

At each step, the condition is re-evaluated against the current state of the data, the matching object collections are read again, and the main action runs once per collection from the new set. Iteration stops when the re-evaluated set is empty. Because the set is recomputed every step, changes made by the main action — including changes that affect the condition itself — are taken into account on the next step. If the main action does not eventually exhaust the matching set, the loop runs without termination.

Inside the main action, the [interruption operator](Interruption_BREAK.md) exits this loop, the [next iteration operator](Next_iteration_CONTINUE.md) moves to the next object collection within the current step (and after the last collection of the step the set is recomputed as usual), and the [exit operator](Exit_RETURN.md) propagates outward, exiting the surrounding action call.

### Language

The syntax of the recursive loop operator is described by the [`WHILE` operator](../language/WHILE_operator.md).

### Examples

```lsf
iterateDates (DATE dateFrom, DATE dateTo)  {
    LOCAL dateCur = DATE();

    dateCur() <- dateFrom;
    WHILE dateCur() <= dateTo DO {
        MESSAGE 'I have a date ' + dateCur();
        dateCur() <- sum(dateCur(), 1);
    }
}
```
