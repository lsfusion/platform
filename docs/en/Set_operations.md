---
title: 'Set operations'
sidebar_label: Overview
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

-   [Group (`GROUP`)](Grouping_GROUP.md)
-   [Partition/order (`PARTITION ... ORDER`)](Partitioning_sorting_PARTITION_..._ORDER.md)
-   [Recursion (`RECURSION`)](Recursion_RECURSION.md)
-   [Distribution (`UNGROUP`)](Distribution_UNGROUP.md)

### Operation correctness {#correct}

You should consider that during each operation on a set of object collections, this set must be finite. In this case, the operation is called *correct*.

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
