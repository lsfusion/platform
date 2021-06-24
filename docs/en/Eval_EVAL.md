---
title: 'Eval (EVAL)'
---

The eval operator creates an action that executes defined program code in the **lsFusion** language. Program code is defined as a property whose value should belong to the string class. This is implemented using an [action](Actions.md) which takes a line of code as an argument.

The code that is passed to an action must be a sequence of [statements](Statements.md). It is assumed that one of these statements will create an action named `run` (it is this action that will be executed).

### Action execution

This operator can also execute a single action (rather than a set of statements): in this case the code should be a sequence of [action operators](Action_operators_paradigm.md) and local property declarations. To refer to the parameters of the action, you can use the special character `$` and the parameter number (starting from `1`).

### Executable code restrictions

In the current implementation, executable code must not contain statements that modify existing objects in the system. For example, you cannot add [events](Events.md) or [data](Data_properties_DATA.md) and materialized properties, [extend](Extensions.md) existing objects, etc.

### Language

To declare an action that executes program code, use the [`EVAL` operator](EVAL_operator.md).

### Examples

```lsf
addProperty  { EVAL ACTION 'MESSAGE \'Hello World\''; }  // source code consisting of one statement is specified with a string constant

code 'Source code' = DATA BPSTRING[2000] ();              // a property to which the user can enter the source code
execute 'Execute code'  { EVAL code(); }      // the action executing this code
```
