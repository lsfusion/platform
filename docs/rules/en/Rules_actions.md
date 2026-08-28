---
slug: "/Rules_actions"
title: 'Rules: actions and assignment'
---

## Action rules

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

## Assignment rules (`<-`)

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

## Loop rules (`FOR`, `WHILE`)

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

## Thread rules (`NEWTHREAD`, `NEWEXECUTOR`)

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
