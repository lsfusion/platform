---
slug: "/Rules_physical"
title: 'Rules: physical model'
---

- [Execution](#execution)
- [Modules](#modules)
- [Migration (migration.script)](#migration-migrationscript)

## Execution

### Tables

How data is stored in the database is described in [tables](../paradigm/Tables.md).

1. Tables SHOULD be declared explicitly for every set of key classes in use, and properties with that set of parameters SHOULD be placed into them with the `TABLE` option. Logic SHOULD NOT rely on an automatically created `_auto_...` table: its name is built from the property's class IDs sorted alphabetically, so changing which classes a property takes moves the data to a different table — while merely reordering the same classes leaves it in the one it is in.

2. Properties with the same set of parameters that are usually read together SHOULD be stored in one table: reading them then requires no table join.

3. The `NODEFAULT` option SHOULD be used for narrow-purpose tables that properties may enter only explicitly. A table whose keys are built-in classes, no keys at all, or classes of other modules MUST be declared `NODEFAULT`, with its own properties placed into it by the `TABLE` option: otherwise at the next start it may receive the automatically placed properties of other modules with those key classes, together with their values.

4. The `FULL` option SHOULD be specified for a table that contains all objects of its key classes. It affects only how queries are executed, so it MUST NOT be specified for a table that is not filled for all objects.

5. The naming policy SHOULD be chosen at the start of a project. The short policy keeps database names readable, but with a large number of materialized properties it requires explicit field names to keep those names unique.

### Materializations

The mechanism itself is described in [materializations](../paradigm/Materializations.md).

1. Calculated properties that are read, or used in filter conditions, considerably more often than the data they depend on changes SHOULD be materialized.

2. Properties whose value is non-`NULL` for an infinite number of object sets SHOULD NOT be materialized — such a property cannot be materialized at all. The typical case is a property with a built-in class parameter, such as a date, that is not restricted by a condition.

3. Materializing a chain of intermediate properties multiplies the work done when data changes: the result that is actually read SHOULD be materialized, not every step of the computation.

4. A property that depends on frequently changing data and is read rarely SHOULD NOT be materialized — its stored values would be updated on every change.

5. After a materialized property's definition changes — a changed or added `+=` implementation of a materialized abstract property included — or after a direct data fix in the database, the stored values MUST be recomputed with the [`RECALCULATE` operator](../language/RECALCULATE_operator.md). A server restart does not do it for a changed definition alone: until the recalculation the stored values stay inconsistent with it, and a change of the data does not necessarily repair even the values it touches (see [materializations](../paradigm/Materializations.md)).

6. When a [set operation](../paradigm/Set_operations.md) reads a property created by another set operation and fails with the `TOO LONG QUERY` error, or the resulting query gets a bad execution plan, the inner property SHOULD be materialized (if it does not have too many values) or, where that is possible, read beforehand into a local property: otherwise its definition is included as a subquery in the text of the outer query (see [materializations](../paradigm/Materializations.md)). The `HINT` / `NOHINT` options do not split such a query. The same applies to the update of a materialized grouping when the definitions of the properties it reads, its grouping keys included, make the query too long — a smaller batch does not necessarily help. Whether the platform first materializes the intermediate changes depends on complexity and statistics, so such an update can fail on one server state and pass on another. One successful run does not guarantee that the next one stays within the query-length limit. If reading into a local property beforehand still computes the grouping for all groups, that computation remains. Materialization SHOULD then be considered, and computing one key at a time (`FOR ... NOINLINE`) should be tested where a suitable index gives fast access to the data of each group (see action rule 1 in the [logic rules](Rules_logic.md)).

7. A condition in which several `OR` conditions are joined by `AND`, where the operands of each `OR` are properties of different related objects (`(a1(i) OR b1(stock(i)) OR c1(group(i))) AND (a2(i) OR b2(stock(i)) OR c2(group(i))) AND ...`), can sharply increase the time it takes to compile the query: combinations of the operands of the different `OR` conditions are processed, so adding one more condition can increase the compilation time several times over — with about ten such conditions it can reach minutes. The delay occurs when the compiled query is not in the cache — for example, at the first execution after a server start (see [materializations](../paradigm/Materializations.md)). Moving such a condition unchanged into an assignment to a local property (rule 6) changes nothing, and neither does the `COMPLEX` option on a property with such a condition: the same condition is compiled. The local property SHOULD be filled in steps: first assign `TRUE` by the first `OR` condition, then, for each of the others, reset the value where it does not hold — `l(Item i) <- TRUE WHERE a1(i) OR b1(stock(i)) OR c1(group(i));`, `l(Item i) <- NULL WHERE l(i) AND NOT (a2(i) OR b2(stock(i)) OR c2(group(i)));` — and use `l(i)` in place of the original condition.

8. A [selection](../paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) with many conditions that is not materialized — including an abstract property whose implementations are such selections — SHOULD be used once in an expression. An operator over a selection can be applied to each of its results, and when two operands of one operator are selections, the combinations of their conditions can be processed, so the time it takes to compile the query can grow much faster than the number of conditions — to tens of seconds with about twenty conditions, and to minutes when two conditions over the same selection are joined by `OR`. The delay occurs before the query is sent to the database, when the compiled query is not in the cache (rule 7), and for a named property with such an expression it can also occur at a full server start after a change of the modules, in the `Synchronizing properties` step of the `start` log. A selection passed to a property that references its parameter several times is used at each reference (`capitalize(STRING s) = IF s THEN upper(left(s, 1)) + substrFrom(s, 2);`, `capitalize(kindName(i))`). When this makes the compilation too slow, and such a property takes a value of a built-in class and can be written in SQL for the SQL servers in use with the same `NULL` handling, it SHOULD be declared as a [`FORMULA`](../language/FORMULA_operator.md) property (`capitalize(STRING s) = FORMULA TEXT PG 'upper(left($1, 1)) || substring($1, 2)';`): the selection stays one operand however many times the text uses the parameter. In a session that has changed the data the selection depends on, a [change operator](../paradigm/Change_operators_SET_CHANGED_etc.md) over it uses both its current and its previous value. When an expression in an action uses the selection several times, and only its current value, that value SHOULD be read into a local property first, and the expression applied to that local property. A materialized selection (rule 1) is read from its stored field in a session that has not changed the data it depends on. After such a change, compiling the definition of the selection can delay a read of it and the update of the stored values.

9. When a query filters or sorts by a calculated property that combines several [selections](../paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) over [groupings](../paradigm/Grouping_GROUP.md) — a form sorted by the column of such a property, for example — and building that query is slow although reading the property is fast, a property that cannot be materialized (rule 2) SHOULD be marked with the [`COMPLEX` option](../language/Property_options.md): the condition by the value of the property, including the comparison that positions a sorted form on its current object, can otherwise be expanded over the conditions and results of those selections, and building the query, not executing it, can take minutes.

10. When an action needs a property created by a [grouping](../paradigm/Grouping_GROUP.md) that is not materialized for a part of the objects only, and the condition that selects them is one the platform does not count as restricting — a range with one given bound, a range of a numeric property or of a property that has no index, a range whose bounds are properties of the same object, the absence of a value of a property — the objects SHOULD first be written into a local property by an assignment with the conditions that do not read the grouping, and the property read with a `WHERE` over that local property: otherwise the grouping can be computed for all objects, with the condition applied to its result (see [materializations](../paradigm/Materializations.md)). The local property SHOULD hold only the objects the grouping is needed for: the platform restricts the subquery by it when it estimates that as cheaper. Worked examples are in the `How-to_Heavy_computations` article.

### Indexes

The mechanism itself is described in [indexes](../paradigm/Indexes.md).

1. Indexes SHOULD be created for properties used for filtering or search in forms and queries, and SHOULD NOT be created just in case: every index is updated whenever the values of its fields change.

2. Only materialized properties can be indexed, so an index on a calculated property requires materializing it — and that decision is made on the materialization rules of this article — a property is materialized because it is read, or used in a filter, considerably more often than the data it depends on changes, not for the sake of the index.

3. A composite index SHOULD be created when filtering uses several fields of one table at once. The fields restricted by equality SHOULD come first and the one restricted by a range after them, since that is the shape a btree scan narrows on.

4. An index duplicating the automatically created ones SHOULD NOT be created: the unique index on all key fields of a table and the indexes on the key suffixes already exist.

5. For fields searched with the `LIKE` and `MATCH` operators, the index types of the same names SHOULD be used instead of a plain index.

6. The value of a property without parameters that bounds a range SHOULD NOT be combined with another value by the `MIN` or `MAX` operator: the value of such a property can be read from its table inside the query, and then for the condition `date(x) > (MIN p(), d) AND date(x) <= (MAX p(), d)` PostgreSQL does not select the range by the index on `date` but scans the whole table. A range between two values SHOULD be written as two branches, each comparing the field with the bounds directly: `(date(x) > d AND date(x) <= p()) OR (date(x) > p() AND date(x) <= d)`. A stored property without parameters that is used in query conditions SHOULD be declared with the `PREREAD` option: its value is then read before the query is executed and passed to it as a value.

### Examples

A table per set of key classes, a property placed into one by `TABLE`, and
the read result materialized — the line-level `sum` is left computed, since
rule 3 says the chain's intermediate step is not what gets stored. The
composite index puts the equality field before the range one, as rule 3 of
the index rules asks.

```lsf
TABLE order (Order);
TABLE orderDetail (OrderDetail);
TABLE skuStock (Sku, Stock);

date = DATA DATE (Order) TABLE order INDEXED;
sum (OrderDetail d) = quantity(d) * price(d);
sum (Order o) = GROUP SUM sum(OrderDetail d) BY order(d) MATERIALIZED TABLE order;

INDEX customer(Order o), date(o);
```

## Modules

1. The assistant MUST split lsFusion code into modules
   by domain logic or feature area,
   not by arbitrary technical grouping.

2. The assistant SHOULD prefer relatively short modules.

   A single broad module SHOULD NOT keep growing
   when the logic naturally separates
   into smaller cohesive modules.

3. The assistant MUST apply low coupling and high cohesion:
   closely related classes, properties, actions, and forms
   SHOULD stay together,
   and cross-module dependencies SHOULD remain narrow and explicit.

4. Module `NAMESPACE` SHOULD be chosen by shared business domain,
   not by the full module name.

5. When a module belongs to an existing domain family,
   the assistant SHOULD reuse that family namespace
   for all its elements.

   A new namespace SHOULD be created only
   for a genuinely new domain,
   not for each technical submodule.

6. If the module name already equals the intended domain namespace,
   omitting `NAMESPACE` is acceptable
   because lsFusion will use the module name as the default.

   Otherwise, the assistant SHOULD specify `NAMESPACE` explicitly.

7. The assistant SHOULD use `REQUIRE`, `EXTEND`,
   abstract properties / actions,
   and form extensions to connect modules
   instead of duplicating logic
   or creating a god module.

8. Before adding code to an existing module,
   the assistant MUST check whether the logic belongs
   to that module's domain.

   If not, the assistant SHOULD create
   or extend a more appropriate module.

9. When introducing a new module,
   the assistant MUST choose dependencies deliberately
   and avoid circular or unnecessary dependencies.

10. To use a property, action, class, or form
    from another module, that module MUST be reachable
    from the current module's `REQUIRE` chain — either
    directly, or transitively through other required modules.

    If the owning module is not in the transitive `REQUIRE`
    closure, the platform raises a "Property not found"
    (or analogous "not found") error at startup.

    The assistant MUST add the owning module
    (or any module that already requires it)
    to the current module's `REQUIRE` list before using
    its elements.

11. The server ships bundled system modules whose names
    MUST NOT be reused for application modules — the server
    fails at startup with `module '<name>' has already been
    added`. The bundled names are:
    System, Utils, UserEvents, Scheduler, Email, Time,
    Reflection, Security, Service, Icon, Authentication,
    SystemEvents, Word, WebSocket, Integration, Profiler,
    SQLUtils, ProcessMonitor, DefaultData, Image, Printer,
    Numerator, Chat, Eval, I18n, Com, Sound, Backup, OpenCV,
    Geo, Historizable, Schedule, Document, QZTray, Excel,
    Hierarchy, RabbitMQ, MasterData, Messenger, Whatsapp,
    Skype, Telegram, Viber, Slack.

    For generic domain names from this list (`MasterData`,
    `Document`, `Schedule`, `Numerator`), the assistant
    SHOULD add a project prefix to the module name.

## Migration (migration.script)

1. Renaming a property or action, or moving it to another
   namespace, changes its canonical name. Whenever the assistant
   renames or re-namespaces an existing element, it MUST record
   the change in `migration.script` in the same edit. Otherwise
   the platform treats the old and new names as unrelated
   elements — the old one is dropped and the new one starts empty.

2. For a primary (`DATA`) property this is silently destructive
   and the assistant MUST take special care. The rename / namespace
   change MUST be recorded as a `STORED PROPERTY` change
   (`old canonical name -> new canonical name`), which renames the
   underlying database column and preserves its data. A plain
   `PROPERTY` change carries over only the security-policy and
   reflection settings, NOT the stored data: on the next server
   start the old column is renamed to `_DELETED_` plus its old
   database name — or dropped outright if a column by that name
   is already there — and a fresh empty column is created for
   the new name, so all existing values of the property are lost.

3. Renaming a custom class, or moving it to another namespace,
   MUST be recorded as a `CLASS` change to preserve its objects
   and their data. Such a class rename can also change the
   canonical names of its `DATA` properties. These are not tracked
   automatically and MUST be added as their own `STORED PROPERTY`
   changes.
