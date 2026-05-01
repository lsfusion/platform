---
title: 'ACTION statement'
---

The `ACTION` statement creates an [action](Actions.md).

### Syntax

```
name [caption] [(param1, ..., paramN)] { actionBody } [options]
name [caption] [(param1, ..., paramN)] contextIndependentOperator [options];
```

### Description

The `ACTION` statement declares a new action and adds it to the current [module](Modules.md).

The statement has two forms: the first form creates an action using the [`{...}` operator](Braces_operator.md), a [context-dependent](Action_operators.md#contextdependent) action operator that runs a sequence of inner actions, the second form creates a [context-independent](Action_operators.md#contextindependent) one. In the first form the closing brace already ends the statement, so the trailing semicolon is not required (an extra one is allowed by the [empty statement](Empty_statement.md) rule).

Also, when declaring an action, a set of its options can be specified.

### Parameters

- `name`

    Action name. [Simple ID](IDs.md#id).

- `caption`

    Action caption. [String literal](Literals.md#strliteral). If no caption is defined, the action name will be its caption.  

- `param1, ..., paramN`

    List of parameters. Each of them is defined by a [typed parameter](IDs.md#paramid). The list may be empty. These parameters can be later used in the action operator describing the action being created (as well as in some options).

    If parameters are not defined explicitly, they will be automatically calculated when the operator is processed. The order of the parameters will match the order of their appearance in the operator. It is recommended to explicitly define action parameters. This will help find typos and other errors in the declaration (for example, a mismatch of the number of defined parameters with the number of parameters of the created action).

- `actionBody`

    Body of the [`{...}` operator](Braces_operator.md): a sequence of [action operators](Action_operators.md) and, where needed, `LOCAL` declarations. The body may be empty. The parameters defined in this statement (if any) can be used inside `actionBody`.

- `contextIndependentOperator`

    A [context-independent](Action_operators.md#contextindependent) action operator describing and creating an action.

- `options`

    [Action options](Action_options.md). 

### Examples

```lsf
// action declared with the {...} operator; the trailing semicolon may be omitted
showMessage  { MESSAGE 'Hello World!'; }

// context-independent ABSTRACT operator
loadImage 'Upload image' ABSTRACT (Item);

// explicit parameter list, body with several action operators and a LOCAL property
copy (Item from, Item to)  {
    LOCAL temp = STRING[100] ();
    temp() <- name(from);
    name(to) <- temp();
}

// abstract action with a return class
getPrice (Item i) ABSTRACT NUMERIC[10,2];

// context-independent INTERNAL operator — inline Java snippet
ping INTERNAL <{ System.out.println("ping"); }>;

// action with options — placed in toolbar, asks for confirmation when run
deleteItem (Item i)  { DELETE i; } TOOLBAR CONFIRM;
```
