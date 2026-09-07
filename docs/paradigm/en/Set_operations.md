---
slug: "/Set_operations"
title: 'Set operations'
---

One of the key features of the platform is the ability to execute certain operations for all object collections for which the values of one or more [properties](Properties.md) are not `NULL`. In the property logic such an operation is the calculation of various *aggregate functions*. 

### Aggregate functions {#func}

An aggregate function calculates a certain *result* as a single object on a set of object collections. This function is defined by the *initial value* (typically `NULL`), [properties](Properties.md) that it uses (*operands*), *operation of addition* to the *intermediate result* of the current operand values, and *conversion function* of the intermediate result to the final (typically the intermediate result is the final result).

<a className="lsdoc-anchor" id="commutative"/>

Aggregate function is *commutative* if the order in which the object collections of the original set are iterated over does not matter when calculating the result. 

The table below shows the currently supported types of aggregate functions:

|Type/statement option  |Initial value|Names of the operands|Add operation|Conversion function|Commutativity|Data type|
|-----------------------|-------------|---------------------|-------------|-------------------|-------------|---------|
|`SUM`                  |`NULL`       |operand              |result = result (+) operand|result|+|number|
|`MAX`                  |`NULL`       |operand              |result = max(result, operand)|result|+|any comparable|
|`MIN`                  |`NULL`       |operand              |result = min(result, operand)|result|+|any comparable|
|`CONCAT`               |`NULL`       |separator, operand   |result = CONCAT separator, result, operand|result|-|string|
|`LAST` / `PREV`        |`NULL`       |where, operand       |result = IF where THEN operand ELSE result|result|-|any|

From the perspective of determining the set of object collections and the result display method, four main operators for working with sets can be distinguished:

-   [Group (`GROUP`)](Grouping_GROUP.md) — divides the object collections into groups and computes one result per group.
-   [Partition/order (`PARTITION ... ORDER`)](Partitioning_sorting_PARTITION_..._ORDER.md) — also groups the object collections, but computes a result for each object collection, over the part of its group up to its own position in the order.
-   [Recursion (`RECURSION`)](Recursion_RECURSION.md) — builds a set recursively from an initial value and a repeated step, then aggregates over the result.
-   [Distribution (`UNGROUP`)](Distribution_UNGROUP.md) — the inverse of grouping: spreads a value across the object collections of a group so that grouping the results by sum gives — or, in non-strict mode, approximates — that value.

### Operation correctness {#correct}

You should consider that during each operation on a set of object collections, this set must be finite. In this case, the operation is called *correct*.

The platform considers a set finite when each of its parameters has a source to iterate over: a [user class](User_classes.md) (the `a IS A` condition, the `a AS A` expression, or the `A a` declaration in the operator) or a property with a finite set of non-`NULL` values in which the parameter takes part (for example, a [data property](Data_properties_DATA.md)). A known built-in type is not such a source by itself (see the `hs` example below), and neither is comparing a parameter cast to another type with a literal or a computed value: in the conditions `LONG(a) = 5` or `STRING(a) = '5'` (looking an object up by its internal identifier) the parameter `a` occurs only under the [type conversion operator](../language/Type_conversion_operator.md), so such a set is considered infinite and the operation incorrect. The source may also follow from other parts of the operation — for example, from the property being assigned to or from another condition on the same parameter; the error occurs when there is none anywhere. It is detected only at execution time (the module loads successfully) and is reported as `Set operation is incorrect` or `Parameter violates type constraint or set operation is incorrect`, without pointing at the statement, so in a script of several statements look for the parameter that has neither a user class nor a source property. The fix is to state the user class explicitly or to rewrite the condition through such a property.

### Examples

```lsf
CLASS A;
d = DATA INTEGER (A);

f (b) = GROUP SUM 1 IF d(a) < b;
messageF  { MESSAGE f(5); } // will be executed successfully

g = GROUP SUM f(b);
messageG  { MESSAGE g(); } // f(b) is not NULL for infinite number b, the platform will throw an error

FORM f
    OBJECTS d=DATE
;

printFWithD { PRINT f OBJECTS d=currentDate(); } // will be executed successfully

// there is no filter for dates, and d IS DATE is not NULL for an infinite number d, the platform will throw an error
printFWithoutD { PRINT f; } 
```


There are several non-trivial cases when the operation is correct but the platform cannot determine this. For example, if the only limiting condition for a parameter is whether it falls within the range:

```lsf
hs = GROUP SUM 1 IF (a AS INTEGER) >= 4 AND a <= 6;
// theoretically, it should return 3, but the platform will still throw an error
messageHS  { MESSAGE hs(); } 
// workaround: to work with intervals, the iterate property can be used
// (which, in turn, is implemented through recursion)
hi = GROUP SUM 1 IF iterate(a, 4, 6); 
```

The class of a parameter does not follow from comparing its cast to another type with a literal:

```lsf
// a has neither a class nor a source property — the set is infinite, the error is thrown at execution time
badById = GROUP MAX a IF LONG(a) = 5;
// declaring the parameter class makes the set finite
byId (LONG id) = GROUP MAX A a IF LONG(a) = id;
```
