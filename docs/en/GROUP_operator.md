---
title: 'GROUP operator'
---

The `GROUP` operator creates a [property](Properties.md) implementing [grouping](Grouping_GROUP.md).

### Syntax 

```
GROUP 
type [expr1, ..., exprN]
[orderClause]
[TOP topExpr [OFFSET offsetExpr]]
[WHERE whereExpr]
[BY groupExpr1, ..., groupExprM]
```

The `type` subrule, which is used immediately after `GROUP`, has the following syntax:

```
SUM | MAX | MIN | AGGR | NAGGR | EQUAL | CONCAT | LAST | CUSTOM [NULL] [className] aggrFunc
```

The `orderClause` subrule, which is used after the list of aggregated expressions, has the following syntax:

```
[WITHIN] ORDER [DESC] orderExpr1, ..., orderExprK
```

Here `type` specifies the aggregate function kind, while `orderClause` specifies the ordering form: either regular `ORDER ...` or `WITHIN ORDER ...` for ordered-set `CUSTOM` aggregates.

### Description

The `GROUP` operator creates a property implementing grouping. The type of grouping is determined by the type of the [aggregate function](Set_operations.md). This operator differs from others in that it can implicitly declare its parameters in the expressions used (by analogy with the [`=` statement](=_statement.md) when the parameters are not defined explicitly). At the same time, it is important to understand that these "implicitly declared" parameters are not parameters of the created property (which are actually determined by the `BY` block and / or the upper parameters used)

The `BY` block describes group expressions. Each expression corresponds to a parameter of the property being created. As in other operators, upper parameters can be used in this operator, and the used parameters also implicitly become groups of the created property. If the `BY` block is omitted and no upper parameters are used, all matching object collections form a single group and the created property has no parameters. Accordingly, when using the operator in the [`=` statement](=_statement.md) and explicitly defining the parameters on the left, the expressions from the `BY` block are mapped only for unused parameters. Moreover, if the classes or the number of these parameters do not match the number / classes of `BY` expressions then the platform will throw an error. 

:::info
If a `BY` block is defined, this operator cannot be used inside [expressions](Expression.md).
:::

