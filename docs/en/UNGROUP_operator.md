---
title: 'UNGROUP operator'
---

:::warning
At the moment, the `UNGROUP` operator is not implemented.
:::

The `UNGROUP` operator creates a [property](Properties.md) that implements [distribution](Distribution_UNGROUP.md) in an extended form.

### Syntax

    UNGROUP 
    propertyId [BY groupExpr1, ..., groupExprM] 
    distributionType exPropertyId [BY exGroupExpr1, ..., exGroupExprM]
    [ORDER [DESC] orderExpr1, ..., orderExprK]
    [WHERE whereExpr]

where `distributionType` can be described in several ways:

    PROPORTION [STRICT] ROUND(digits)
    LIMIT [STRICT]

### Description

The `UNGROUP` operator creates a property that distributes a certain value among object collections of the same group on a "many-to-many" basis.

The first block, `BY`, describes groups that the set of object collections will be broken into. If the `BY` block is not specified, all object collections are considered to belong to the same group. 

The second `BY` block describes additional groups that the result will be grouped by for checking constraints / calculating proportions. If the second block `BY` is not specified, all object collections are considered to belong to the same group. 

The `ORDER` block defines the order in which distribution will be done. The defined order must be uniquely determined.

The `WHERE` block defines a condition under which an object collection will participate in distribution operation.

### Parameters

- `propertyId`

    [ID](IDs.md#propertyid) of the distributed property. The value of this property must be numeric, and the number of parameters must be equal to the number of groups in the `BY` block. When calculating the values of group/partition expressions will be passed to this property as an input.

- `groupExpr1, ..., groupExprM`  

    List of group expressions. 

- `distributionType`

    Distribution type. These are of the following types:

    - `PROPORTION`

        Keyword specifying the use of proportional distribution. In this case, the value of the distributed property for a particular group is distributed proportionally among the object collections belonging to the group. The proportion is defined by the `expr` expression that is specified after the distribution type.

        - `STRICT`

            When this keyword is specified, the value of the distributed property must be exactly (without a remainder) distributed between the object collections belonging to the group. If after distribution there is a remainder (which may also be negative), it is added to the first object collection in accordance with the order defined in the `ORDER` block.

        - `ROUND(digits)`

            Specifies the number of decimal places the value will be rounded to.

            - `digits`

                [Integer literal](Literals.md#intliteral) specifying the number of decimal places. 

    - `LIMIT`

        A keyword specifying the use of distribution with specified limits. In this case, the value of the distributed property is initially set for the first object collection. If the limit is exceeded for this set, the limit is set for the first object collection, and the rest of the value of the distributed property is assigned to the second set of objects. It is then checked for exceeding the limit for the second object collection, and so on. The limit is defined by the `expr` expression specified after specifying of the distribution type.

        - `STRICT`

            When this keyword is specified, the value of the distributed property must be exactly (without a remainder) distributed between the object collections belonging to the group. If after distribution there is a remainder, it is added to the last object collection in accordance with the order specified in the `ORDER` block.

- `exPropertyId`

    [ID](IDs.md#propertyid) of the property that defines a proportion for `PROPORTION` type distributions, and defines limits for `LIMIT` type distributions. The value of this property must be numeric, and the number of parameters must be equal to the number of groups in the `BY` block. 

- `exGroupExpr1, ..., exGroupExprM`  

    List of additional group expressions. 

- `DESC`

    Keyword. Specifies a reverse iterate order for object collections. 

- `orderExpr1, ..., orderExprK`

    A list of expressions that determine the order in which object collections will be iterated when calculating the aggregate function or during distribution. To determine the order, first the value of the first expression is used; then, if equal, the value of the second one is used, etc. 

- `whereExpr`

    Filtering expression. Only object groups for which the value of the filtering expression is not `NULL` will participate in the grouping.

### Examples
