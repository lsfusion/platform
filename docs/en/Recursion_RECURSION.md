---
title: 'Recursion (RECURSION)'
---

The *recursion* operator is an operator that creates a [property](Properties.md) which sequentially performs two operations:

1.  Recursively builds an intermediate property (result) with an additional first parameter (operation number) as follows:
    1.  `result(0, o1, o2, ..., oN) = initial(o1, ..., oN)`, where `initial` is an *initial* property.
    2.  `result(i+1, o1, o2, ..., oN) = step(o1, ..., oN, $o1, $o2, ..., $oN) IF result(i, $o1, $o2, ..., $oN)`, where `step` is a *step* property.
2.  For all values of the obtained property, it calculates the given [aggregate function](Set_operations.md#func) grouping by all its parameters except the operation number.

Currently, only two types of aggregate functions are supported for the recursion operator: `SUM` and `OR`. The latter is used in the case when the initial value and step are of class `BOOLEAN`. `SUM` is used in all other cases.

Note that sets of objects may begin to repeat after a certain number of iterations. In this case, we say that a cycle is formed. There are three policies for working with cycles:

1.  `CYCLES YES` - cycles are allowed. In this case, when a cycle is detected, the value of the property will be equal to the maximum allowed value for the value class of this property. This policy is not supported when the initial value and step are of class `BOOLEAN`.
2.  `CYCLES NO` (default) - cycles are not allowed. It works similarly to the previous policy, but an additional constraint is created that the value of the obtained property should not be equal to the maximum value (which just means that a cycle has formed for this set of objects).
3.  `CYCLES IMPOSSIBLE` - cycles are impossible. As a rule, it is used if there is a counter among the objects which increases at each iteration and, as a result, cannot be repeated.

When using the recursion operator, it is important to make sure that the first step execution process is finite, that is, the step value will sooner or later become `NULL`. (This refers primarily to a `CYCLES IMPOSSIBLE` policy because otherwise the recursion will stop at the first cycle found). If this condition is not met, the operation will be forced to stop depending on the settings of the SQL server.

### Language

To declare a property that implements recursion, use the [`RECURSION` operator](RECURSION_operator.md).

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
