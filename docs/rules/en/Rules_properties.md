---
slug: "/Rules_properties"
title: 'Rules: properties'
---

- [Properties](#properties)

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
   if any operand is `NULL`, the result is `NULL`.
   A `NULL` argument acts only through the operands that use
   it: with a `DATA` property `a`, `OVERRIDE a(x, d), b(x)` is
   `b(x)` for a `NULL` date `d`. To get `NULL`, use
   `(OVERRIDE a(x, d), b(x)) IF d IS DATE`.
   In `GROUP`, however, an upper parameter `d` in an aggregate
   operand is a grouping: `NULL` has no group, even with
   `NOT d OR ...`. A value for that case MUST be selected
   outside `GROUP` (`IF d THEN ... ELSE ...`), see
   [composition](../paradigm/Composition_JOIN.md#null).

   Exceptions that do NOT nullify on one `NULL` operand:
   the selection operators — `OVERRIDE` (returns the first
   non-`NULL` operand) and `IF ... THEN ... ELSE`, which
   returns the selected value as is (`ELSE` for a `NULL`
   condition, `THEN` otherwise), so
   `IF TRUE THEN NULL ELSE 1` is `NULL` —
   `MIN` / `MAX`, the arithmetic `(+)` / `(-)`
   (numeric only: it does not concatenate strings),
   `CONCAT` (concatenation skips `NULL` with its separator),
   and `GROUP` aggregates (`GROUP SUM`, `GROUP MAX`, etc.) —
   a `NULL` operand or value is skipped.
   `OR`, `NOT` and `XOR` do not propagate either: a non-`NULL`
   operand reads as `TRUE`, so `NULL OR TRUE` and `NOT NULL`
   are `TRUE`. `AND` does: it is `TRUE` only when both
   operands are non-`NULL`, so `TRUE AND NULL` is `NULL`.
   `GROUP LAST` skips a `NULL` only without `WHERE`, when
   non-`NULL`ness of the aggregated expression is the
   condition. With `WHERE`, each matching row contributes its
   value, including `NULL`.

   These exceptions still yield `NULL` when:
   - every operand or aggregated value is `NULL` — except
     `NOT`, which answers `TRUE` there;
   - `(+)` / `(-)` or `GROUP SUM` produces `0`
     (zero is returned as `NULL`).

   A custom formula (`FORMULA`) runs as written. The platform
   does not check it against its form's rules.
   Without the trailing `NULL` the SQL
   text MUST return `NULL` when any argument is `NULL`: an
   SQL function returning a value for `NULL` input (`concat`,
   `COALESCE`, `string_to_array` with a `NULL` delimiter) needs
   an explicit check (`CASE WHEN $2 IS NULL THEN NULL
   ELSE ... END`). With the trailing `NULL` the SQL text MUST
   return a non-`NULL` value when any argument is
   non-`NULL`, see
   [custom formula](../paradigm/Custom_formula_FORMULA.md#null).

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
   (`GROUP SUM f(x) IF g(x) = y`). Otherwise,
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

8. To check whether a property is `NULL`,
   the assistant SHOULD use `IF NOT property(...)`.

   To check that it is not `NULL`,
   the assistant SHOULD use `IF property(...)`.

   This applies to a property of any class, not only
   `BOOLEAN`: a condition holds when its value is not `NULL`,
   whatever that value is —
   `hasNotes(o) = IF notes(o) THEN TRUE` for a `STRING`,
   `isScheduled(o) = IF scheduledAt(o) THEN TRUE` for a
   `DATETIME`, `hasDepartment(e) = IF department(e) THEN TRUE`
   for an object-valued property,
   `NOT minSum() OR sum(o) >= minSum()`
   for a bound that passes everything while unset (`NOT` and
   `OR` do not propagate `NULL`, rule 3).

   The assistant MUST NOT compare with the `NULL` literal:
   `expr = NULL`, `expr == NULL` and `expr != NULL` are
   ordinary comparisons, and by rule 3 the `NULL` operand makes
   the result `NULL` whatever `expr` holds —
   `scheduledAt(o) != NULL` is never `TRUE`, so a `FILTER` or
   `WHERE` on it selects nothing, a `SHOWIF` on it always hides,
   an `IF` on it always takes the `ELSE` branch — and the server
   only reports a warning when it loads the module, the code still
   compiles and starts.

9. The assistant SHOULD specify `CHARWIDTH`
   in the property definition rather than in form design.

   For a simple property composition that only forwards
   another property, the assistant SHOULD NOT repeat
   `CHARWIDTH` on the derived property unless it must differ.

10. For static objects, the assistant MUST NOT use
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

11. The assistant SHOULD NOT specify an explicit namespace
    for a property unless necessary.

12. When creating a DATA property — or a simple composition
    over a DATA property (for example, pulling the name of a
    related object) — for a single object's own attribute,
    the assistant MUST deliberately decide
    whether to place it in the system `id` or `base` group
    via `IN`.

    Attributes that form the object's business identity
    and appear in its representation SHOULD go in the `id`
    group, and other primary attributes go in the `base` group
    (`id` is nested under `base`).

    A property SHOULD NOT be placed in `id` or `base`
    when it is not the object's own primary attribute.

13. When dividing values of integer classes, the assistant MUST
    cast one of the operands to `NUMERIC`, not the result.

    The ratio of two integers is integer division,
    so an outer cast like `NUMERIC[16,4](a * b / c)`
    silently drops the fractional part;
    the correct form is `NUMERIC[16,4](a) * b / c`.

14. The class of an expression's result can be wider than
    the classes it is built from, and the assistant MUST
    account for that wherever a narrower class is required
    (a `+=` implementation above all: the abstract property
    rules).

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
    the operand cast of rule 13 does not bound the result —
    the division still widens to scale `32` — so both casts
    are needed: `NUMERIC[16,2](NUMERIC[16,2](a(x)) / b(x))`.

    For a `GROUP SUM` over `NUMERIC` whose total can exceed
    the class of its operand, the assistant MUST widen the
    operand inside the aggregate
    (`GROUP SUM NUMERIC[16,2](x)`), not cast the result: the
    precision of the class is enforced only where the sum is
    materialized into a column of that class, and the
    materialization of a subquery happens intermittently,
    so the overflow can escape a single run, even on a cold
    server.

15. A parameter's class annotation (`prop(SubClass x)`) is a
    signature, not a runtime filter: it resolves same-named
    properties and sets the signature, but the computed set is
    determined by the properties used in the expression.
    Reading a parent-class property with a subclass-annotated
    parameter still ranges over ALL objects of the parent class
    (e.g. in a `GROUP SUM` — silently wrong totals).

    To restrict the set to a class, the assistant MUST add
    an explicit `x IS SubClass` condition (or use a property
    declared on that subclass).

16. In the `GROUP ... BY` operator the assistant MUST NOT
    list in the `BY` block the upper parameters used
    in the operator's expressions: each such parameter
    is already implicitly a group — a parameter of the
    created property — and keeps its place in the signature.

    With an explicit parameter list on the left,
    the `BY` expressions are mapped in order only
    to the parameters not used in the expressions;
    a mismatch in count or classes is an error.

    In the inline form `[GROUP ... BY ...](...)` such parameters
    are passed automatically: the arguments correspond in order
    only to the `BY` expressions
    (`[GROUP SUM f(x) IF g(x) = s BY h(x)](y)`), and listing `s`
    among them is a parameter-count error. The assistant MUST
    make sure that a name used inside the brackets without
    a class is already declared outside, earlier in the text:
    otherwise it silently becomes a parameter of the `GROUP`
    itself, and the aggregate runs over all its values.

17. `MAX` and `MIN` are prefix operators over a comma-separated
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

18. `AND`, `OR`, `XOR` and `NOT` always yield `BOOLEAN`
    (`TRUE` or `NULL`), never the value of an operand:
    `name(o) AND active(o)` is `TRUE`, not the name, and
    `a OR b` is `TRUE`, not the first non-`NULL` value.
    The assistant MUST NOT use them to select or pass a value
    through. For that use `expr IF cond` and `OVERRIDE a, b`.

19. The assistant MUST use `GROUP ... WHERE` only with `AGGR`,
    `NAGGR`, `CONCAT` or `LAST`. For `SUM`, `MAX`, `MIN` and
    `EQUAL` the condition goes into the aggregated expression
    with `IF` (`GROUP MAX f(x) IF cond(x) BY k(x)`), because
    these grouping types do not accept `WHERE` — see the
    [`GROUP` operator](../language/GROUP_operator.md).

### Abstract property rules (`+=`)

1. The value class of a `+=` implementation MUST fit within
   the value class declared on the abstract property. There
   is no implicit cast — an implementation with a wider
   class is rejected at server startup with a
   "wrong value class of implementation" error, whose
   `specified` and `expected` lines name the implementation's
   class and the declared one.

   An expression that widens the value class — arithmetic
   above all, and division most of all (rule 14 of the
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
   runs. It is that the row chosen is whichever one a service
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

4. A `PARTITION` does not split its window by the parameters of
   the property: to number rows separately for each `loc` in
   `idx(loc, x) <- PARTITION SUM 1 IF cond(loc, x) ORDER x`, the
   assistant MUST add `BY loc`. Without it the numbering runs
   across all values of `loc`. So it is with `idx(x) <- ...`
   where `loc` is an action or loop parameter. Rows whose
   summed expression is `NULL` are not in the window, so
   `SUM 1 IF cond` by itself numbers, under a unique order,
   the rows where `cond` holds from 1.
