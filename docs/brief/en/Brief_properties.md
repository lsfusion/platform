---
slug: "/Brief_properties"
title: 'Brief: properties'
---

## Expressions and composition

A [property](../paradigm/Properties.md) takes a set of objects as parameters and returns exactly one value — like a pure function, but computed over the whole database at once, like a column of an SQL query. The value is either stored (a [data property](../paradigm/Data_properties_DATA.md), the [`DATA` operator](../language/DATA_operator.md)) or computed by an expression: arithmetic, logic (`AND`, `OR`, `NOT`), comparisons, string operations, class tests and casts (`IS`, `AS`). String, number and date functions (`lpad`, `substr`, `mod`, `currentDate`) are not operators — they are properties of the `Utils` and `Time` modules.

Substituting a property into another property's expression is [composition](../paradigm/Composition_JOIN.md); the [`JOIN` operator](../language/JOIN_operator.md) writes it out explicitly. The operators that create properties are listed in [Operators](../paradigm/Property_operators_paradigm.md).

```lsf
price = DATA NUMERIC[14,2] (Item);
vat = DATA NUMERIC[6,2] (Item);
priceWithVAT (Item i) = price(i) * (1 + vat(i) / 100);
```

## Grouping (GROUP)

The [`GROUP` operator](../language/GROUP_operator.md) splits all object collections into groups and computes one aggregate function per group — `SUM`, `MAX`, `MIN`, `CONCAT`, `LAST`, `EQUAL`, `AGGR` / `NAGGR`, `CUSTOM` (a DBMS aggregate):

```
GROUP
type [expr1, ..., exprN]
[orderClause]
[TOP topExpr] [OFFSET offsetExpr]
[WHERE whereExpr]
[BY groupExpr1, ..., groupExprM]
```

There is no separate counting function: a count is written as `GROUP SUM 1`. **Analogy**: SQL `GROUP BY`, except that the result is a property of its own rather than part of a query. The mechanism — which object collections fall into a group, what the parameters of the created property are, and where the order matters — is described in [grouping](../paradigm/Grouping_GROUP.md).

```lsf
sold (Sku s) = GROUP SUM quantity(OrderDetail d) BY sku(d);
```

## Partitioning and ordering (PARTITION)

The [`PARTITION` operator](../language/PARTITION_operator.md) also splits object collections into groups with a `BY` block, but returns a result not per group but per object collection, over an `ORDER`ed window inside its group. The aggregate functions here are `SUM`, `PREV`, `LAST`, `CUSTOM`: hence places and ranks, running numbering, cumulative totals, the value of the previous or the last collection in the window. **Analogy**: SQL window functions (`OVER (PARTITION BY ... ORDER BY ...)`). What the window covers is described in [partition / order](../paradigm/Partitioning_sorting_PARTITION_..._ORDER.md).

The `PARTITION UNGROUP` form solves the reverse task — it [distributes](../paradigm/Distribution_UNGROUP.md) the value of a source property over the object collections of a group: proportionally to a given expression (`PROPORTION`) or in order (`LIMIT`).

```lsf
place (Team t) = PARTITION SUM 1 ORDER DESC points(t), t BY conference(t);
```

## Object aggregation (GROUP AGGR, AGGR)

These operators work with objects rather than with values.

`GROUP AGGR` is the form of the [`GROUP` operator](../language/GROUP_operator.md) that returns the group's object itself. The result is the mapping inverse to the properties listed in `BY` — finding an object by its code, for example; the platform adds a constraint that a group has at most one such object.

The [`AGGR` operator](../language/AGGR_operator.md) goes further: it creates the object itself when the aggregated expression becomes not `NULL`, and deletes it when the expression becomes `NULL` again, filling in the properties that map the object back to the parameters. The mechanism is described in [aggregations](../paradigm/Aggregations.md).

```lsf
countryName = GROUP AGGR Country c BY name(c);
shipment (Invoice i) = AGGR ShipmentInvoice WHERE createShipment(i);
```

## Selection and override (CASE, IF, OVERRIDE)

The [selection operator](../paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) checks conditions in order and returns the result of the first one that holds; a condition holds if its value is not `NULL`.

- [`CASE`](../language/CASE_operator.md) — explicit `WHEN ... THEN ...` pairs and an optional `ELSE`.
- [`IF`](../language/IF_operator.md) — the postfix single form `result IF condition`; [`IF ... THEN`](../language/IF_..._THEN_operator.md) adds an `ELSE` block.
- [`OVERRIDE`](../language/OVERRIDE_operator.md) — the first operand that is not `NULL`; this is also how a default value is substituted for `NULL`.
- [`EXCLUSIVE`](../language/EXCLUSIVE_operator.md) — the same, plus a declaration that at most one operand is not `NULL`.
- [`MULTI`](../language/MULTI_operator.md) — the operand is chosen by the compatibility of the argument classes with its signature.

```lsf
signedQuantity (Ledger l) = MULTI quantity[InLedger](l), quantity[OutLedger](l);
price (Item i) = OVERRIDE salePrice(i), basePrice(i), 0;
```

## Recursion (RECURSION)

The [`RECURSION` operator](../language/RECURSION_operator.md) creates a property computed by iteration; it is reached for on trees, graphs and transitive closures — the level of a node, all the ancestors of an object, reachability along a chain of references. Its parts — `STEP`, the `$` prefix on a parameter and the `CYCLES` option — and the way the iterations are computed are described in [recursion](../paradigm/Recursion_RECURSION.md).

```lsf
level (Group child, Group parent) = RECURSION 1 IF child IS Group AND parent = child
                                              STEP 1 IF parent = parent($parent);
```

## Abstract properties (ABSTRACT)

The [`ABSTRACT` operator](../language/ABSTRACT_operator.md) declares a property without an implementation: the base module sets the value class and the parameter classes, and other modules add implementations with the [`+=` statement](../language/plus_equals_statement.md). From them the platform assembles a selection operator — this is [property extension](../paradigm/Property_extension.md), a way to remove a dependency between modules and to get property polymorphism.

Which implementation is chosen, and what is required of the implementations, is set by options — `MULTI`, `CASE`, `VALUE`, `EXCLUSIVE`, `OVERRIDE`, `FULL` — and is described in [property extension](../paradigm/Property_extension.md).

```lsf
name 'Name' = ABSTRACT ISTRING[250] (Document);              // base module
name (Shipment s) += ISTRING[250]('Shipment ' + number(s));  // shipment module
```
