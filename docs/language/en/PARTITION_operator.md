---
slug: "/PARTITION_operator"
title: 'PARTITION operator'
---

The `PARTITION` operator creates a [property](../paradigm/Properties.md) that implements [partition/order](../paradigm/Partitioning_sorting_PARTITION_..._ORDER.md) or [simple distribution](../paradigm/Distribution_UNGROUP.md).

### Syntax

```
PARTITION 
type [expr1, ..., exprN]
[ORDER [DESC] orderExpr1, ..., orderExprK]
[TOP topExpr] [OFFSET offsetExpr]
[BY groupExpr1, ..., groupExprM]
```

Where `type` is defined as:

```
SUM
PREV
LAST
UNGROUP propertyId distributionType
CUSTOM [NULL] [className] aggrFunc
```

And `distributionType` is defined as:

```
PROPORTION [STRICT] ROUND(digits)
LIMIT [STRICT]
```

### Description

The `PARTITION` operator creates a property that, for each object collection, either computes an [aggregate function](../paradigm/Set_operations.md#func) over the partition window (`SUM`, `PREV`, `LAST`, `CUSTOM`) or distributes a value among the object collections of the group (`UNGROUP`).

The `BY` block describes the groups into which object collections are split. If the `BY` block is not specified, all object collections are considered to belong to the same group.

The `ORDER` block defines the order in which the aggregate function will be calculated or the distribution will take place. If this function is [non-commutative](../paradigm/Set_operations.md), the specified order must be uniquely determined. If a new parameter (not used earlier in the `PARTITION` and `BY` options and in the upper context) is declared in the expressions defining the order, the condition of non-`NULL`ness of all these expressions is automatically added when calculating the resulting value.

The `TOP` and `OFFSET` blocks restrict the subset of records selected inside each partition: first `OFFSET` skips the leading records, then the next `TOP` records are taken in the specified order. Either block may be specified independently.

### Parameters

- `type`

    Type of operation. Can be one of: `SUM`, `PREV`, `LAST`, `UNGROUP`, `CUSTOM`.

- `propertyId`

    [ID](IDs.md#propertyid) of the distributed property. The value of this property must be numeric, and the number of parameters must be equal to the number of groups in the `BY` block. Objects identifying a group are passed to this property as input.

- `distributionType`

    Distribution strategy. One of:

    - `PROPORTION` — proportional distribution: the value of `propertyId` is split among the object collections of the group in proportion to the main expression and rounded to `digits` decimal places.
    - `LIMIT` — limit-based distribution: the value of `propertyId` is assigned to the first (in the `ORDER`) object collection up to the limit given by the main expression; the remainder is then assigned to the next collection, and so on. The limits are given in the same units as the distributed value.

- `STRICT`

    The value of `propertyId` must be split exactly (without remainder) across the object collections of the group. If a remainder remains (which may be negative for `PROPORTION`), it is added to the first object collection in the `ORDER` for `PROPORTION` and to the last object collection in the `ORDER` for `LIMIT`.

- `digits`

    [Integer literal](Literals.md#intliteral) specifying the number of decimal places used by `PROPORTION` rounding.

- `NULL`

    Specifies that the aggregate may return `NULL` even when all parameter values are non-`NULL`.

- `className`

    Name of the [built-in class](../paradigm/Built-in_classes.md) of the value returned by `CUSTOM`. If omitted, the result class is inferred from the first main expression (or from the first `ORDER` expression when the main expression list is empty).

- `aggrFunc`

    [String literal](IDs.md#strliteral) containing the name of a user-defined or DBMS built-in aggregate function.

- `expr1, ..., exprN`

    Main expressions. For `SUM`, `PREV`, `LAST`, and `UNGROUP` the list contains exactly one expression: for `SUM` it is summed cumulatively over the partition window; for `PREV` and `LAST` it is taken from the previous-row and current-row respectively (`NULL` for the first row in the case of `PREV`); for `UNGROUP` it defines the proportion (with `PROPORTION`) or the limit (with `LIMIT`). With `UNGROUP`, an object collection whose value of this expression is `NULL` is excluded from the distribution — the created property returns `NULL` for it — unlike a collection with the value `0`, which participates in the distribution. The `STRICT` remainder is likewise assigned only among the participating collections. If every collection must participate, convert the possible `NULL` of this expression to `0` (for example, with the [`OVERRIDE` operator](OVERRIDE_operator.md)). For `CUSTOM`, the list contains the operands passed to `aggrFunc`; it may be empty, but then the `ORDER` block is mandatory.

- `groupExpr1, ..., groupExprM`

    List of group expressions.

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections.

- `orderExpr1, ..., orderExprK`

    A list of expressions that define the order in which object collections will be iterated. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc.

- `TOP topExpr`

    Within each partition, only the first `n` records in the partition order will participate in the calculation, where `n` is the value of the expression `topExpr`.

- `OFFSET offsetExpr`

    Within each partition, the first `m` records in the partition order will be skipped, where `m` is the value of the expression `offsetExpr`.

### Examples

```lsf
// determines the place of the team in the conference
CLASS Conference;
conference = DATA Conference (Team);
points = DATA INTEGER (Team);
gamesWon = DATA INTEGER (Team);
place 'Place' (Team team) = PARTITION SUM 1 ORDER DESC points(team), gamesWon(team) BY conference(team);

// building ordinal indexes of objects in the database in ascending order of their internal IDs (i.e., in the order of creation)
index 'Number' (Object o) = PARTITION SUM 1 IF o IS Object ORDER o;

// finds the team next in the conference standings
prevTeam (Team team) = PARTITION PREV team ORDER place(team), team BY conference(team);

// proportional distribution example
CLASS Order;
transportSum 'Freight costs' = DATA NUMERIC[10,2] (Order);

CLASS OrderDetail;
order = DATA Order (OrderDetail) NONULL DELETE;
sum = DATA NUMERIC[14,2] (OrderDetail);

// with transportSum equal to 100 and three lines with sum 1, 1, 1 the lines get 33.34, 33.33 and 33.33
transportSum 'Freight costs by line' (OrderDetail d) = PARTITION UNGROUP transportSum
                                    PROPORTION STRICT ROUND(2) sum(d)
                                    ORDER d
                                    BY order(d);

// example of distribution with limits
// with discountSum equal to 60 and lines with sum 100 and 300 the first line in the ORDER gets 60, the second — NULL;
// with discountSum equal to 500 the lines get 100 and 400 (300 up to the limit plus the remainder of 100 due to STRICT)
discountSum 'Discount' = DATA NUMERIC[10,2] (Order);
discountSum 'Discount by line' (OrderDetail d) =
    PARTITION UNGROUP discountSum
                LIMIT STRICT sum(d)
                ORDER sum(d), d
                BY order(d);
;
```
