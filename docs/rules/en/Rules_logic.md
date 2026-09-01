---
slug: "/Rules_logic"
title: 'Rules: domain logic'
---

- [Properties](#properties)
- [Actions and assignment](#actions-and-assignment)
- [Events (WHEN)](#events-when)
- [Constraints](#constraints)
- [Change sessions (NEWSESSION, APPLY)](#change-sessions-newsession-apply)

## Properties

### Property rules

1. The assistant MUST NOT declare a property
   if it is used only once.

   Exception:
   a property may still be declared if it is added to a form.

2. Every property parameter MUST be used in its expression.
   Unused parameters are forbidden.

3. The assistant MUST assume standard `NULL` propagation
   for property expressions:
   if any parameter is `NULL`, the result is `NULL`.

   Exceptions that do NOT nullify on a single `NULL` operand:
   the selection operators — `OVERRIDE`, which returns the first
   non-`NULL` operand, and `IF ... THEN ... ELSE`, whose
   CONDITION is the `NULL`-tolerant part: a `NULL` condition
   takes the `ELSE` branch, while a non-`NULL` one returns the
   `THEN` value as it is, so `IF TRUE THEN NULL ELSE 1` is
   `NULL` —
   `MIN` / `MAX`, the `NULL`-tolerant arithmetic `(+)` / `(-)`,
   the `CONCAT` concatenation
   (a `NULL` operand is skipped together with its separator),
   and `GROUP` aggregates (`GROUP SUM`, `GROUP MAX`, etc.) —
   a `NULL` operand or value is skipped instead of propagating.
   `OR`, `NOT` and `XOR` do not propagate either: they read a
   non-`NULL` operand as `TRUE`, so `NULL OR TRUE` is `TRUE`
   and `NOT NULL` is `TRUE`. `AND` is the one that does —
   it returns `TRUE` only when both operands are non-`NULL`,
   so `TRUE AND NULL` is `NULL`.
   `GROUP LAST` skips a `NULL` only while it has no `WHERE`,
   where non-`NULL`ness of the aggregated expression is what
   serves as the condition; given an explicit `WHERE`, a row
   satisfying it contributes its value even when that value
   is `NULL`.

   These exceptions still yield `NULL` when:
   - every operand or aggregated value is `NULL` — except
     `NOT`, whose whole point is to answer `TRUE` there;
   - `(+)` / `(-)` or `GROUP SUM` produces `0`
     (a zero result is returned as `NULL`).

4. The assistant MUST NOT use `GROUP` with a `BY` block
   (including `GROUP AGGR`) inside expressions:
   in a type cast, in arithmetic
   (including `(+)` / `(-)`), as an argument
   of another property, or as an implementation
   of an abstract property via `+=`.

   Such an operator defines the parameters
   of its result itself, so it is allowed only
   as an entire property definition:
   the right-hand side of a definition via `=`
   or an inline definition in square brackets;
   in any other position the platform raises
   the error `BY clause in GROUP operator
   cannot be used in expressions`.
   To use the result in an expression,
   the assistant SHOULD first rewrite the operator
   without `BY`, replacing each grouping
   with an equality condition on an outer parameter
   (`GROUP SUM f(x) IF g(x) = y`); otherwise,
   apply the inline form `[GROUP ... BY ...](...)`
   to arguments or declare a separate property
   and refer to it.

   The restriction is tied specifically
   to the `BY` block: `GROUP` without `BY`
   takes its parameters from the outer context
   and may be used inside expressions.

   When reasoning about `GROUP AGGR`, the assistant
   MUST treat it as `GROUP MAX`
   with an additional constraint.

5. The assistant SHOULD avoid unnecessary conditions
   when the language semantics already produce the required result.

6. The assistant MUST NOT create a property whose expression
   is equal to one of its parameters.

7. The assistant MUST NOT create multiple properties
   with identical expressions.

8. If a property is calculated from another property
   but has different parameters, the assistant SHOULD try
   to keep the same property name.

9. To check whether a property is `NULL`,
   the assistant SHOULD use `IF NOT property(...)`.

   To check that it is not `NULL`,
   the assistant SHOULD use `IF property(...)`.

10. The assistant SHOULD specify `CHARWIDTH`
   in the property definition rather than in form design.

   For a simple property composition that only forwards
   another property, the assistant SHOULD NOT repeat
   `CHARWIDTH` on the derived property unless it must differ.

11. For static objects, the assistant MUST NOT use
    `staticCaption` or `staticName` properties.

    The assistant MUST use `caption` and `name` instead.

    This applies to writing as well: `caption` and `name` are
    simple compositions over the stored caption and name, so an
    assignment to them passes into the stored property.
    A static object's caption is assigned through `caption`;
    the name must not be changed — changing `name` is
    forbidden by a system constraint.

    `name` returns the static object's canonical name —
    `<namespace>_<Class>.<object>`, not the short identifier.
    When the part after the dot is needed, the assistant
    SHOULD use `basicName` from the `Utils` system module.

12. Property names SHOULD be concise
    and avoid unnecessary words.

13. The assistant SHOULD NOT use words in a property name
    that duplicate parameter class names
    unless required for clarity.

14. The assistant SHOULD NOT specify an explicit namespace
    for a property unless necessary.

15. When creating a DATA property — or a simple composition
    over a DATA property (for example, pulling the name of a
    related object) — for a single object's own attribute,
    the assistant MUST deliberately decide
    whether to place it in the system `id` or `base` group
    via `IN`.

    Attributes that form the object's business identity
    and appear in its representation SHOULD go in the `id`
    group; other primary attributes go in the `base` group
    (`id` is nested under `base`).

    A property SHOULD NOT be placed in `id` or `base`
    when it is not the object's own primary attribute.

16. When dividing values of integer classes, the assistant MUST
    cast one of the operands to `NUMERIC`, not the result.

    The ratio of two integers is integer division,
    so an outer cast like `NUMERIC[16,4](a * b / c)`
    silently drops the fractional part;
    the correct form is `NUMERIC[16,4](a) * b / c`.

17. The class of an expression's result can be wider than
    the classes it is built from, and the assistant MUST
    account for that wherever a narrower class is required —
    above all in a `+=` implementation, where it is
    a server startup error.

    Arithmetic widens further than it looks:

    - `+` and `-` — like `MIN` / `MAX` and the selection
      operators — take the common ancestor, widening the
      whole part and the scale independently, so the result
      can be wider than either operand:
      `NUMERIC[16,2] + NUMERIC[10,4]` is `NUMERIC[18,4]`;
    - `*` adds both the whole parts and the scales:
      `NUMERIC[16,2] * NUMERIC[10,4]` is `NUMERIC[26,6]`;
    - `/` widens catastrophically: with the default settings
      its scale is always the maximum `NUMERIC` scale (`32`),
      so `NUMERIC[16,2] / NUMERIC[16,2]` is `NUMERIC[48,32]`.

    A `GROUP` aggregate mostly keeps the class of what it
    aggregates — a `GROUP SUM`, `GROUP MAX` or `GROUP LAST`
    over a `NUMERIC[16,2]` is `NUMERIC[16,2]` — but it
    carries outward whatever that expression already widened
    to. `GROUP CONCAT` is the aggregate that widens by
    itself: its result is a string of unlimited length.
    String concatenation widens as well, summing the
    operands' lengths: `ISTRING[200] + ISTRING[126]`
    is `ISTRING[326]`.

    A narrower class is obtained only by an explicit cast of
    the whole expression. With operands of integer classes
    the operand cast of rule 16 does not bound the result —
    the division still widens to scale `32` — so both casts
    are needed: `NUMERIC[16,2](NUMERIC[16,2](a(x)) / b(x))`.

18. A parameter's class annotation (`prop(SubClass x)`) is a
    signature, not a runtime filter: it resolves same-named
    properties and sets the signature, but the computed set is
    determined by the properties used in the expression.
    Reading a parent-class property with a subclass-annotated
    parameter still ranges over ALL objects of the parent class
    (e.g. in a `GROUP SUM` — silently wrong totals).

    To restrict the set to a class, the assistant MUST add
    an explicit `x IS SubClass` condition (or use a property
    declared on that subclass).

19. In the `GROUP ... BY` operator the assistant MUST NOT
    list in the `BY` block the upper parameters used
    in the operator's expressions: each such parameter
    is already implicitly a group — a parameter of the
    created property — and keeps its place in the signature.

    With an explicit parameter list on the left,
    the `BY` expressions are mapped in order only
    to the parameters not used in the expressions;
    a mismatch in count or classes is an error.

20. `MAX` and `MIN` are prefix operators over a comma-separated
    operand list (`MAX a, b`), not infix ones: `a MAX b`
    does not parse — the platform reports
    `no viable alternative at input 'MAX'`.

    The operand list extends as far as the expression allows,
    so everything after the comma belongs to the operator:
    `MAX a, b * c` is `MAX(a, b * c)`, while `x * MAX a, b`
    is fine as it stands. Where a following operator must
    apply to the maximum itself, the operator MUST be
    parenthesized: `(MAX a, b) * c`.

    These operators compare the operands of a single row;
    a maximum across rows is `GROUP MAX`.

### Abstract property rules (`+=`)

1. The value class of a `+=` implementation MUST fit within
   the value class declared on the abstract property; there
   is no implicit cast — an implementation with a wider
   class is rejected at server startup with a
   "wrong value class of implementation" error, whose
   `specified` and `expected` lines name the implementation's
   class and the declared one.

   An expression that widens the value class — arithmetic
   above all, and division most of all (rule 17 of the
   property rules) — the assistant MUST wrap in an explicit
   cast to the declared class:
   `f(X x) += NUMERIC[16,2](a(x) / b(x));`
   `f(X x) += ISTRING[250](a(x) + b(x));`

### Ordering rules (`ORDER`)

1. Where two rows can share an order key and the answer depends
   on which of them wins — which of two same-date rows is the
   `GROUP LAST`, which of two equal-priority rows a `TOP 1`
   takes, where a `PARTITION PREV` steps back to — the
   assistant MUST spell the tiebreak out, usually as the object
   itself: `ORDER date(d), d`.

   The platform does fill an incomplete order in on its own for
   several of these, so the symptom is not randomness between
   runs; it is that the row chosen is whichever one a service
   order over the interfaces selects, which is not what the
   domain asked for. Writing the tiebreak is how the choice
   becomes the intended one.

2. A cumulative `PARTITION SUM ... ORDER` with no `TOP` or
   `OFFSET` is the case where a tiebreak MUST NOT be added by
   reflex. Its default frame gives every row sharing an order
   key the same cumulative value. Adding a tiebreak changes the
   result — from a total per group of equal keys to a total per
   row — which is a decision about the domain, not a safety
   measure. Under `TOP` or `OFFSET` rule 1 applies as usual:
   those pick rows, and which rows they pick is worth saying.

3. `PARTITION LAST` does not read the order to compute its
   value: it is the value of the current row. `GROUP LAST` is
   the one that picks by order.

## Actions and assignment

### Action rules

1. The assistant MUST avoid `FOR` when the same result
   can be expressed with a set-based construct.

   `FOR` iterates row by row and SHOULD be the last resort
   when no declarative alternative exists.
   The one measured exception runs the other way: when the
   assigned value is a `GROUP` aggregate whose bounds correlate
   with the row being updated, and both sets are large, the
   set-based form can compile to a query that materializes the
   whole correlation, and a row-by-row `FOR ... NOINLINE` can
   be the faster one — where an index on the aggregated class
   lets each row's aggregate be answered by an index lookup.

   Prefer set-based alternatives, for example:
   - aggregation or set materialization
     -> `GROUP SUM`, `GROUP CONCAT`, `GROUP MAX`,
        `GROUP LAST`, `GROUP AGGR`
   - assigning a property over a set
     -> direct property assignment with parameters
        instead of a `FOR ... DO` loop
   - exporting tabular or hierarchical data
     -> `EXPORT FROM`, `EXPORT JSON FROM`,
        `EXPORT XML FROM`, `EXPORT CSV FROM`
   - building structured payloads
     -> `JSON FROM`, `XML FROM`
   - bulk integration writes
     -> `NEW`, `DELETE`, or set-based property change
        instead of a per-row `FOR`

   `FOR` is acceptable when the body has genuine
   per-row control flow such as conditional `APPLY`,
   `MESSAGE`, `throwException`, or external calls
   that cannot be expressed as a set operation.

2. Parameters introduced in `NEW alias = Class`
   and `FOR expr(p) [NEW alias = Class] DO { ... }`
   do NOT follow the usual lexical scoping rules
   of mainstream programming languages.

   Such parameters are visible ONLY inside the body
   of the `NEW` block or the `FOR` loop that introduces them.

   The assistant MUST NOT reference these parameters
   outside their introducing block.

   When dependent computation must reuse these parameters,
   the assistant SHOULD nest further `NEW` or `FOR` blocks
   inside the introducing block, where the parameters
   are still in scope, rather than lifting values out
   into auxiliary storage.

   Conversely, a parameter declared inside a `GROUP`
   aggregate belongs to that aggregate and is NOT visible
   outside of it; in particular it cannot serve as the loop
   variable of the enclosing `FOR`. Declare the variable as
   the `FOR`'s own parameter and use the aggregate only as
   a boolean condition over it.

3. The assistant SHOULD avoid introducing `LOCAL`
   properties without a concrete need.

   A `LOCAL` materializes a temporary table in PostgreSQL
   only once it holds more than one row, so the runtime
   cost well above a stack variable in a conventional
   language applies to `LOCAL`s with parameters (buffers
   keyed by row number, per-object values). A parameterless
   `LOCAL` holds at most one row and always stays in
   memory, so parameterless flags and single values are
   cheap; avoid them to keep the number of entities
   down, not because of cost.

4. A `LOCAL` is normally justified when BOTH conditions hold:
   - its value is non-trivial to compute
     (aggregation, joins, multi-step logic, external calls,
     or other work worth materializing), AND
   - the same value is consumed more than once,
     so materializing it avoids recomputation.

5. When possible, the assistant SHOULD prefer alternatives
   to a fresh `LOCAL`:
   - inline the expression at each use site if it is cheap
   - nest `NEW` / `FOR` blocks so intermediate values stay
     in parameter scope
   - use a regular (non-`LOCAL`) calculated property
     when the value is reusable across actions

6. These are recommendations, not hard prohibitions.
   If the assistant cannot find a working syntax for
   a `LOCAL`-free construction, or some other approach
   keeps failing and a clean action cannot be built,
   falling back to a `LOCAL` is acceptable as a last resort.

   Established `LOCAL` patterns mandated by other rules
   (e.g. import staging, nested-session carry-over)
   remain valid; the assistant SHOULD still keep such
   `LOCAL`s minimal in count and scope.

7. The parameters of the top-level statements of an action
   body share one parameter context: identical names denote
   the same parameter, and a parameter's class is declared
   only at its first use.

   In generated scripts (`eval`, data seeding) the assistant
   SHOULD give the parameters of top-level statements unique
   names, so as not to depend on the statement order.

8. Many system utility actions return their result through
   a same-named parameterless `LOCAL` property (for example,
   in `Utils`: the action `fileExists[ISTRING[500]]` writes
   into the property `fileExists[]`). Such an element is an ACTION,
   not a boolean property: the assistant MUST call the action
   first and then read the parameterless property
   (`fileExists(path); IF fileExists() THEN ...`), and MUST
   NOT use the parameterized form inside an expression
   (`IF fileExists(path)` is wrong).

### Assignment rules (`<-`)

1. The arguments of the changed property on the left side
   of `<-` may be expressions over the statement's
   parameters (`sentFolder(account(f)) <- f`), but new
   local parameters can be introduced only as typed
   parameters, not inside expressions. Writing "into
   a computed key" by analogy with imperative
   `map[key] = value` easily breaks this.

   So when remapping self-referential links while
   deep-copying an object graph, the assistant SHOULD keep
   an inverse map and iterate with the TARGET object as the
   parameter —
   `link(Copy n) <- newOf(link(srcOf(n))) WHERE spec(n);` —
   rather than write `link(newOf(x)) <- newOf(link(x));`

2. `<- expr IF cond` assigns the whole expression to ALL
   objects: where `cond` fails, the property is overwritten
   with `NULL`. It is effectively reset-plus-set.

   When ADDING an assignment to a property already populated
   earlier in the same action, the assistant MUST use the
   `WHERE` form (`prop(x) <- TRUE WHERE cond(x)`), which
   changes only the rows matching the condition. A second
   IF-form assignment to the same property MUST be treated
   as a review red flag.

3. Inline in an action or event body, `PREV(<expr>)` takes
   the WHOLE wrapped expression to the session-start state,
   including its argument sub-expressions: an argument
   computed in the current session (a `LOCAL`, a property
   of an object created in the session) reads as `NULL`
   inside `PREV`, silently nulling the result.

   To read previous data with current arguments, the
   assistant MUST wrap `PREV` in a separate property —
   `prevF(x) = PREV(f(x));` — and call it instead of
   writing `PREV(f(<session-computed arg>))` inline.

4. A parameter through which a property whose name is
   declared on several classes is read or changed MUST be
   annotated with its class at first use
   (`date(Interaction i) <- ...`): an overloaded name is
   resolved by the parameter classes, and an untyped
   parameter yields an "ambiguous name" error. This
   especially concerns events: their statement is a separate
   parameter context in which the class is not inferred from
   anywhere else, and an `i IS Interaction` condition does
   not set the parameter's class.

### Loop rules (`FOR`, `WHILE`)

1. `FOR` fixes its set before the first iteration: the
   condition is evaluated once, the matching rows are read,
   and the body then runs once per row of that set. What the
   body changes — the data under the condition included —
   does not add or remove iterations.

   `WHILE` is the operator that re-reads, but it does so per
   STEP, not per row: one step re-evaluates the condition,
   reads the whole matching set and runs the body for every
   row of it, and only then is the set read again; iteration
   stops when it comes back empty. So a row already in the
   current step still gets its turn even if an earlier row of
   that same step has made the condition false for it.

2. Without `ORDER` a `FOR` walks its set in arbitrary order.
   The assistant MUST give an explicit `ORDER` whenever the
   result depends on the sequence — numbering, running totals,
   anything reading what an earlier iteration wrote — or
   whenever `TOP` limits how many rows are taken, and MUST
   end that `ORDER` with a key that separates any two rows.

### Thread rules (`NEWTHREAD`, `NEWEXECUTOR`)

1. A server-side thread action shares the change session of
   the calling code, and change sessions are not thread-safe.

   The assistant SHOULD therefore wrap the body of a
   server-side `NEWTHREAD` in `NEWSESSION`, and in
   `NEWSESSION NEWSQL` when it needs a database transaction of
   its own. It is a trade: a plain `NEWSESSION` no longer sees
   the caller's unsaved changes, so the wrapping is left out
   only where sharing the session is deliberate AND the two
   are known not to run at the same time.

   What the wrapping buys inside an `APPLY` transaction
   depends on WHEN the body actually starts. `NEWSESSION`,
   `NEWSQL` included, creates no session while the transaction
   is still open — the action is deferred into the current one
   — and the check happens at the moment the body runs, not at
   the moment it is scheduled. A `SCHEDULE DELAY` is a number
   of milliseconds, not a barrier waiting for the apply, so it
   guarantees nothing either. The assistant MUST NOT count on
   a thread started from a global handler being isolated.

   A client executor is the opposite case: the action is
   delivered to the user's connection and runs there in its
   own fresh session, so wrapping it adds nothing.

## Events (WHEN)

### Event rules (`WHEN`)

1. A `WHEN` event fires whenever its condition becomes true
   during a session and writes its target property
   unconditionally. If the same target property is also
   changed explicitly elsewhere in the session
   (user input, action assignment, import),
   the event overwrites that explicit change.

2. When the event's purpose is only to derive or default
   a value from other inputs, the assistant SHOULD guard
   the condition with `AND NOT CHANGED(<target>)`
   for each target property the event writes.

   This prevents the event from clobbering an explicit
   change to the target made elsewhere in the same session.

3. The guard SHOULD be omitted only when the event must
   forcibly override any explicit change — for example,
   maintained totals, audit stamps, or invariants
   the user is not allowed to bypass.

4. Rules 1-3 describe the event-action form
   `WHEN <condition> DO <target> <- <expr>`. The calculated
   event form `<target> <- <expr> WHEN <condition>` behaves
   differently: its change is calculated when the target
   property is accessed, and an explicit change of that
   property in the session takes priority over the event's
   change.

   So to default a value while yielding to an explicit
   change, the calculated event form alone is enough —
   no guard is needed. Testing `CHANGED(<target>)` in its
   condition is not possible in any case: the target would
   then depend on its own change, forming a cycle
   `<target>` -> `CHANGED(<target>)` -> `<target>`.

   In the absence of an explicit change the event writes
   the value of the expression even when it is `NULL`.

5. A `WHEN` condition is checked on deleted objects too.
   Deleting an object resets its data properties to `NULL`,
   so a condition that reacts to a value becoming `NULL` is
   satisfied for every deleted object whose value had been
   non-`NULL`, and the handler runs on the object that is
   already gone.

   Which change operators those are is decided by the
   transition each of them covers: `DROPPED`, `CHANGED`,
   `DROPCHANGED` and `SETDROPPED` include non-`NULL` to
   `NULL` and therefore fire on deletion; `SET` and
   `SETCHANGED` require the new value to be non-`NULL` and
   do not.

   Where the condition can fire on the way to `NULL`, and the
   handler must not act on a deletion or on an object leaving
   the class, it MUST be narrowed with `<object> IS <Class>`.

### Where local events actually run

1. A local event handler does not run at the moment the data
   changes. It runs at a point in the session's life: a form
   synchronising, a form opening, an `APPLY` starting, a
   nested session being created, or an explicit
   `System.executeLocalEvents[]`.

   Outside an interactive form — an action called from an
   external system, a scheduler task — the only one of those
   that normally happens is the apply. So reading a property
   right after changing the data it depends on returns the
   value WITHOUT the local handlers applied, unlike a
   calculated property, which is always current.

   The assistant MUST NOT rely on a local handler having run
   in such a place: either let `APPLY` do it, or call
   `System.executeLocalEvents[]` before the read.

## Constraints

1. When a value choice in one property must be restricted
   based on values of other properties — sibling fields
   on the same form, current context, related objects —
   the assistant SHOULD first consider
   `CONSTRAINT ... CHECKED BY <property>`.

   `CHECKED BY` makes the change dialog for the listed
   property automatically filter out values that would
   violate the constraint, so the restriction is enforced
   declaratively at the point of selection, not after
   the fact.

   The filter reaches those change dialogs only. An input
   mechanism that offers values in some other way does not
   use it, and there a violating value is rejected only when
   the constraint itself is checked.

2. Manual form filters or hand-rolled validation actions
   SHOULD be the fallback when `CHECKED BY` cannot express
   the restriction (e.g. the filter depends on transient UI
   state not modeled as a property, or the rule is advisory
   rather than enforced) — and also when the restriction IS
   expressible that way but the value is picked through some
   other mechanism, which the `CHECKED BY` filter does not
   reach.

3. The assistant SHOULD NOT put heavy aggregates over large
   tables (especially nested non-materialized ones) into a
   `CONSTRAINT` condition: the incremental check at apply
   can expand into an impractically large query, even with
   computation hints on the properties.

   For such expensive checks, use a `WHEN` event instead:
   a cheap change-detector condition, reads of the heavy
   values into `LOCAL`s in the handler, then `MESSAGE` +
   `CANCEL` on violation.

## Change sessions (NEWSESSION, APPLY)

1. Before introducing `NEWSESSION`, the assistant MUST decide
   which session behavior is required. None of the choices below
   applies during an `APPLY` transaction — inside a global event
   handler or an applied action — where no session is created at
   all: the inner action is deferred and runs in the current
   session, inside the same transaction. The assistant MUST NOT
   expect an independent commit there.
   - isolated independent unit -> `NEWSESSION`
   - isolated unit that must also see selected local properties
     from the upper session -> `NEWSESSION NESTED (...)`
   - isolated unit that must see all local properties
     from the upper session -> `NEWSESSION NESTED LOCAL`
   - child dialog or editor that must work with unsaved upper-session
     objects and return its changes to that upper session
     -> `NESTEDSESSION`

2. For actions added to forms,
   there are two main patterns:

   - readonly form pattern:
     the form is effectively browse-only, so actions added to it
     SHOULD run in a new session by default
   - editable form pattern:
     the form has editable properties, so any action added to it
     that uses `NEWSESSION` MUST either:
     `APPLY;`
     `IF canceled() THEN RETURN;`
     before `NEWSESSION`, or be fully independent
     from unsaved changes in that form

3. Plain `NEWSESSION` is the default
   for isolated work that must not accidentally apply
   the caller's pending form changes.

   Typical patterns in the source:
   - readonly list forms with
     `PROPERTIES(...) NEWSESSION NEW, EDIT, DELETE`
   - status transitions or dependent document creation
     after a preceding `APPLY`
   - external or integration actions that isolate HTTP calls
     and persist their own results
   - small immediate UI updates with
     `NEWSESSION { APPLY { ... } }`

4. If inner logic depends on upper-session local state
   such as selections, marks, or import buffers,
   the assistant MUST carry that state explicitly
   through `NESTED (...)` or `NESTED LOCAL`
   on the operator, or declare the property itself
   `DATA LOCAL NESTED`, which carries it over
   without being listed on the operator.
   Neither route works under `NEWSQL`: on a connection
   of its own it migrates nothing, so the assistant
   MUST NOT combine `NEWSQL` with a dependency on
   upper-session local state.

5. A successful `APPLY` clears the session, and with it
   every `LOCAL` property in it by default: after such an
   `APPLY` returns, a plain `LOCAL` is empty again. An
   `APPLY` that fails or is cancelled leaves the session
   as it was, locals included — which is why the assistant
   MUST NOT read a `LOCAL` after `APPLY` to tell success
   from failure; `canceled()` is what tells them apart.
   Inside a nested session there is no clearing at all:
   the changes are copied to the parent session and the
   nested one is left standing, locals and all.

   Outside a nested session, a `LOCAL` value survives
   a SUCCESSFUL `APPLY` when EITHER:
   - the `LOCAL` is declared as `NESTED` at declaration
     time (`LOCAL NESTED name = Type ();` or
     `name = DATA LOCAL NESTED Type (...);`), OR
   - the `APPLY` explicitly preserves it via
     `APPLY NESTED (name1, ..., nameN)`
     or `APPLY NESTED LOCAL` for all locals.

   The assistant MUST NOT rely on a plain `LOCAL` value
   computed before a SUCCESSFUL `APPLY` to still be readable
   after it. Two cases keep it: a nested session, which clears
   nothing at all, and an apply that failed or was cancelled,
   which leaves the session as it was.
   If a staged value must outlive `APPLY` — for example,
   an import buffer read during post-apply follow-up —
   the assistant MUST either declare it with `NESTED`,
   or list it in `APPLY NESTED (...)` (or use
   `APPLY NESTED LOCAL`) at the call site.

6. When using `NEWSESSION NESTED (...)` or
   `NEWSESSION NESTED LOCAL`, the assistant SHOULD preserve
   the same nested local properties on `APPLY`
   if the result must be copied back to the upper session,
   for example with `APPLY NESTED (...)`
   or `APPLY NESTED LOCAL`.

7. The assistant MUST NOT replace `NESTEDSESSION`
   with plain `NEWSESSION` for child forms or dialogs
   attached to a parent object that may still be unsaved
   in the current form session.

8. Before opening a fresh `NEWSESSION` from an action
   started on an edit form, the assistant SHOULD decide
   whether current form changes must be saved first.

   The common pattern is:
   `APPLY;`
   `IF canceled() THEN RETURN;`
   `NEWSESSION { ... }`

   This pattern is used before status changes,
   document generation, and other isolated follow-up actions.

9. After `APPLY`, the assistant MUST check `canceled()`
   only when later logic depends on whether the save
   succeeded — to early-return, skip a follow-up side
   effect, or roll back staged work.

   `APPLY` in an interactive context shows the constraint
   message to the user on its own. The assistant MUST NOT
   add `IF canceled() THEN MESSAGE applyMessage()` after
   `APPLY` in interactive actions solely to report the
   failure — it duplicates the message the platform
   already shows. Explicit surfacing via `applyMessage()`
   or `throwException(applyMessage())` is required only
   for non-interactive callers (API endpoints, background
   integrations) where no dialog is shown.

   If `APPLY` fails because of a constraint, the changes
   remain unsaved in the current session, and any following
   `APPLY` in the same session will also fail until the
   offending data is fixed or the changes are discarded
   (for example with `CANCEL`).

10. The assistant SHOULD keep `NEWSESSION` blocks small
    and purpose-specific: isolate one unit of work,
    apply it if needed, and exit.

    The assistant MUST NOT introduce `NEWSESSION`
    merely to hide session-visibility bugs.
    If upper-session changes must remain visible,
    nested session semantics are required.

11. The body of `APPLY` may run more than once. The apply
    transaction MAY be retried automatically after an update
    conflict, a deadlock or a timeout — whether it is depends
    on the failure and on the attempt limit — and the applied
    action and the synchronous global handlers are inside what
    a retry repeats.

    So they MUST be safe to repeat. An irreversible external
    side effect — sending mail, calling an HTTP API, printing,
    writing a file — MUST NOT be done there: it belongs after
    the apply has succeeded, where `canceled()` says whether
    it did.
