---
slug: "/Brief_logic"
title: 'Brief: domain logic'
---

- [Classes](#classes)
- [Properties](#properties)
- [Actions](#actions)
- [Events](#events)
- [Constraints](#constraints)
- [Change sessions](#change-sessions)

## Classes

### What a class is

A [class](../paradigm/Classes.md) is a set of objects, and the base element
everything else is typed by: the signature of a property or an action is the
classes of its parameters, and a form's objects each have one. Classes may
inherit, including from several parents at once.

```
CLASS [ABSTRACT] name [caption] [: parent1, ..., parentN];
CLASS [NATIVE] name [caption] [{ objectName1 [objectCaption1], ... }] [: parent1, ..., parentN];
```

The second form declares a class with STATIC objects — a fixed, named set
declared in the code rather than created at runtime.

**Analogy**: OOP classes, except that dispatch is multiple — by the classes of
all the parameters, not of one receiver.

### A class is not a table

This is the single most expensive misreading of the model, so it is worth
stating plainly. A table does not hold a class's objects; it holds the values
of properties. Its key fields hold object ids, and the parameter classes of
those properties are what the key fields are typed by. A property declared
without an explicit `TABLE` goes to the table whose key classes fit it. Which
means the mapping from classes to tables is a consequence of how properties are
declared, not something the class declaration decides. See
[Brief: physical model](Brief_physical.md#execution).

### Polymorphism

Behaviour is specialized by class through `ABSTRACT` properties and actions
with `+=` implementations, and through `MULTI`. See
[abstract properties](#properties) and [extending classes](Brief_physical.md#extensions).

## Properties

### Expressions and composition

A [property](../paradigm/Properties.md) takes a set of objects as parameters and returns exactly one value — like a pure function, but computed over the whole database at once, like a column of an SQL query. The value is either stored (a [data property](../paradigm/Data_properties_DATA.md), the [`DATA` operator](../language/DATA_operator.md)) or computed by an expression: arithmetic, logic (`AND`, `OR`, `NOT`), comparisons, string operations, class tests and casts (`IS`, `AS`). String, number and date functions (`lpad`, `substr`, `mod`, `currentDate`) are not operators — they are properties of the `Utils` and `Time` modules.

Substituting a property into another property's expression is [composition](../paradigm/Composition_JOIN.md); the [`JOIN` operator](../language/JOIN_operator.md) writes it out explicitly. The operators that create properties are listed in [Operators](../paradigm/Property_operators_paradigm.md).

```lsf
price = DATA NUMERIC[14,2] (Item);
vat = DATA NUMERIC[6,2] (Item);
priceWithVAT (Item i) = price(i) * (1 + vat(i) / 100);
```

### Grouping (GROUP)

The [`GROUP` operator](../language/GROUP_operator.md) splits all object collections into groups and computes one aggregate function per group — `SUM`, `MAX`, `MIN`, `CONCAT`, `LAST`, `EQUAL`, `AGGR` / `NAGGR`, `CUSTOM` (a DBMS aggregate):

```
GROUP
type [expr1, ..., exprN]
[orderClause]
[TOP topExpr] [OFFSET offsetExpr]
[WHERE whereExpr]
[BY groupExpr1, ..., groupExprM]
```

There is no separate counting function: a count is written as `GROUP SUM 1`. **Analogy**: SQL `GROUP BY`, except that the result is a property of its own rather than part of a query. The mechanism — which object collections fall into a group, what the parameters of the created property are, and where the order matters — is described in [grouping](../paradigm/Grouping_GROUP.md).

```lsf
sold (Sku s) = GROUP SUM quantity(OrderDetail d) BY sku(d);
```

### Partitioning and ordering (PARTITION)

The [`PARTITION` operator](../language/PARTITION_operator.md) also splits object collections into groups with a `BY` block, but returns a result not per group but per object collection, over an `ORDER`ed window inside its group. The aggregate functions here are `SUM`, `PREV`, `LAST`, `CUSTOM`: hence places and ranks, running numbering, cumulative totals, the value of the previous or the last collection in the window. **Analogy**: SQL window functions (`OVER (PARTITION BY ... ORDER BY ...)`). What the window covers is described in [partition / order](../paradigm/Partitioning_sorting_PARTITION_..._ORDER.md).

The `PARTITION UNGROUP` form solves the reverse task — it [distributes](../paradigm/Distribution_UNGROUP.md) the value of a source property over the object collections of a group: proportionally to a given expression (`PROPORTION`) or in order (`LIMIT`).

```lsf
place (Team t) = PARTITION SUM 1 ORDER DESC points(t), t BY conference(t);
```

### Object aggregation (GROUP AGGR, AGGR)

These operators work with objects rather than with values.

`GROUP AGGR` is the form of the [`GROUP` operator](../language/GROUP_operator.md) that returns the group's object itself. The result is the mapping inverse to the properties listed in `BY` — finding an object by its code, for example; the platform adds a constraint that a group has at most one such object.

The [`AGGR` operator](../language/AGGR_operator.md) goes further: it creates the object itself when the aggregated expression becomes not `NULL`, and deletes it when the expression becomes `NULL` again, filling in the properties that map the object back to the parameters. The mechanism is described in [aggregations](../paradigm/Aggregations.md).

```lsf
countryName = GROUP AGGR Country c BY name(c);
shipment (Invoice i) = AGGR ShipmentInvoice WHERE createShipment(i);
```

### Selection and override (CASE, IF, OVERRIDE)

The [selection operator](../paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) checks conditions in order and returns the result of the first one that holds; a condition holds if its value is not `NULL`.

- [`CASE`](../language/CASE_operator.md) — explicit `WHEN ... THEN ...` pairs and an optional `ELSE`.
- [`IF`](../language/IF_operator.md) — the postfix single form `result IF condition`; [`IF ... THEN`](../language/IF_..._THEN_operator.md) adds an `ELSE` block.
- [`OVERRIDE`](../language/OVERRIDE_operator.md) — the first operand that is not `NULL`; this is also how a default value is substituted for `NULL`.
- [`EXCLUSIVE`](../language/EXCLUSIVE_operator.md) — the same, plus a declaration that at most one operand is not `NULL`.
- [`MULTI`](../language/MULTI_operator.md) — the operand is chosen by the compatibility of the argument classes with its signature.

```lsf
signedQuantity (Ledger l) = MULTI quantity[InLedger](l), quantity[OutLedger](l);
price (Item i) = OVERRIDE salePrice(i), basePrice(i), 0;
```

### Recursion (RECURSION)

The [`RECURSION` operator](../language/RECURSION_operator.md) creates a property computed by iteration; it is reached for on trees, graphs and transitive closures — the level of a node, all the ancestors of an object, reachability along a chain of references. Its parts — `STEP`, the `$` prefix on a parameter and the `CYCLES` option — and the way the iterations are computed are described in [recursion](../paradigm/Recursion_RECURSION.md).

```lsf
level (Group child, Group parent) = RECURSION 1 IF child IS Group AND parent = child
                                              STEP 1 IF parent = parent($parent);
```

### Abstract properties (ABSTRACT)

The [`ABSTRACT` operator](../language/ABSTRACT_operator.md) declares a property without an implementation: the base module sets the value class and the parameter classes, and other modules add implementations with the [`+=` statement](../language/plus_equals_statement.md). From them the platform assembles a selection operator — this is [property extension](../paradigm/Property_extension.md), a way to remove a dependency between modules and to get property polymorphism.

Which implementation is chosen, and what is required of the implementations, is set by options — `MULTI`, `CASE`, `VALUE`, `EXCLUSIVE`, `OVERRIDE`, `FULL` — and is described in [property extension](../paradigm/Property_extension.md).

```lsf
name 'Name' = ABSTRACT ISTRING[250] (Document);              // base module
name (Shipment s) += ISTRING[250]('Shipment ' + number(s));  // shipment module
```

## Actions

### State changes (`<-`, NEW, DELETE)

The [`CHANGE` operator](../language/CHANGE_operator.md) writes a value into a changeable property, the [`NEW` operator](../language/NEW_operator.md) adds an [object](../paradigm/New_object_NEW.md) of a concrete class, and the [`DELETE`](../language/DELETE_operator.md) and [`CHANGECLASS`](../language/CHANGECLASS_operator.md) operators delete an object or move it to another class:

```
[CHANGE] propertyId(expr1, ..., exprN) <- valueExpr [WHERE whereExpr]
NEW className WHERE whereExpr [TO propertyId(prm1, ..., prmN)]
NEW [alias =] className [AUTOSET] action
DELETE expr [WHERE whereExpr]
CHANGECLASS expr TO className [WHERE whereExpr]
```

A [change](../paradigm/Property_change_CHANGE.md) is written for all argument sets satisfying the condition at once, as a single set operation. **Analogy**: `UPDATE ... SET ... WHERE`, not an assignment to a variable. On a [class change or a deletion](../paradigm/Class_change_CHANGECLASS_DELETE.md) the platform clears the stored values of the data properties the object is no longer valid in.

```lsf
setDiscount () {
    discount(Customer c) <- 15 WHERE totalOrders(c) > 100;
    NEW o = Order { date(o) <- currentDate(); }
}
```

### Calls and sequencing

An [action](../paradigm/Actions.md) is dual to a property: a property says what the value is, an action says how it changes. It is declared by the [`ACTION` statement](../language/ACTION_statement.md):

```
name [caption] [(param1, ..., paramN)] { actionBody } [options]
```

The body in braces is a [sequence](../paradigm/Sequence.md): nested actions run in the order written; inside the block `LOCAL` properties can be declared that live only while the block runs. An [action call](../paradigm/Call_EXEC.md) is written as the name with arguments, `[EXEC] actionId(expression1, ..., expressionN) [TO toProperty]`, or substituted directly as a value. **Analogy**: a procedure call.

### Loops (FOR, WHILE)

The [`FOR` operator](../language/FOR_operator.md) runs its body once per object set for which the condition is not `NULL`; the [`WHILE` operator](../language/WHILE_operator.md) recomputes the condition at every step, so the changes made by the body are taken into account:

```
FOR expression [ORDER [DESC] orderExpr1, ..., orderExprN]
[TOP topExpr] [OFFSET offsetExpr]
[NEW [alias =] className]
DO action
[ELSE alternativeAction]

WHILE expression [ORDER [DESC] orderExpr1, ..., orderExprN]
[NEW [alias =] className]
DO action
```

A loop is used when the body is genuinely row-by-row — a dialog, a message, an external call. The mechanisms are described in [loop](../paradigm/Loop_FOR.md) and [recursive loop](../paradigm/Recursive_loop_WHILE.md).

```lsf
createDetails (Order o) {
    FOR in(Sku s) NEW d = OrderDetail DO {
        order(d) <- o;
        sku(d) <- s;
    }
}
```

### Branching (CASE, IF)

[Branching](../paradigm/Branching_CASE_IF_MULTI.md) runs the action matching the condition that holds; a condition holds if its value is not `NULL`. In the [`IF ... THEN`](../language/IF_..._THEN_action_operator.md) and [`CASE`](../language/CASE_action_operator.md) operators the condition is written out; in the [`MULTI` operator](../language/MULTI_action_operator.md) it is that the call arguments match an action's signature, that is, dispatch by the argument class:

```
IF condition
THEN action
[ELSE alternativeAction]

CASE [exclusionType]
    WHEN condition1 THEN action1
    ...
    WHEN conditionN THEN actionN
    [ELSE elseAction]

MULTI [exclusionType] action1, ..., actionN
```

```lsf
message (Shape s) { MULTI { message[Square](s); }, { message[Circle](s); } }
```

The deferred variant is an abstract action, [`ABSTRACT`](../language/ABSTRACT_action_operator.md): a base module declares the extension point and other modules add implementations to it ([action extension](../paradigm/Action_extension.md)).

### Flow control

- [`BREAK`](../paradigm/Interruption_BREAK.md) exits the innermost loop, [`CONTINUE`](../paradigm/Next_iteration_CONTINUE.md) moves to its next iteration, [`RETURN [expression]`](../paradigm/Exit_RETURN.md) exits the innermost action call with the given value as its result.
- [`TRY action [CATCH catchAction] [FINALLY finallyAction]`](../language/TRY_operator.md) — `CATCH` swallows the [error](../paradigm/Exception_handling_TRY.md), giving access to it through `messageCaughtException[]` and `lsfStackTraceCaughtException[]`; `FINALLY` runs in any case. **Analogy**: `try` / `catch` / `finally`.
- [`NEWTHREAD action [dispatchClause]`](../language/NEWTHREAD_operator.md) — execution in a [separate thread](../paradigm/New_threads_NEWTHREAD_NEWEXECUTOR.md), at once or on a schedule (`SCHEDULE`: a delay and a period). [`NEWEXECUTOR`](../language/NEWEXECUTOR_operator.md) picks where the thread goes: a server pool, where the body runs in the caller's [change session](../paradigm/Change_sessions.md), or the client connection (`CLIENT`), where it gets a new session of its own in that connection's navigator.

### Form actions

- [`SHOW`](../language/SHOW_operator.md) — [opening a form](../paradigm/In_an_interactive_view_SHOW_DIALOG.md) in the interactive view; the passed objects become the current ones.
- [`DIALOG`](../language/DIALOG_operator.md) — the same opening as a value-input dialog: every object marked `INPUT` or `CHANGE` returns its last current value to the `DO` block.
- [`ACTIVATE`](../language/ACTIVATE_operator.md) — [activation](../paradigm/Activation_ACTIVATE.md) of a form, a tab, a property, or a set of objects in an object group.
- [`EXPAND`](../language/EXPAND_operator.md) / `COLLAPSE` — expanding and collapsing a [form container](../paradigm/Container_visibility_EXPAND_COLLAPSE.md) and the nodes of an [object tree](../paradigm/Object_tree_visibility_EXPAND_COLLAPSE.md).
- [`MESSAGE`](../paradigm/Show_message_MESSAGE_ASK.md) and [`INPUT`](../paradigm/Value_input.md) — a message and a value input without a separate form.

```
ACTIVATE FORM formName
ACTIVATE TAB formName.componentSelector
ACTIVATE PROPERTY formPropertyId

ACTIVATE [seekDirection] formObjectId = expr
ACTIVATE [seekDirection] formGroupObjectId [OBJECTS formObject1 = expr1, ..., formObjectK = exprK]
```

## Events

### Data events (WHEN)

An [event](../paradigm/Events.md) runs a given action — its *handler* — on a data change. The [event description block](../language/Event_description_block.md) says whether the event is global or local — for the whole database, or within a [change session](../paradigm/Change_sessions.md) — with `GLOBAL` and `LOCAL`, restricts it to given forms with `FORMS`, and orders its handler against others with `AFTER`.

Three statements create an event on a data change:

- [`WHEN`](../language/WHEN_statement.md) — a [simple event](../paradigm/Simple_event.md): the handler runs per object set on which the condition is not `NULL`;
- [`<- WHEN`](../language/lt-_WHEN_statement.md) — a [calculated event](../paradigm/Calculated_events.md): instead of a handler it gives a change of a data property;
- [`ON`](../language/ON_statement.md) — the general event: the handler runs once over all the changes.

```
WHEN eventClause eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;
propertyId(param1, ..., paramN) <- valueExpr WHEN eventExpr;
ON eventClause eventAction;
```

**Analogy**: a database trigger, except that the condition is an arbitrary property over the whole database.

```lsf
sum(OrderDetail d) <- quantity(d) * price(d) WHEN CHANGED(quantity(d)) OR CHANGED(price(d));
```

### Form events (ON)

[Form events](../paradigm/Form_events.md) occur on an open form: at a point in its life (`INIT`, `APPLY`, `CANCEL`, `CLOSE`, `DROP`), on what the user does, or on a timer with `SCHEDULE`. A handler is attached with the `ON` option — in the [event](../language/Event_block.md), [properties and actions](../language/Properties_and_actions_block.md) and [object](../language/Object_blocks.md#objects) blocks of the `FORM` statement, or in the [property options](../language/Property_options.md).

| What the event serves | Events |
| --------------------- | ------ |
| the form as a whole | `INIT`, `QUERYCLOSE`, `QUERYOK`, `OK`, `APPLY`, `CANCEL`, `CLOSE`, `DROP`, `SCHEDULE` |
| a form object | `CHANGE` |
| an object group | `FILTER`, `ORDER`, `SELECT`, `FILTERS`, `ORDERS` |
| a filter group | `FILTERGROUPS` |
| a property or an action | `CHANGE`, `CHANGEWYS`, `GROUPCHANGE`, `EDIT`, `CONTEXTMENU`, `KEYPRESS`, `FILTERS PROPERTY`, `SELECT PROPERTY` |
| a container | `EXPAND`, `COLLAPSE`, `TAB` |

The `BEFORE` and `AFTER` postfixes give the moments before and after the operation. For the property change events there are the predefined handlers `READONLY`, `READONLYIF` and `SELECTOR`.

```lsf
FORM sku 'Item'
    OBJECTS s = Sku
    PROPERTIES(s) price ON CHANGE changePrice(s)
    EVENTS ON INIT initSku()
;
```

### Order of execution

The handlers of local events run not at the moment the data changes but at certain moments in the life of the session — see [executing local events](../paradigm/Events.md#local). The handlers of synchronous global events run inside the transaction of [applying changes](../paradigm/Apply_changes_APPLY.md), together with the checks of [constraints](../paradigm/Constraints.md).

The order between the handlers reacting to the same change is determined by the data dependencies; it is set explicitly with the `AFTER` keyword (the synonym `GOAFTER`) in the [event description block](../language/Event_description_block.md).

## Constraints

### Constraints (CONSTRAINT)

A [constraint](../paradigm/Constraints.md) is a property whose value must always be `NULL`. It is checked on the event given by the [event description block](../language/Event_description_block.md) of the statement — by default, the global `APPLY` event; if by that moment it has become not `NULL` on at least one object set, the platform shows a message listing those sets and [cancels](../paradigm/Cancel_changes_CANCEL.md) the changes. It is created by the [`CONSTRAINT` statement](../language/CONSTRAINT_statement.md):

```
CONSTRAINT [eventClause] constraintExpr [CHECKED [BY propertyId1, ..., propertyIdN]] MESSAGE messageExpr
    [PROPERTIES outExpr1, ..., outExprM];
```

The `CHECKED BY` option makes the dialog that changes the listed properties filter out the values that would violate the constraint.

[Simple constraints](../paradigm/Simple_constraints.md) are set by the kind of relation between properties rather than by an expression, Two kinds are implemented: consequence — the [`=>` statement](../language/=gt_statement.md) — and definiteness — the [`NONULL` option](../language/Property_options.md). The consequence can resolve a violation itself: its `RESOLVE [LEFT] [RIGHT]` clause tells the platform which side to correct. `NONULL` has no such clause — it takes `[DELETE]`, which deletes the offending objects instead.

**Analogy**: `CHECK` and `NOT NULL` in SQL, except that the condition is an arbitrary property over the whole database.

```lsf
CONSTRAINT balance(Sku s, Stock st) < 0 MESSAGE 'Balance cannot be negative';
```

### Aggregated objects and constraints

An [aggregation](../paradigm/Aggregations.md) creates an *aggregated* object when the aggregated property becomes not `NULL` on a combination of parameter values, and deletes that object once the property becomes `NULL` again. Only the [`AGGR` operator](../language/AGGR_operator.md) creates and deletes the object; [`GROUP AGGR`](../language/GROUP_operator.md) builds the reverse property — from the parameter values back to the object. Both are described in [Brief: properties](Brief_logic.md#properties).

For integrity this is one more invariant the platform maintains on its own: at most one aggregated object per combination of parameter values. `GROUP AGGR` adds it as a constraint over its group, and `AGGR` keeps the correspondence between the objects and the parameter values within it. How that constraint is maintained is described in [aggregations](../paradigm/Aggregations.md) and [simple constraints](../paradigm/Simple_constraints.md).

```lsf
// a shipment is created for an invoice when the create-shipment flag is set, and deleted when it is dropped
shipment (Invoice i) = AGGR ShipmentInvoice WHERE createShipment(i);
```

## Change sessions

### What a session is

A [change session](../paradigm/Change_sessions.md) is where changes are accumulated locally instead of being written to the database right away. It holds changes of [data properties](../paradigm/Data_properties_DATA.md), including local ones, and changes of object classes — created and deleted objects, `CHANGECLASS`. Until the changes are applied they stay in this session; which of them another session can see is decided by the operator that opens it — `NESTEDSESSION` shows all of the upper session's changes, `NEWSESSION` reads from the database, carrying over the local properties its `NESTED` names — a list in brackets, or all of them with `NESTED LOCAL` — the class changes if `CLASSES` is given, and, listed or not, anything declared `DATA LOCAL NESTED` — together with how the properties themselves are declared, since a `DATA LOCAL NESTED` one travels on its own; an action's current session comes from the execution context — the form's session, the calling action's session, or the session supplied by the platform.

The value of a property at the start of the session is returned by the [`PREV` operator](../paradigm/Previous_value_PREV.md), and the [change operators](../paradigm/Change_operators_SET_CHANGED_etc.md) derived from it — `SET`, `DROPPED`, `CHANGED`, `SETCHANGED`, `DROPCHANGED`, `SETDROPPED` — answer what exactly changed in the session.

**Analogy**: an uncommitted database transaction that lives for as long as the user works with the form.

### NEWSESSION and NESTEDSESSION

The [`NEWSESSION` operator](../language/NEWSESSION_operator.md) runs the inner action in a separate [session](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md), isolated from the current one; the [`NESTEDSESSION` operator](../language/NESTEDSESSION_operator.md) runs it in a nested session, which copies the changes of the current session into itself and, on apply, copies them back. If either operator is executed during an [apply transaction](../paradigm/Apply_changes_APPLY.md) of the current session, no session is created at that moment — the inner action is deferred and executed in the current session inside the same transaction.

```
NEWSESSION [NEWSQL] [FORMS formId1, ..., formIdM] [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] action
NESTEDSESSION [SINGLE] action
```

The `NESTED` option lists the local properties to carry over, `NEWSQL` opens the session on a separate SQL connection. The two are mutually exclusive: written together they still parse, but `NEWSQL` wins and the whole `NESTED` clause is ignored.

A new session is reached for when the action is a unit of work of its own: a dialog, an import, file processing, a background record.

```lsf
logError (STRING message) {
    NEWSESSION NEWSQL {
        NEW e = LogEntry { text(e) <- message; }
        APPLY;
    }
}
```

### APPLY and CANCEL

The [`APPLY` operator](../language/APPLY_operator.md) [applies the changes](../paradigm/Apply_changes_APPLY.md) — writes what has been accumulated into the database, running the handlers of global [events](../paradigm/Events.md) and the checks of [constraints](../paradigm/Constraints.md) along the way. In a nested session it writes nothing to the database: the changes are copied back into the session the nested one is nested in. The [`CANCEL` operator](../language/CANCEL_operator.md) [cancels the changes](../paradigm/Cancel_changes_CANCEL.md) — clears the session, except during an apply transaction, where it cancels the running `APPLY` instead.

```
APPLY [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] [SERIALIZABLE] [action]
CANCEL [NESTED [nestedPropertySelector] [CLASSES]]
```

The outcome of an apply is read from the `System.canceled[]` and `System.applyMessage[]` properties.

**Analogy**: `COMMIT` and `ROLLBACK`.

### How changes are seen across sessions

Outside an apply transaction, what a new session sees is set by the operator that created it together with how the properties themselves are declared, between two ends: a session that inherits the upper session's uncommitted changes and one that ignores them and reads the committed state of the database. It is not a switch — `NESTED (...)` carries over the local properties it lists, and `CLASSES` carries over the class changes independently, so a session can inherit part of the state.

The operator is not the only place this is decided. A property declared `DATA LOCAL NESTED` carries over on its own, without being listed on the operator, and `MANAGESESSION` / `NOMANAGESESSION` on that declaration narrow it to `APPLY` / `CANCEL` or to `NEWSESSION` respectively ([`DATA`](../language/DATA_operator.md), [`NEWSESSION`](../language/NEWSESSION_operator.md)).

`NESTEDSESSION` is at one end — all changes of the upper session are visible. `NEWSESSION` is at the other — properties are read from the database, except those declared `DATA LOCAL NESTED`, which come along on their own — and its `NESTED` and `CLASSES` options widen that. `NEWSQL` narrows it instead: on a connection of its own nothing of the upper session comes along, declared `NESTED` or not.

Which combination gives what is described in [creating sessions](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md) and in the [`NEWSESSION` operator](../language/NEWSESSION_operator.md).
