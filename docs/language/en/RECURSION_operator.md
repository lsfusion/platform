---
slug: "/RECURSION_operator"
title: 'RECURSION operator'
---

The `RECURSION` operator creates a [property](../paradigm/Properties.md) that implements [recursion](../paradigm/Recursion_RECURSION.md).

### Syntax 

```
RECURSION initialExpr STEP stepExpr [CYCLES policy]
```

### Description

The `RECURSION` operator creates a property that implements recursion. An [expression](Expression.md) that describes the next step of the recursion may access not only the property parameters but also the parameters at the previous step. This access has the syntax `$name`, where `name` is the name of the parameter. If `$name` is used, the corresponding parameter `name` (without `$`) must also appear in `initialExpr` or `stepExpr` — using `$name` alone in `stepExpr` is not enough.

Values produced across all iterations are aggregated per set of property parameters: if `initialExpr` and `stepExpr` are of a numeric class, the value of the next iteration equals the value of the previous one multiplied by `stepExpr`, and the values of all iterations are summed (`SUM`), so the result is the sum over all paths from the initial sets of objects to the given one, where the `initialExpr` value at the start of the path is multiplied by the product of the `stepExpr` values along the path (in particular, with `initialExpr` and a constant `stepExpr` both equal to `1`, the number of such paths); otherwise (typically when they are `BOOLEAN`), the `OR` aggregation is used. For a detailed description of the semantics and cycle policies, see [Recursion (RECURSION)](../paradigm/Recursion_RECURSION.md).

Another `RECURSION` operator cannot be used inside `stepExpr` — such nesting is forbidden. The restriction applies only to `stepExpr`; `RECURSION` may appear inside `initialExpr`.

### Parameters

- `initialExpr`

    An expression whose value is the initial property.

- `stepExpr`

    An expression whose value is a property of a recursion step. Allows a special syntax `$name` to access the value of the `name` parameter in the previous step.

- `policy`

    Cycle-handling policy. One of:

    - `YES` — cycles are allowed: when a cycle is detected, a marker value is added for the repeated set of objects — the rounded square root of the maximum value of the result class (`46341` for `INTEGER`, `3037000500` for `LONG`), and no further iterations are built from the marker row; the final value for the set is the marker summed with the other values accumulated for it, so it may exceed the marker. For `BOOLEAN`-valued recursion there is no marker: repeated sets of objects are discarded, and the cycle does not change the result.
    - `NO` (default) — cycles are not allowed; an additional constraint is created that rejects a result greater than half of the cycle marker. This is a numeric threshold, so a sufficiently large value of a recursion without cycles can exceed it as well. For `BOOLEAN`-valued `initialExpr`/`stepExpr` under this policy every non-`NULL` value is replaced by the number `1`, and the result class becomes `LONG`.
    - `IMPOSSIBLE` — cycles are not possible (an optimisation hint, typically used when one of the parameters is a strictly increasing counter).

### Examples

```lsf
CLASS Node;
edge = DATA BOOLEAN (Node, Node);

// iteration over an integer from 'from' to 'to' (this property is by default included in the System module)
iterate(i, from, to) = RECURSION i==from AND from IS INTEGER AND to IS INTEGER STEP i==$i+1 AND i<=to CYCLES IMPOSSIBLE;

// counts the number of different paths from a to b in the graph
pathes 'Number of paths' (a, b) = RECURSION 1 AND a IS Node AND b==a STEP 1 IF edge(b, $b);

// not NULL if parent is an ancestor of the child group or that group itself (so the property selects all descendants of parent);
// the numeric value is the number of paths from child to parent along the parent property, always 1 in a tree
parent = DATA Group (Group);
isParent 'Is parent' (Group child, Group parent) = RECURSION 1 IF child IS Group AND parent == child
                                                             STEP 1 IF parent == parent($parent);

// level of a group in the hierarchy — the number of its ancestors including itself (1 for a root)
level 'Level' (Group child) = GROUP SUM 1 IF isParent(child, Group parent);

// Fibonacci numbers, the property calculates all Fibonacci numbers up to the value to, (afterwards it will return null)
fib(i, to) = RECURSION 1 IF (i==0 OR i==1) AND to IS INTEGER STEP 1 IF (i==$i+1 OR i==$i+2) AND i<to CYCLES IMPOSSIBLE;
```


Note that Fibonacci numbers can be implemented without adding the to parameter:

```lsf
fib(i) = RECURSION 1 IF (i==0 OR i==1) STEP 1 IF (i==$i+1 OR i==$i+2);
```

In the current implementation, however, the platform optimizer is less focused on working with numbers, so it cannot yet determine that the step function is increasing and stop the recursion on its own, artificially creating the corresponding condition, as is done in the above example. Even more questions arise when this property needs to be displayed in a dynamic list (and in a static list this cannot be done at all, since the number of non-`NULL` values is infinite). In this case, the current order in this list must also be taken into account and also pushed into the query. These limitations will be removed in future versions, but in the current version it is recommended to take them into account.
