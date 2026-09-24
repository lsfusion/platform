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

The platform considers a set finite when each of its parameters has a source to iterate over: a [user class](User_classes.md) (the `a IS A` condition, the `a AS A` expression, or the `A a` declaration of a parameter the operator introduces) or a property with a finite set of non-`NULL` values in which the parameter takes part (for example, a [data property](Data_properties_DATA.md)). A known [built-in class](Built-in_classes.md) is not such a source by itself, and neither is comparing a [type conversion](Type_conversion.md) of a parameter with a literal or a computed value: in the conditions `LONG(a) = 5` or `STRING(a) = '5'` (looking an object up by its internal identifier) the parameter `a` occurs only under the type conversion, so such a set is considered infinite and the operation incorrect. The source may also follow from other parts of the operation — for example, from the property being assigned to or from another condition on the same parameter; the error occurs when there is none anywhere. A source counts only in the operation that iterates over the parameter. Grouping iterates only over the parameters it introduces; the upper parameters become its groupings, and their source comes from the operation that uses the result of the grouping. Partition/order, recursion and distribution iterate over all their parameters, although they do not introduce them, so for them neither the `A a` declaration (in the operator itself or among the property's parameters), nor the property being assigned to, nor a condition of the operation that uses their result counts. The error is detected only at execution time (the module loads successfully) and is reported as `Set operation is incorrect` or `Parameter violates type constraint or set operation is incorrect`, without pointing at the statement, so in a script of several statements look for the parameter that has neither a user class nor a source property. The fix is to state the user class explicitly or to rewrite the condition through such a property.

### Examples

```lsf
CLASS A;
d = DATA INTEGER (A);

f (b) = GROUP SUM 1 IF d(a) < b;
messageF  { MESSAGE f(5); } // will be executed successfully

// the iterate property is the source to iterate over for b;
// without this condition f(b) is not NULL for an infinite number of b, and the operation would be incorrect
g = GROUP SUM f(b) IF iterate(b, 1, 10);
messageG  { MESSAGE g(); } // will be executed successfully

FORM f
    OBJECTS d=DATE
;

// the value of d is passed to the form;
// without OBJECTS d=... there is no filter for dates, d IS DATE is not NULL for an infinite number of d, and the operation would be incorrect
printFWithD { PRINT f OBJECTS d=currentDate(); } // will be executed successfully
```

The class of a parameter does not follow from comparing its type conversion with a literal:

```lsf
// declaring the parameter class makes the set finite;
// without it a has neither a class nor a source property, and the operation would be incorrect
byId (LONG id) = GROUP MAX A a IF LONG(a) = id;
```

For partition/order the `A a` declaration among the property's parameters is not a source:

```lsf
locked = DATA BOOLEAN (A);
// the a IS A condition inside the operator makes the set finite;
// without it NOT locked(a) holds for an infinite number of a despite the A a declaration, and the operation would be incorrect
index (A a) = PARTITION SUM 1 IF NOT locked(a) AND a IS A ORDER a;
```
