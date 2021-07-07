---
title: 'PARTITION operator'
---

The `PARTITION` operator creates a [property](Properties.md) that implements [partition/order](Partitioning_sorting_PARTITION_..._ORDER.md) or [simple distribution](Distribution_UNGROUP.md).

### Syntax

There are two different types of `PARTITION` operator. The first implements partition/order:

    PARTITION 
    type expr
    [ORDER [DESC] orderExpr1, ..., orderExprK]
    [BY groupExpr1, ..., groupExprM]

The second implements simple distribution:

    PARTITION 
    UNGROUP propertyId distributionType expr
    [ORDER [DESC] orderExpr1, ..., orderExprK]
    [BY groupExpr1, ..., groupExprM]

where `distributionType` can be described in several ways:

    PROPORTION [STRICT] ROUND(digits)
    LIMIT [STRICT]

### Description

The `PARTITION` operator creates a property that divides all object collections in the system into groups, and taking into account the specified order:

-   calculates an [aggregate function](Set_operations.md) for each object collection. In case of partition/order,
-   it distributes a certain value among the object collections of one group in the case of distribution.

The `BY` block describes the groups into which numerous sets of object collections will be divided. If the `BY` block is not specified, all object collections are considered to belong to the same group. 

The `ORDER` block defines the order in which the aggregate function will be calculated or the distribution will take place. If this function is [non-commutative](Set_operations.md), the specified order must be uniquely determined. If a new parameter (not used earlier in the  `PARTITION` and `BY` options and in the upper context) is declared in the expressions defining the order, when calculating the resulting value the condition of non-NULLness of all these expressions is automatically added.

### Parameters

- `type`

    Type of aggregate function. Currently the aggregate function types `SUM` and `PREV` are supported.

- `propertyId`

    [ID](IDs.md#propertyid) of the distributed property. The value of this property must be numeric, and the number of parameters must be equal to the number of groups in the `BY` block. When calculating the values of group/partition expressions, objects that identify a certain group of object sets will be passed to this property as an input.

- `distributionType`

    Distribution type. These are of the following types:

    - `PROPORTION`

        Keyword specifying the use of proportional distribution. In this case, the value of the distributed property for a particular group is distributed proportionally among the object collections belonging to the group. The proportion is defined by the `expr` expression that is specified after the distribution type.

        - `STRICT`

            When this keyword is specified, the value of the distributed property must be exactly (without a remainder) distributed between the object collections belonging to the group. If after distribution there is a remainder (which may also be negative), it is added to the first object collection in accordance with the order defined in the `ORDER` block.

        - `ROUND(digits)`

            Specifies the number of decimal places the value will be rounded to.

            - `digits` – [Integer literal](Literals.md#intliteral) specifying the number of decimal places. 

    - `LIMIT`

        A keyword specifying the use of distribution with specified limits. In this case, the value of the distributed property is initially set for the first object collection. If the limit is exceeded for this set, the limit is set to the first object collection, and the rest of the value of the distributed property is assigned to the second object collection. It is then checked for exceeding the limit for the second object collection, and so on. The limit is defined by the `expr` expression specified after specifying the distribution type.

        - `STRICT`

            When this keyword is specified, the value of the distributed property must be exactly (without a remainder) distributed between the object collections belonging to the group. If after distribution there is a remainder, it is added to the last object collection in accordance with the order defined in the `ORDER` block.

- `expr`

    An [expression](Expression.md) whose value is passed as an input to the aggregating function as an operand in case of partition/order. In case of distribution with type `PROPORTION` it defines the proportion, and with type `LIMIT` it defines the limit.

- `groupExpr1, ..., groupExprM`  

    List of group expressions (groups). 

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections. 

- `orderExpr1, ..., orderExprK`

    A list of expressions that define the order in which object collections will be iterated when calculating the aggregate function or during distribution. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc. 

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

transportSum 'Freight costs by line' (OrderDetail d) = PARTITION UNGROUP transportSum
                                    PROPORTION STRICT ROUND(2) sum(d)
                                    ORDER d
                                    BY order(d);

// example of distribution with limits
discountSum 'Discount' = DATA NUMERIC[10,2] (Order);
discountSum 'Discount by line' (OrderDetail d) =
    PARTITION UNGROUP discountSum
                LIMIT STRICT sum(d)
                ORDER sum(d), d
                BY order(d);
;
```
