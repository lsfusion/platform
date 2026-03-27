---
title: 'Grouping (GROUP)'
---

The *group* operator creates a [property](Properties.md) that divides all object collections in the system into groups, and calculates an [aggregating function](Set_operations.md#func) for each group following specified order. Accordingly, the set for which this aggregating function is calculated is determined as all the object collections belonging to this group. If the grouping set is empty, all matching object collections belong to one common group. 

Groups are defined for this operator as a set of properties (*groups*), and the order is defined as a list of properties and an increasing or decreasing marker. If the aggregation function is not [commutative](Set_operations.md#commutative), then the order must be uniquely determined. For commutative aggregators, the order does not change the final value by itself, but it becomes important when aggregation is performed not over the whole group, but only over some ordered fragment of the group.

At the same time, it is useful to distinguish between the *group key* and the *group elements*. The group key determines which object collections belong to the same group and for which set of values a result exists, while the group elements determine from which data that result is calculated. This distinction helps explain why grouping returns one value per group rather than a separate value for each source object collection, as in [partitioning / ordering](Partitioning_sorting_PARTITION_..._ORDER.md).

Functionally, grouping can be viewed as follows:

- for each group, the full set of object collections belonging to it is determined
- if necessary, only the relevant elements are kept from this set
- if order matters for the aggregate or only part of the group should participate, an order is defined inside the group
- if necessary, only a fragment of the ordered group is used, for example after skipping the first `m` elements the next `n` elements are taken
- the aggregate function is then calculated over the remaining data

Order inside a group can play two different roles. For non-commutative aggregators (`CONCAT`, `LAST`, and order-sensitive custom aggregates) it directly affects the result, so it should be uniquely determined. For commutative aggregators (`SUM`, `MAX`, `MIN`, `EQUAL`, `AGGR`, `NAGGR`) it is used primarily to select the required fragment of the group.


:::info
A uniquely determined order can be guaranteed if, for example, the IDs of all objects for which grouping is performed are specified when the order is defined.
:::

In addition to the standard types of aggregate functions for grouping, there are three additional types: `EQUAL`, `AGGR` and `NAGGR`.

-   `EQUAL` is a special case of the aggregation function `MAX` (or `MIN`), with the additional [constraint](Constraints.md) that the value of the operand of the aggregating function within each group must be the same. 
-   `AGGR` and `NAGGR` are a special case of `EQUAL`, but with an even more strict constraint: for each group there is no more than one object collection, the operand of the aggregating function is one of the objects, and the groups include all other objects. Aggregate function `NAGGR` only differs from `AGGR` in the fact that if it is used, no constraint is created (it is assumed that the constraint follows from the semantics of the properties of the operands and / or groupings themselves).

User-defined DBMS aggregate functions and custom aggregate functions declared in the database can also be used in grouping. Functionally, there are two different scenarios here:

- a regular custom aggregate computes its result from the values being aggregated inside the group; if order is specified, it defines the processing order of those values
- an ordered-set aggregate computes its result from an ordered sample of values inside the group, while additional arguments act as parameters of the function itself rather than elements of that sample; percentile-like functions belong to this category

### Language

To declare a property that implements grouping, use the [`GROUP` operator](GROUP_operator.md).

### Examples

```lsf
CLASS Game;
CLASS Team;
hostGoals = DATA INTEGER (Game);
hostTeam = DATA Team (Game);
date = DATA DATE (Game);
// sum over the whole group
hostGoalsScored(team) = GROUP SUM hostGoals(Game game) BY hostTeam(game);
// sum only over the three latest games in the group
last3HostGoalsScored(team) = GROUP SUM hostGoals(Game game) ORDER DESC date(game), game TOP 3 BY hostTeam(game);

name = DATA STRING[100] (Country);
// property (STRING[100]) -> Country is obtained
countryName = GROUP AGGR Country country BY name(country); 

CLASS Book;
CLASS Tag;
name = DATA STRING[100] (Tag);
in = DATA BOOLEAN (Book, Tag);
// example of an order-dependent aggregate with filtering inside the group
tags(Book b) = GROUP CONCAT name(Tag t) IF in(b, t), ', ' ORDER name(t), t;

value = DATA NUMERIC[14,2] (INTEGER);
// ordered-set aggregate: 90th percentile of value(i)
percentile90() = GROUP CUSTOM NUMERIC[14,2] 'percentile_cont' 0.9 WITHIN ORDER value(INTEGER i);
```
