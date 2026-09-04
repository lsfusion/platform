---
slug: "/Recursion_RECURSION"
title: 'Recursion (RECURSION)'
---

The *recursion* operator creates a [property](Properties.md) that sequentially performs two operations:

1.  Recursively builds an intermediate property (result) with an additional first parameter (operation number) as follows:
    1.  `result(0, o1, o2, ..., oN) = initial(o1, ..., oN)`, where `initial` is an *initial* property.
    2.  `result(i+1, o1, o2, ..., oN) = step(o1, ..., oN, $o1, $o2, ..., $oN) * result(i, $o1, $o2, ..., $oN)` for a numeric value class and `result(i+1, o1, o2, ..., oN) = step(o1, ..., oN, $o1, $o2, ..., $oN) IF result(i, $o1, $o2, ..., $oN)` otherwise, where `step` is a *step* property and `$o1, ..., $oN` denote the parameter values at the previous iteration. If the same set of objects is obtained at an iteration from several sets of the previous iteration, each of them yields a separate value.
2.  For all values of the obtained property, it calculates the given [aggregate function](Set_operations.md#func) grouping by all its parameters except the operation number.

The aggregate function is selected automatically based on the value class of `initial`/`step`: if they are of a numeric class, `SUM` is used and the result class is the same numeric class; otherwise (typically when they are `BOOLEAN`), `OR` is used and the result class is the same non-numeric class (except under the `CYCLES NO` policy, see below). Thus a numeric recursion sums, over all paths from the initial sets of objects to the given one, the initial property value at the start of the path multiplied by the product of the step values along the path. With an initial value of `1` and a constant step `1` the result is the number of such paths (in a tree, where a single path leads to each set, always `1`), with an initial value of `1` and a constant step `2` it is the sum of `2` to the power of the path length (in a tree, `2` to the power of the distance to the ancestor).

Note that sets of objects may begin to repeat after a certain number of iterations. In this case, we say that a cycle is formed. There are three policies for working with cycles:

1.  `CYCLES YES` - cycles are allowed. In this case, when a cycle is detected (a set of objects repeats along the path of iterations), a marker value is added for the repeated set — the rounded square root of the maximum value of the result class (`46341` for `INTEGER`, `3037000500` for `LONG`) — and no further iterations are built from the marker row; the final value for the set is the marker summed with the other values accumulated for it, so it may exceed the marker. For `BOOLEAN`-valued recursion there is no marker: repeated sets of objects are discarded, and the cycle does not change the result.
2.  `CYCLES NO` (default) - cycles are not allowed. It works similarly to the previous policy, but an additional constraint is created that rejects a value of the obtained property greater than half of the cycle marker. This is a numeric threshold, so a sufficiently large value of a recursion without cycles can exceed it as well. For `BOOLEAN`-valued `initial`/`step` under this policy every non-`NULL` value is replaced by the number `1`, and the result class becomes `LONG`.
3.  `CYCLES IMPOSSIBLE` - cycles are impossible. As a rule, it is used if there is a counter among the objects which increases at each iteration and, as a result, cannot be repeated.

When using the recursion operator, it is important to make sure that the recursive construction of the collection is finite, that is, the step value will sooner or later become `NULL`. (This refers primarily to a `CYCLES IMPOSSIBLE` policy because otherwise the recursion will stop at the first cycle found). If this condition is not met, the operation will be forced to stop depending on the settings of the SQL server.

For a hierarchy given by a `parent[class]` property, the typical recursive properties — ancestor, level, number of descendants, full name from the root — are provided by the ready-made [`Hierarchy`](Utils_Hierarchy.md) system module.

### Language

To declare a property that implements recursion, use the [`RECURSION` operator](../language/RECURSION_operator.md).

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
