---
title: 'DIALOG operator'
---

The `DIALOG` operator creates an [action](Actions.md) that [opens a form](In_an_interactive_view_SHOW_DIALOG.md) in an interactive view in [dialog](In_an_interactive_view_SHOW_DIALOG.md#dialog) mode. 

## Syntax

    DIALOG name
    [OBJECTS objName1 [= expr1] [NULL] [inputOptions1], ..., objNameN [= exprN] [NULL] [inputOptionsN]]
    [formActionOptions]
    [DO actionOperator [ELSE elseActionOperator]]

When opening the list/edit form, the syntax is slightly different:

    DIALOG classFormType className
    [= expr] [NULL] [inputOptions]
    [formActionOptions]
    [DO actionOperator [ELSE elseActionOperator]]

`inputOptions` – object options that determine whether the last current values of this object should be returned after the form is closed and what to do next with these values (defined with one of the following syntaxes):

    INPUT [alias] [NULL]
    CHANGE [= changeExpr] [NOCONSTRAINTFILTER] [alias] [NULL]

`formActionOptions` - additional options for this action. They can be added one by one in any order:

    windowType
    MANAGESESSION | NOMANAGESESSION
    CANCEL | NOCANCEL
    NEWSESSION | NESTEDSESSION
    READONLY

## Description

The `DIALOG` operator creates an action that opens the specified form. When opening a form in the `OBJECTS` block you can [specify](Open_form.md#params) [initial values](Value_input.md#initial) for the [form objects](Form_structure.md) and also return the last current values to the specified parameters and execute the `DO` action which will process the received values in case the input has not been cancelled.

## Parameters

- `name`

    Form name. [Composite ID](IDs.md#cid).

- `classFormType`

    Keyword. Determines which form to open:

    - `LIST` – list form
    - `EDIT` – edit form

- `className`

    The name of the custom class for which a list/edit form should be opened. [Composite ID](IDs.md#cid).

- `objName1 ... objNameN`

    Names of form objects for which initial values are specified. [Simple IDs](IDs.md#id).

- `expr1 ... exprN`

    [Expressions](Expression.md) which values determine the initial values for form objects.

- `NULL`

    Specifies that `NULL` values can be passed. This option is automatically enabled if the object returns a value.

### Input options

- `INPUT`

    Keyword. If used, then the last current value of the object of the opened form will be written to the specified local parameter and, if necessary, to the specified property.

- `alias`

    The name of the local parameter to which the input result is written. [Simple ID](IDs.md#id). If the name is not specified, then the name of the form object will be used as the parameter name.

- `CHANGE`

    A keyword indicating that in addition to the value input the result needs to be written to the specified property. Also by default, this option sets an additional filter for the object values on the opened form so that when selecting an object and changing the specified property to the value of this object, none of the existing [constraints](Constraints.md) are violated. 

- `NOCONSTRAINTFILTER`

    If used, no additional filter for constraint compliance is set.

- `changeExpr`

    An [expression](Expression.md) that determines the property to which the input result is written. By default, the property specified as the initial object value is used.

- `NULL`

    A keyword that determines whether the user can select `NULL` as the return value (using the special "Drop" action – `System.formDrop`). If not used, will be enabled if and only if the `CHANGE` option is specified.

- `actionOperator`

    A [context-dependent action operator](Action_operators.md#contextdependent) that is executed if the input was completed successfully.

- `elseActionOperator`

    A [context-dependent action operator](Action_operators.md#contextdependent) that is executed if the input was cancelled. Parameters added to the input block `INPUT` cannot be used as parameters.

### Additional options

- `windowType`

    Method of [the form layout](In_an_interactive_view_SHOW_DIALOG.md#location):

    - `DOCKED` - as a tab.
    - `FLOAT` - as a window. Used by default.

- `MANAGESESSION` | `NOMANAGESESSION`

    Keywords. Determine whether or not the created form is considered to be the [session owner](Interactive_view.md#owner) (if so, the corresponding buttons for managing the session will be shown on the form in interactive mode). By default, the platform tries to determine which mode to use [automatically](Interactive_view.md#sysactions) depending on the context.

- `CANCEL` | `NOCANCEL`

    Keywords. Determine whether or not to show the "Cancel" system action (`System.formCancel`) on the form. By default, the platform tries to determine which mode to use [automatically](Interactive_view.md#sysactions) depending on context.

- `NEWSESSION | NESTEDSESSION`

    Keywords. Determine that the form will be opened in a new (nested) session. By default, the form is opened in the current session.

- `READONLY`

    Keyword. If specified, the form is opened in [read-only](In_an_interactive_view_SHOW_DIALOG.md#extra) mode.

## Examples

```lsf
FORM selectSku
    OBJECTS s = Sku
    PROPERTIES(s) id
;

testDialog  {
    DIALOG selectSku OBJECTS s INPUT DO {
        MESSAGE 'Selected sku : ' + id(s);
    }
}

sku = DATA Sku (OrderDetail);
idSku (OrderDetail d) = id(sku(d));

changeSku (OrderDetail d)  {
    DIALOG selectSku OBJECTS s = sku(d) CHANGE;

    //equivalent to the first option
    DIALOG selectSku OBJECTS s = sku(d) INPUT NULL CONSTRAINTFILTER DO {
        sku(d) <- s;
    }
}
```
