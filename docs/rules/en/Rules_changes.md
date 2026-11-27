---
slug: "/Rules_changes"
title: 'Rules: actions and data changes'
---

- [Actions and assignment](#actions-and-assignment)
- [Events (WHEN)](#events-when)
- [Constraints](#constraints)
- [Change sessions (NEWSESSION, APPLY)](#change-sessions-newsession-apply)

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
   outside of it. In particular it cannot serve as the loop
   variable of the enclosing `FOR`. Declare the variable as
   the `FOR`'s own parameter and use the aggregate only as
   a boolean condition over it. To iterate over the groups
   of an aggregate together with its value, apply the inline
   form to new typed parameters:
   `FOR NUMERIC[16,2] q = [GROUP SUM f(x) BY h(x)](Class y) DO ...`.

3. The assistant SHOULD avoid introducing `LOCAL`
   properties without a concrete need.

   A `LOCAL` materializes a temporary table in PostgreSQL
   only once it holds more than one row, so the runtime
   cost well above a stack variable in a conventional
   language applies to `LOCAL`s with parameters (buffers
   keyed by row number, per-object values). A parameterless
   `LOCAL` holds at most one row and always stays in
   memory, so parameterless flags and single values are
   cheap. Avoid them to keep the number of entities
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
   remain valid. The assistant SHOULD still keep such
   `LOCAL`s minimal in count and scope.

7. A parameter introduced locally by a top-level statement of
   an action body (the implicit loop of an assignment, `FOR`,
   `NEW`) is visible only inside that statement: the same name
   in the next statement is a new parameter with its own class.

   In generated scripts (`eval`, data seeding) the assistant
   SHOULD still give such parameters unique names, so that the
   class of each one is evident at the place of use.

8. Many system utility actions return their result through
   a same-named parameterless `LOCAL` property (for example,
   in `Utils`: the action `fileExists[ISTRING[500]]` writes
   into the property `fileExists[]`). Such an element is an ACTION,
   not a boolean property: the assistant MUST call the action
   first and then read the parameterless property
   (`fileExists(path); IF fileExists() THEN ...`), and MUST
   NOT use the parameterized form inside an expression
   (`IF fileExists(path)` is wrong).

### Abstract action rules (`ACTION+`)

1. The assistant MUST add an implementation to an abstract
   action with the `ACTION+` statement — `act(Dog d) + { ... }`,
   and `act(Dog d) + WHEN cond THEN { ... }` for the `CASE`
   type — never with `+=`, which extends properties only; see
   the [`ACTION+` statement](../language/ACTION_plus_statement.md).

2. The assistant SHOULD choose the type of an abstract action
   by its extension point: `LIST` when every implementation
   runs in sequence, `MULTI` when the argument classes select
   the implementation, `CASE` when an explicit condition does
   — see [action extension](../paradigm/Action_extension.md).

### Assignment rules (`<-`)

1. The arguments of the changed property on the left side
   of `<-` may be expressions over the statement's
   parameters (`sentFolder(account(f)) <- f`), but new
   local parameters can be introduced only as typed
   parameters, not inside expressions. Writing "into
   a computed key" by analogy with imperative
   `map[key] = value` easily breaks this.

   A key that is an expression over a new parameter is
   written from the loop that introduces that parameter:
   `FOR cond(Cls x) DO out(rowNum(x)) <- value(x);`
   (`rowNum[Cls]` computed beforehand, e.g. by `PARTITION SUM 1`).

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

5. A string that can exceed the length of the target
   property's class MUST be cut by an explicit cast to that
   class — `name(x) <- STRING[100](a(x) + b(x))` — or the
   property's class widened: a set-based assignment does not
   guarantee implicit cutting, and a longer value can fail with
   a database error at the assignment or, for a stored
   property, at `APPLY` — see
   [type conversion](../paradigm/Type_conversion.md).

### Loop rules (`FOR`, `WHILE`)

1. `FOR` fixes its set before the first iteration: the
   condition is evaluated once, the matching rows are read,
   and the body then runs once per row of that set. What the
   body changes — the data under the condition included —
   does not add or remove iterations.

   `WHILE` is the operator that re-reads, but it does so per
   STEP, not per row: one step re-evaluates the condition,
   reads the whole matching set and runs the body for every
   row of it, and only then is the set read again. Iteration
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

   Inside an `APPLY` transaction the wrapping creates no
   session (change-session rule 1), and whether the body is
   inside that transaction is decided at the moment the body
   runs, not at the moment it is scheduled. A `SCHEDULE DELAY`
   is a number of milliseconds, not a barrier waiting for the
   apply, so it guarantees nothing either. The assistant MUST
   NOT count on a thread started from a global handler being
   isolated.

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
   change to the target made in the same batch of changes.
   For a `LOCAL` event the protection is that narrow: change
   operators there count changes since the previous
   execution of local events (after every change on a form,
   when a form is opened, at the start of `APPLY`, when a
   nested session is created), and that reference point
   moves even when the handler did not fire. A target value
   set earlier — preset when the object was created before
   the form was opened, or entered in a previous form
   request — no longer counts as changed and is overwritten
   when the source changes. Only for a global `WHEN` is the
   reference point the database, so there the guard covers
   the whole session.

3. The guard SHOULD be omitted only when the event must
   forcibly override any explicit change — for example,
   maintained totals, audit stamps, or invariants
   the user is not allowed to bypass.

4. Rules 1-3 describe the event-action form
   `WHEN <condition> DO <target> <- <expr>`. The calculated
   event form `<target> <- <expr> WHEN <condition>` behaves
   differently: its change is calculated when the target
   property is accessed in a session that has changed a
   property its condition depends on (changes only to the
   event's value sources are insufficient to trigger
   recalculation), and an explicit change of that property
   in the session takes priority over the event's change.

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
   `NULL` and therefore fire on deletion. `SET` and
   `SETCHANGED` require the new value to be non-`NULL` and
   do not.

   Where the condition can fire on the way to `NULL`, and the
   handler must not act on a deletion or on an object leaving
   the class, it MUST be narrowed with `<object> IS <Class>`.

6. The platform does not wrap a condition containing `PREV`
   in `SET`: the event occurs only through the change operators
   used in it (in the condition or the handler, directly or
   through another property), and `PREV` itself does not
   trigger it. So if a `WHEN`, `WHEN LOCAL` or
   `CONSTRAINT` condition uses `PREV`, the assistant SHOULD
   include a change operator in the condition for the property
   whose changes should trigger it:
   `CONSTRAINT q(i) > PREV(m(i))` is never checked, while
   `CONSTRAINT CHANGED(q(i)) AND q(i) > PREV(m(i))` is checked
   when `q` changes.

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
   by the values of other properties (sibling fields of the
   form, the context, related objects), the assistant SHOULD
   first consider `CONSTRAINT ... CHECKED BY <property>`.

   `CHECKED BY` makes the change dialog for the listed
   property filter out the values that would violate the
   constraint, and refuses such a value passed by a custom
   component with `controller.change(value)`, so the
   restriction is enforced at selection. The filter is not
   applied to a value written in some other way: a violating
   one is rejected only when the constraint itself is checked.

2. Manual form filters or hand-rolled validation actions
   SHOULD be the fallback when `CHECKED BY` cannot express
   the restriction (e.g. the filter depends on UI state not
   modeled as a property, or the rule is advisory, not
   enforced) — and also when the value is written in a way
   the filter does not reach: by an assignment in an action,
   or by a change handler of the form that requests it with
   `INPUT` without the `CHANGE` block
   ([details](../language/CONSTRAINT_statement.md)).

3. The assistant SHOULD NOT put heavy aggregates over large
   tables (especially nested non-materialized ones) into a
   `CONSTRAINT` condition: the incremental check at apply
   can expand into an impractically large query, even with
   computation hints on the properties.

   For such expensive checks, use a `WHEN` event instead:
   a cheap change-detector condition, reads of the heavy
   values into `LOCAL`s in the handler, then `MESSAGE` +
   `CANCEL` on violation.

4. A `CONSTRAINT` MUST NOT validate the parameters an action
   reads from `LOCAL` properties — the settings of an
   integration library, for instance: with the default global
   event it is checked only when the session applies its changes
   to the database, after everything the action has done by
   then. Such parameters SHOULD be checked explicitly in the
   action before it acts
   (`IF NOT ok THEN { MESSAGE ...; RETURN; }`).

## Change sessions (NEWSESSION, APPLY)

1. Before introducing `NEWSESSION`, the assistant MUST decide
   which session behavior is required. None of the choices below
   — `NEWSQL` included — applies during an `APPLY` transaction:
   inside a global event handler or an applied action no session
   is created at all, the inner action is deferred and runs in
   the current session, inside the same transaction. The
   assistant MUST NOT expect an independent commit there.
   The deferred action runs only after the constraint checks
   of that pass, so the changes needed to keep a constraint of
   the same apply from being violated MUST be made by the handler
   directly, without deferring them through `NEWSESSION`.
   - isolated independent unit -> `NEWSESSION`
   - isolated unit that must also see selected local properties
     from the upper session -> `NEWSESSION NESTED (...)`
   - isolated unit that must see all local properties
     from the upper session -> `NEWSESSION NESTED LOCAL`
   - child dialog or editor that must work with unsaved upper-session
     objects and return its changes to that upper session
     -> `NESTEDSESSION`. The assistant MUST NOT replace it with
     plain `NEWSESSION` while the parent object may still be
     unsaved in the form session

2. Plain `NEWSESSION` is the default
   for isolated work that must not accidentally apply
   the caller's pending form changes:
   - readonly list forms with
     `PROPERTIES(...) NEWSESSION NEW, EDIT, DELETE`
   - external or integration actions that isolate HTTP calls
     and persist their own results
   - small immediate UI updates with
     `NEWSESSION { APPLY { ... } }`

   An action started on a form with editable properties MUST
   either be fully independent of that form's unsaved changes
   or save them first:
   `APPLY;`
   `IF canceled() THEN RETURN;`
   `NEWSESSION { ... }`
   This is the pattern before status changes, dependent
   document creation and other isolated follow-up actions.

3. If inner logic depends on upper-session local state
   such as selections, marks, or import buffers,
   the assistant MUST carry it explicitly
   through `NESTED (...)` or `NESTED LOCAL`
   on the operator, or declare the property itself
   `DATA LOCAL NESTED`, which needs no listing.
   A local not carried over is `NULL` in the new session
   until set: `f(picked()) <- v` silently changes nothing.
   Under `NEWSQL` inner logic MUST NOT depend on
   upper-session local state: on a separate connection
   nothing is migrated. A single value SHOULD be passed
   as an action or loop parameter, even with `NEWSQL`.
   The parameter needs no `NESTED`.

4. A successful `APPLY` clears the session, and with it
   every plain `LOCAL` property in it: after such an
   `APPLY` returns, the `LOCAL` is empty again. A `LOCAL`
   survives a successful `APPLY` only when it is declared
   `NESTED` (`LOCAL NESTED name = Type ();` or
   `name = DATA LOCAL NESTED Type (...);`) or when the
   `APPLY` preserves it explicitly —
   `APPLY NESTED (name1, ..., nameN)` or `APPLY NESTED LOCAL`
   for all locals. A staged value that must outlive `APPLY` —
   for example, an import buffer read during post-apply
   follow-up — MUST take one of these routes. So must the
   locals carried in by `NEWSESSION NESTED (...)` or
   `NEWSESSION NESTED LOCAL` when their result is to be copied
   back to the upper session, since it is the cleared values
   that would be copied back. So a form that keeps its page
   state — the chosen section, the search text — in local
   properties does not run an applying action under
   `NEWSESSION NESTED LOCAL`: it carries into the new session
   every local the action uses or changes — every local at
   all when the action can open an interactive form — and
   after a successful `APPLY` returns the plain ones cleared.
   Only the locals the action needs are listed instead:
   `NEWSESSION NESTED (a, b)`.

   An `APPLY` that fails or is cancelled leaves the session
   as it was, locals included — which is why the assistant
   MUST NOT read a `LOCAL` after `APPLY` to tell success
   from failure: `canceled()` is what tells them apart.
   Inside a nested session there is no clearing at all:
   the changes are copied to the parent session and the
   nested one is left standing, locals and all.

5. After `APPLY`, the assistant MUST check `canceled()`
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

6. The assistant SHOULD keep `NEWSESSION` blocks small
   and purpose-specific: isolate one unit of work,
   apply it if needed, and exit.

   The assistant MUST NOT introduce `NEWSESSION`
   merely to hide session-visibility bugs.
   If upper-session changes must remain visible,
   nested session semantics are required.

7. The body of `APPLY` may run more than once. The apply
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

   A call made before the `APPLY` that saves its result is
   exposed the same way: a constraint fires at that apply, not
   earlier, and cancels the saved result after the call has been
   made. What the constraints will check SHOULD be checked
   before the call, and a cancelled `APPLY` MUST NOT be answered
   by simply repeating the action, which repeats the call.
