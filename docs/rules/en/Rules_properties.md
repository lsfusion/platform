---
slug: "/Rules_properties"
title: 'Rules: properties'
---

## Property rules

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

17. A parameter's class annotation (`prop(SubClass x)`) is a
    signature, not a runtime filter: it resolves same-named
    properties and sets the signature, but the computed set is
    determined by the properties used in the expression.
    Reading a parent-class property with a subclass-annotated
    parameter still ranges over ALL objects of the parent class
    (e.g. in a `GROUP SUM` — silently wrong totals).

    To restrict the set to a class, the assistant MUST add
    an explicit `x IS SubClass` condition (or use a property
    declared on that subclass).

18. In the `GROUP ... BY` operator the assistant MUST NOT
    list in the `BY` block the upper parameters used
    in the operator's expressions: each such parameter
    is already implicitly a group — a parameter of the
    created property — and keeps its place in the signature.

    With an explicit parameter list on the left,
    the `BY` expressions are mapped in order only
    to the parameters not used in the expressions;
    a mismatch in count or classes is an error.

19. `MAX` and `MIN` are prefix operators over a comma-separated
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

## Abstract property rules (`+=`)

1. The value class of a `+=` implementation MUST fit within
   the value class declared on the abstract property; there
   is no implicit cast — an implementation with a wider
   class is rejected at server startup with a
   "wrong value class of implementation" error, whose
   `specified` and `expected` lines name the implementation's
   class and the declared one.

   Arithmetic is what widens the class most often, and it
   widens further than it looks:

   - `+` and `-` — like `MIN` / `MAX` and the selection
     operators — take the common ancestor, widening the whole
     part and the scale independently, so the result can be
     wider than either operand:
     `NUMERIC[16,2] + NUMERIC[10,4]` is `NUMERIC[18,4]`;
   - `*` adds both the whole parts and the scales:
     `NUMERIC[16,2] * NUMERIC[10,4]` is `NUMERIC[26,6]`;
   - `/` widens catastrophically: with the default settings
     its scale is always the maximum `NUMERIC` scale (`32`),
     so `NUMERIC[16,2] / NUMERIC[16,2]` is `NUMERIC[48,32]`.

   A `GROUP` aggregate mostly keeps the class of what it
   aggregates — a `GROUP SUM`, `GROUP MAX` or `GROUP LAST`
   over a `NUMERIC[16,2]` is `NUMERIC[16,2]` — but it carries
   outward whatever that expression already widened to.
   `GROUP CONCAT` is the aggregate that widens by itself: its
   result is a string of unlimited length (`ISTRING` against
   a declared `ISTRING[250]`). Plain string concatenation
   widens as well, summing the operands' lengths
   (`ISTRING[326]` against a declared `ISTRING[250]`).

   Any such expression the assistant MUST wrap in an explicit
   cast to the declared class:
   `f(X x) += NUMERIC[16,2](a(x) / b(x));`
   `f(X x) += ISTRING[250](a(x) + b(x));`

   For operands of integer classes the cast MUST go on an
   operand first, so that the division is not integer
   division (see rule 16 of the property rules); the result
   still widens to scale `32` like any other division, so
   the outer cast is needed as well:
   `f(X x) += NUMERIC[16,2](NUMERIC[16,2](a(x)) / b(x));`

## Ordering rules (`ORDER`)

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
