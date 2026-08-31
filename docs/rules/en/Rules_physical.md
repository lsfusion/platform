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

1. Tables should be declared explicitly for every set of key classes in use, and properties with that set of parameters should be placed into them with the `TABLE` option. No logic should rely on an automatically created `_auto_...` table: its name is built from the property's class IDs sorted alphabetically, so changing which classes a property takes moves the data to a different table — while merely reordering the same classes leaves it in the one it is in.

2. Properties with the same set of parameters that are usually read together should be stored in one table: reading them then requires no table join.

3. The `NODEFAULT` option should be used for narrow-purpose tables that properties may enter only explicitly.

4. The `FULL` option should be specified for a table that contains all objects of its key classes. It affects only how queries are executed, so it must not be specified for a table that is not filled for all objects.

5. The naming policy should be chosen at the start of a project. The short policy keeps database names readable, but with a large number of materialized properties it requires explicit field names to keep those names unique.

### Materializations

The mechanism itself is described in [materializations](../paradigm/Materializations.md).

1. Aggregated properties that are read, or used in filter conditions, considerably more often than the data they depend on changes should be materialized.

2. Properties whose value is non-`NULL` for an infinite number of object sets should not be materialized — such a property cannot be materialized at all. The typical case is a property with a built-in class parameter, such as a date, that is not restricted by a condition.

3. Materializing a chain of intermediate properties multiplies the work done when data changes: the result that is actually read should be materialized, not every step of the computation.

4. A property that depends on frequently changing data and is read rarely should not be materialized — its stored values would be updated on every change.

5. After a materialized property's definition changes, or after a direct data fix in the database, the stored values should be recomputed with the [`RECALCULATE` operator](../language/RECALCULATE_operator.md).

### Indexes

The mechanism itself is described in [indexes](../paradigm/Indexes.md).

1. Indexes should be created for properties used for filtering or search in forms and queries, and should not be created just in case: every index is updated whenever the values of its fields change.

2. Only materialized properties can be indexed, so an index on a calculated property requires materializing it — and that decision is made on the materialization rules of this article — a property is materialized because it is read, or used in a filter, considerably more often than the data it depends on changes, not for the sake of the index.

3. A composite index should be created when filtering uses several fields of one table at once; the fields restricted by equality should come first and the one restricted by a range after them, since that is the shape a btree scan narrows on.

4. An index duplicating the automatically created ones should not be created: the unique index on all key fields of a table and the indexes on the key suffixes already exist.

5. For fields searched with the `LIKE` and `MATCH` operators, the index types of the same names should be used instead of a plain index.

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
   the change in `migration.script` in the same edit; otherwise
   the platform treats the old and new names as unrelated
   elements — the old one is dropped and the new one starts empty.

2. For a primary (`DATA`) property this is silently destructive
   and the assistant MUST take special care. The rename / namespace
   change MUST be recorded as a `STORED PROPERTY` change
   (`old canonical name -> new canonical name`), which renames the
   underlying database column and preserves its data. A plain
   `PROPERTY` change carries over only the security-policy and
   reflection settings, NOT the stored data.

3. Without the `STORED PROPERTY` entry, on the next server start
   the old column is renamed to `_DELETED_` plus its old database
   name — or dropped outright if a column by that name is already
   there — and a fresh
   empty column is created for the new name, so all existing
   values of the property are lost. The assistant MUST NOT rename
   or move a `DATA` property to another namespace without adding
   this entry.

4. Renaming a custom class, or moving it to another namespace,
   MUST be recorded as a `CLASS` change to preserve its objects
   and their data. Such a class rename can also change the
   canonical names of its `DATA` properties; these are not tracked
   automatically and MUST be added as their own `STORED PROPERTY`
   changes.