The `ORDER` block defines the order in which the aggregate function will be calculated. It is mandatory for `CONCAT` and `LAST`. For [commutative](Set_operations.md#commutative) aggregators (`SUM`, `MAX`, `MIN`, `EQUAL`, `AGGR`, `NAGGR`) it may also be specified; in that case the order does not change the final aggregate value by itself, but it affects which records participate in aggregation when `TOP` / `OFFSET` are used. If the function is non-commutative, the order should be specified so that it is uniquely determined. If a new parameter is declared in the expressions specifying the order (i.e. parameter is not used in the remaining blocks or in the upper context), the condition of non-NULLness of all these expressions is automatically added.

For `CUSTOM` aggregates, the `WITHIN` keyword does not introduce some separate "extra ordering"; instead, it selects the SQL call form of the user-defined or DBMS built-in aggregate function. Without `WITHIN`, the order is passed inside the aggregate call, so a construction like `GROUP CUSTOM ... 'aggrFunc' expr1, ..., exprN ORDER orderExpr1, ...` corresponds to `aggrFunc(expr1, ..., exprN ORDER BY orderExpr1, ...)`. With `WITHIN`, the ordered-set form is used instead, so `GROUP CUSTOM ... 'aggrFunc' expr1, ..., exprN WITHIN ORDER orderExpr1, ...` corresponds to `aggrFunc(expr1, ..., exprN) WITHIN GROUP (ORDER BY orderExpr1, ...)`. This matters for aggregates that are defined in the DBMS specifically via `WITHIN GROUP`, such as `percentile_cont`.

From the result perspective, the difference is which values are treated as the aggregate input. Without `WITHIN`, the aggregate computes its result from the values of `expr1, ..., exprN`, while `ORDER` only specifies the processing order of those values. With `WITHIN`, the result is computed from the ordered set of values coming from `ORDER`: these values form the sample over which the aggregate is calculated, while `expr1, ..., exprN` become parameters of the aggregate function itself. For example, in `GROUP CUSTOM ... 'percentile_cont' 0.9 WITHIN ORDER value(i)`, the expression `0.9` specifies which percentile is requested, and the result is the 90th percentile of `value(i)` values inside the group.

The `TOP` block and its optional `OFFSET` continuation restrict the subset of records already selected for aggregation inside each group: first `OFFSET` is applied, then the next `TOP` records are taken in the specified order. The `OFFSET` block is not used on its own without `TOP`. If the `ORDER` block is omitted, these records are selected in arbitrary order.

The `WHERE` block defines the condition under which object collections will participate in the group operation. It can only be used with the aggregate functions `AGGR`, `NAGGR`, `CONCAT`, and `LAST`. For `LAST`, if `WHERE` is omitted, non-`NULL`ness of the aggregated expression itself is used as the condition.


:::info
For `AGGR` and `NAGGR` using this block explicitly (and not, say, an [`IF` operator](IF_operator.md) in `GROUP` and `BY` blocks) only makes sense from the perspective of being able to change the created property to non-`NULL` in some automatic mechanisms of the platform (for example, [automatic resolution](Simple_constraints.md) of simple constraints).
:::

### Parameters

- `type`

    Type of aggregate function. Can be one of the following built-in values: `SUM`, `MAX`, `MIN`, `CONCAT`, `EQUAL`, `AGGR`, `NAGGR`, `LAST`, or the form `CUSTOM [NULL] [className] aggrFunc`.

- `NULL`

    Keyword. Used only for `CUSTOM` aggregates and specifies that the aggregate may return `NULL` even when all parameter values are non-`NULL`.

- `className`

    Name of the [built-in class](Built-in_classes.md) of the returned value. Used only for `CUSTOM` aggregates and allows the result type to be set explicitly. If it is omitted, the result type is inferred from the main expression of the aggregate: usually from the first main operand, and for the `WITHIN` form or the form without main operands from the first `ORDER` expression.

- `aggrFunc`

    [String literal](IDs.md#strliteral) containing the name of a user-defined or DBMS built-in aggregate function. Used only for `CUSTOM` aggregates.

- `expr1, ..., exprN`

    A list of expressions whose values are passed to the aggregate function as operands. The number of expressions should correspond to the number of operands of the function used. `SUM`, `MAX`, `MIN`, `EQUAL`, `AGGR`, and `NAGGR` support one expression. For `SUM`, this expression must have a numeric class from the `IntegralClass` family, for example `INTEGER`, `LONG`, `NUMERIC`, or `DOUBLE`. For `AGGR` and `NAGGR`, the operand must be a simple object parameter rather than an arbitrary expression. `CONCAT` supports either two expressions, the aggregated value and the separator, or one JSON / JSONTEXT expression. `LAST` uses one expression, while the filtering condition is defined either by `WHERE` or automatically by non-`NULL`ness of that expression. For `CUSTOM` aggregates, the list may be empty, but then the `ORDER` / `WITHIN ORDER` block is mandatory.

- `groupExpr1, ..., groupExprM`  

    List of group expressions. If the list is omitted and no upper parameters are used, all matching object collections form a single group.

- `WITHIN`

    Keyword. Used only for `CUSTOM` aggregates and enables the ordered-set form of the aggregate function call. Without `WITHIN`, expressions from `ORDER` are passed inside the aggregate call: `aggrFunc(... ORDER BY ...)`. With `WITHIN`, they are moved into a separate SQL clause: `aggrFunc(...) WITHIN GROUP (ORDER BY ...)`. Functionally, this means that without `WITHIN` the aggregate works on the values of the main expressions, while with `WITHIN` it works on the ordered values from `ORDER`, and the main expressions become parameters of the function itself. It is not applicable to other aggregate types.

- `DESC`

    Keyword. Specifies a reverse iteration order for object collections. 

- `orderExpr1, ..., orderExprK`

    A list of expressions that define the order in which object collections will be iterated over when calculating the aggregate function. To determine the order, first the value of the first expression is used; then, if equal, the value of the second is used, etc. This list is mandatory for `CONCAT` and `LAST`. For `CUSTOM` aggregates it is mandatory if the main expression list is omitted.

- `TOP topExpr`

  Only the first `n` records will participate in aggregation inside each group, where `n` is the value of expression `topExpr`.

- `OFFSET offsetExpr`

  Optional continuation of the `TOP` block. Only records starting from offset `m` will participate in aggregation inside each group, where `m` is the value of expression `offsetExpr`.

- `whereExpr`

    Filtering expression. Only object groups for which the value of the filtering expression is not `NULL` will participate in the grouping.

### Examples

```lsf
CLASS Game;
CLASS Team;
hostGoals = DATA INTEGER (Game);
hostTeam = DATA Team (Game);
date = DATA DATE (Game);
hostGoalsScored(team) = GROUP SUM hostGoals(Game game) BY hostTeam(game);
last3HostGoalsScored(team) = GROUP SUM hostGoals(Game game) ORDER DESC date(game), game TOP 3 BY hostTeam(game);

name = DATA STRING[100] (Country);
// property (STRING[100]) -> Country is obtained
countryName = GROUP AGGR Country country BY name(country); 

CLASS Book;
CLASS Tag;
name = DATA STRING[100] (Tag);
in = DATA BOOLEAN (Book, Tag);

tags(Book b) = GROUP CONCAT name(Tag t) IF in(b, t), ', ' ORDER name(t), t TOP 10 OFFSET 0;

value = DATA NUMERIC[14,2] (INTEGER);
// 90th percentile of value(i)
percentile90() = GROUP CUSTOM NUMERIC[14,2] 'percentile_cont' 0.9 WITHIN ORDER value(INTEGER i);
```
