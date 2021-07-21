---
title: 'GROUP operator'
---

The `GROUP` operator creates a [property](Properties.md) implementing [grouping](Grouping_GROUP.md).

### Syntax 

    GROUP 
    type expr1, ..., exprN
    [ORDER [DESC] orderExpr1, ..., orderExprK]
    [WHERE whereExpr]
    [BY groupExpr1, ..., groupExprM]

### Description

The `GROUP` operator creates a property implementing grouping. The type of grouping is determined by the type of the [aggregate function](Set_operations.md). This operator differs from others in that it can implicitly declare its parameters in the expressions used (by analogy with the [`=` statement](=_statement.md) when the parameters are not defined explicitly). At the same time, it is important to understand that these "implicitly declared" parameters are not parameters of the created property (which are actually determined by the `BY` block and / or the upper parameters used)

The `BY` block describes group expressions. Each expression corresponds to a parameter of the property being created. As in other operators, upper parameters can be used in this operator, and the used parameters also implicitly become groups of the created property. Accordingly, when using the operator in the [`=` statement](=_statement.md) and explicitly defining the parameters on the left, the expressions from the `BY` block are mapped only for unused parameters. Moreover, if the classes or the number of these parameters do not match the number / classes of `BY` expressions then the platform will throw an error. 

:::info
If a `BY` block is defined, this operator cannot be used inside [expressions](Expression.md).
:::

The `ORDER` block defines the order in which the aggregate function will be calculated. Can only be used for [non-commutative](Set_operations.md) aggregate functions (`CONCAT`, `LAST` ) and must be uniquely defined. If a new parameter is declared in the expressions specifying the order (i.e. parameter is not used in the remaining blocks or in the upper context), the condition of non-NULLness of all these expressions is automatically added.

The `WHERE` block defines the condition under which object collections will participate in the group operation. Can only be used for the aggregate functions `AGGR`, `NAGGR`, `LAST`.


:::info
For `AGGR` and `NAGGR` using this block explicitly (and not, say, an [`IF` operator](IF_operator.md) in `GROUP` and `BY` blocks) only makes sense from the perspective of being able to change the created property to non-`NULL` in some automatic mechanisms of the platform (for example, [automatic resolution](Simple_constraints.md) of simple constraints).
:::

### Parameters

- `type`

    Type of aggregate function. Can be one of the following: `SUM`, `MAX`, `MIN`, `CONCAT`, `EQUAL`, `AGGR`, `NAGGR`, `LAST`. 

- `expr1, ..., exprN`

    A list of expressions whose values are passed to the aggregate function as operands. The number of expressions should correspond to the number of operands of the function used. 

- `groupExpr1, ..., groupExprN`  

    List of group expressions. 

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections. 

- `orderExpr1, ..., orderExprM`

    A list of expressions that define the order in which object collections will be iterated over when calculating the aggregate function. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc. 

- `whereExpr`

    Filtering expression. Only object groups for which the value of the filtering expression is not `NULL` will participate in the grouping.

### Examples

```lsf
CLASS Game;
CLASS Team;
hostGoals = DATA INTEGER (Game);
hostTeam = DATA Team (Game);
hostGoalsScored(team) = GROUP SUM hostGoals(Game game) BY hostTeam(game);

name = DATA STRING[100] (Country);
// property (STRING[100]) -> Country is obtained
countryName = GROUP AGGR Country country WHERE country IS Country BY name(country); 

CLASS Book;
CLASS Tag;
name = DATA STRING[100] (Tag);
in = DATA BOOLEAN (Book, Tag);

tags(Book b) = GROUP CONCAT name(Tag t) IF in(b, t), ', ' ORDER name(t), t;
```
