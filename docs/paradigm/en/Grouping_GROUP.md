---
slug: "/Grouping_GROUP"
title: 'Grouping (GROUP)'
---

The *group* operator creates a [property](Properties.md) that divides all object collections in the system into groups, and calculates an [aggregating function](Set_operations.md#func) for each group following specified order. Accordingly, the set for which this aggregating function is calculated is determined as all the object collections belonging to this group. If the grouping set is empty and the operator does not reference any upper parameters, all matching object collections belong to one common group. Any upper parameters that the operator does reference implicitly extend the grouping set, so that each combination of their values forms a separate group. 

Groups are defined for this operator as a set of properties (*groups*), and the order is defined as a list of properties and an increasing or decreasing marker. An object collection for which at least one of the groups is `NULL` belongs to no group and takes no part in the calculation of any result. If the aggregation function is not [commutative](Set_operations.md#commutative), then the order must be uniquely determined. For commutative aggregators, the order does not change the final value by itself, but it becomes important when aggregation is performed not over the whole group, but only over some ordered fragment of the group.

At the same time, it is useful to distinguish between the *group key* and the *group elements*. The group key determines which object collections belong to the same group and for which set of values a result exists, while the group elements determine from which data that result is calculated. This distinction helps explain why grouping returns one value per group rather than a separate value for each source object collection, as in [partitioning / ordering](Partitioning_sorting_PARTITION_..._ORDER.md).

Functionally, grouping can be viewed as follows:

- for each group, the full set of object collections belonging to it is determined
- if necessary, only the relevant elements are kept from this set
- if order matters for the aggregate or only part of the group should participate, an order is defined inside the group
- if necessary, only a fragment of the ordered group is used — for example, the first `n` elements, all elements after skipping the first `m`, or the next `n` elements after skipping the first `m`
- the aggregate function is then calculated over the remaining data

Order inside a group can play two different roles. For non-commutative aggregators (`CONCAT`, `LAST`, and order-sensitive custom aggregates) it directly affects the result, so it should be uniquely determined. For commutative aggregators (`SUM`, `MAX`, `MIN`, `EQUAL`, `AGGR`, `NAGGR`) it is used primarily to select the required fragment of the group.


:::info
A uniquely determined order can be guaranteed if, for example, the IDs of all objects for which grouping is performed are specified when the order is defined.
:::

In addition to the standard types of aggregate functions for grouping, there are three additional types: `EQUAL`, `AGGR` and `NAGGR`.

-   `EQUAL` is a special case of the aggregation function `MAX` (or `MIN`), with the additional [constraint](Constraints.md) that the value of the operand of the aggregating function within each group must be the same. The result class is the class of the operand.
-   `AGGR` and `NAGGR` are a special case of `EQUAL`, but with an even more strict constraint: for each group there is no more than one object collection, the operand of the aggregating function is one of the objects, and the groups include all other objects. Aggregate function `NAGGR` only differs from `AGGR` in the fact that if it is used, no constraint is created (it is assumed that the constraint follows from the semantics of the properties of the operands and / or groupings themselves). Since the operand is itself one of the objects of the group, the result class is the class of that object. The constraint of `AGGR` is checked when changes are [applied](Apply_changes_APPLY.md), and two concurrent applies do not see each other's changes that have not been committed yet: when each of them creates an object in the same group, both pass the check, and the group ends up with two objects. To prevent this, the property with `AGGR` should be [materialized](Materializations.md): it is stored in a row keyed by the group values, so concurrent applies write the same row, one of them is retried after the conflict, and on the retry the constraint cancels it.

The class of a `SUM` is the class of its operand. A sum over `NUMERIC` is computed inside a query with no limit on its precision, so the precision of its class is enforced only where the sum is written to a column created with that class: a [materialized](Materializations.md) property or a [local property](Data_properties_DATA.md#local) declared with that class, or the temporary table of the subquery holding the sum, when the platform materializes it while [running the query](Working_parameters.md). A `GROUP SUM` over `NUMERIC[6,2]` whose total reaches `10000` is therefore computed by one execution of a query and fails with `numeric field overflow` by another, in which that subquery is materialized. Materialization can be switched on by a retry after a timeout, so on the same data one execution can pass and another fail: more often on a cold server or over a larger range of data, but not on every execution even after a cold start. Retries after a timeout and the moves between materialization steps are written to the `sqladjust` log ([Journals and logs](Journals_and_logs.md#logs)). A cast of the result does not prevent this, since the materialized column holds the sum itself: the operand is cast to a wider class inside the aggregate, `GROUP SUM NUMERIC[16,2](x)`.

User-defined DBMS aggregate functions and custom aggregate functions declared in the database can also be used in grouping. Functionally, there are two different scenarios here:

- a regular custom aggregate computes its result from the values being aggregated inside the group, and if order is specified, it defines the processing order of those values
- an ordered-set aggregate computes its result from an ordered sample of values inside the group, while additional arguments act as parameters of the function itself rather than elements of that sample. Percentile-like functions belong to this category

The result class of a custom aggregate may be set explicitly. Otherwise it is inferred from the values that drive the aggregation: the aggregated operand values for a regular custom aggregate, or the ordered sample for an ordered-set aggregate.

### Language

To declare a property that implements grouping, use the [`GROUP` operator](../language/GROUP_operator.md).

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
