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

Values produced across all iterations are aggregated: if `initialExpr` and `stepExpr` are of a numeric class, the `SUM` aggregation is used; otherwise (typically when they are `BOOLEAN`), the `OR` aggregation is used. For a detailed description of the semantics and cycle policies, see [Recursion (RECURSION)](../paradigm/Recursion_RECURSION.md).

Another `RECURSION` operator cannot be used inside `stepExpr` — such nesting is forbidden. The restriction applies only to `stepExpr`; `RECURSION` may appear inside `initialExpr`.

### Parameters

- `initialExpr`

    An expression whose value is the initial property.

- `stepExpr`

    An expression whose value is a property of a recursion step. Allows a special syntax `$name` to access the value of the `name` parameter in the previous step.

- `policy`

    Cycle-handling policy. One of:

    - `YES` — cycles are allowed. For `BOOLEAN`-valued recursion there is no maximum to use as a cycle marker, so the marker has no effect on the result.
    - `NO` (default) — cycles are not allowed; a constraint is added that the result must not reach the maximum value of its class.
    - `IMPOSSIBLE` — cycles are not possible (an optimisation hint, typically used when one of the parameters is a strictly increasing counter).

### Examples

```lsf
CLASS Node;
edge = DATA BOOLEAN (Node, Node);

// iteration over an integer from 'from' to 'to' (this property is by default included in the System module)
iterate(i, from, to) = RECURSION i==from AND from IS INTEGER AND to IS INTEGER STEP i==$i+1 AND i<=to CYCLES IMPOSSIBLE;

// counts the number of different paths from a to b in the graph
pathes 'Number of paths' (a, b) = RECURSION 1 AND a IS Node AND b==a STEP 1 IF edge(b, $b);

// defines at what level child is from parent, and null if it is not a child (thus this property can be used to define all children)
parent = DATA Group (Group);
level 'Level' (Group child, Group parent) = RECURSION 1 IF child IS Group AND parent == child
                                                                  STEP 1 IF parent == parent($parent);

// Fibonacci numbers, the property calculates all Fibonacci numbers up to the value to, (afterwards it will return null)
fib(i, to) = RECURSION 1 IF (i==0 OR i==1) AND to IS INTEGER STEP 1 IF (i==$i+1 OR i==$i+2) AND i<to CYCLES IMPOSSIBLE;
```


Note that Fibonacci numbers can be implemented without adding the to parameter:

```lsf
fib(i) = RECURSION 1 IF (i==0 OR i==1) STEP 1 IF (i==$i+1 OR i==$i+2);
```

In the current implementation, however, the platform optimizer is less focused on working with numbers, so it cannot yet determine that the step function is increasing and stop the recursion on its own, artificially creating the corresponding condition, as is done in the above example. Even more questions arise when this property needs to be displayed in a dynamic list (and in a static list this cannot be done at all, since the number of non-`NULL` values is infinite). In this case, the current order in this list must also be taken into account and also pushed into the query. These limitations will be removed in future versions, but in the current version it is recommended to take them into account.
