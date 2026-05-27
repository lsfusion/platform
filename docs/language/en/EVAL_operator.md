---
slug: "/EVAL_operator"
title: 'EVAL operator'
---

The `EVAL` operator creates an [action](../paradigm/Actions.md) that [executes code](../paradigm/Eval_EVAL.md) in the **lsFusion** language.

### Syntax

```
EVAL [ACTION] expression [PARAMS paramExpr1, ..., paramExprN]
```

### Description

The string produced by `expression` is wrapped by the platform into a uniquely-named module whose dependencies cover every loaded module of the project, then executed. Without `ACTION` the code is treated as a sequence of [statements](Statements.md); one of those statements must declare an action named `run`, which is the action that gets executed. With `ACTION` the code is treated as the body of `run` directly — a sequence of [action operators](Action_operators.md) and local property declarations — and runtime arguments are addressed positionally as `$1, $2, ...`.

Runtime arguments are supplied via the optional `PARAMS` block — a list of expressions whose values are passed to `run` as positional arguments. Without `PARAMS` `run` is invoked with no arguments.

### Parameters

- `ACTION`

    Optional keyword. When specified, the source code is taken as the body of `run` directly, rather than as a sequence of statements that declares it.

- `expression`

    An [expression](Expression.md) whose value is the source code string to be executed.

- `paramExpr1, ..., paramExprN`

    Non-empty list of expressions whose values are passed to `run` as positional arguments. The count must match the parameter count of `run`.

### Examples

```lsf
// statements form: the source string declares a `run` action
code 'Source code' = DATA BPSTRING[2000] ();
execute 'Execute code' { EVAL code(); }

// ACTION form: the string is the body of `run` directly
addProperty { EVAL ACTION 'MESSAGE \'Hello World\''; }

// ACTION form with PARAMS — runtime arguments addressed as `$1, $2, ...`
greet 'Greet user' (CustomUser u) { EVAL ACTION 'MESSAGE \'Hello, \' + name($1);' PARAMS u; }

// statements form with PARAMS — values bind positionally to `run`'s parameters
greetByName 'Greet by name' (STRING n) {
    EVAL 'run(STRING name) \{ MESSAGE \'Hello, \' + name; \}' PARAMS n;
}
```
