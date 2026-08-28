---
slug: "/Rules_constraints"
title: 'Rules: constraints'
---

## Constraint rules

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
