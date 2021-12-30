---
title: 'Actions'
---

An *action* is an element of the system that takes a set of objects (*parameters*) and uses them in one way or another to change the system state (that of the system in which the action is executed, as well as the state of any other external system).

The type and behavior of each action is determined by the [operator](Action_operators_paradigm.md) that creates the action.

### Language

To add a new action to the system, use the [`ACTION` statement](ACTION_statement.md).

### Examples

```lsf
showMessage  { MESSAGE 'Hello World!'; } 								// action declaration
loadImage 'Upload image'  ABSTRACT ( Item);
```
