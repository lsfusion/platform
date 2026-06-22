---
slug: "/System_Reflection"
title: 'Reflection'
---

`Reflection` is a [system module](System_modules.md) that exposes the application's own metadata — the navigator, forms, properties, actions, and physical-model tables — as classes, properties, and forms, so that the running logic can introspect its own structure. It is pulled in via `REQUIRE Reflection` (`System` and `Authentication` are pulled in automatically).

The module stores one object per metadata element (one `Property` per property, one `Form` per form, one `Table` per table, and so on), filled by the platform at startup. Lookup properties recover an object from its canonical name or SID; statistics, storage attributes, and user column preferences hang off these objects; and the forms at the end of the module display them under the `metadata` navigator folder.

### Property groups

[Properties and actions](User_classes.md) are organized into a tree of groups.

| Class / property                            | What it holds                                                                 |
|---------------------------------------------|-------------------------------------------------------------------------------|
| `PropertyGroup`                             | one object per group of properties and actions                                |
| `parent[PropertyGroup]`                     | the parent group in the tree                                                  |
| `level[PropertyGroup, PropertyGroup]`       | recursive depth: `1` for a group relative to itself, growing by `1` per step up to an ancestor |
| `caption[PropertyGroup]` / `number[PropertyGroup]` / `SID[PropertyGroup]` | display caption, sort order, and string identifier |
| `propertyGroup[STRING]`                     | the group with the given `SID`                                                |

### Navigator metadata

The [navigator](Navigator.md) tree is represented by `NavigatorElement` and its two subclasses.

| Class / property                              | What it holds                                                               |
|-----------------------------------------------|------------------------------------------------------------------------------|
| `NavigatorElement`                            | one object per navigator element                                            |
| `NavigatorFolder`                             | a navigator element that groups other elements                              |
| `NavigatorAction`                             | a navigator element that opens a form or runs an action                     |
| `caption[NavigatorElement]` / `canonicalName[NavigatorElement]` / `number[NavigatorElement]` | display caption, unique canonical name, and sort order |
| `parent[NavigatorElement]`                    | the parent element in the tree                                              |
| `level[NavigatorElement, NavigatorElement]`   | recursive depth, defined like `level[PropertyGroup, PropertyGroup]`         |
| `form[NavigatorElement]`                      | the form a navigator element opens                                          |
| `action[NavigatorAction]`                     | the action a navigator action runs                                         |
| `navigatorElementCanonicalName[STRING]`       | the element with the given canonical name                                  |
| `isNavigatorFolder[NavigatorElement]` / `isNavigatorAction[NavigatorElement]` | flags telling a folder from an action |

### Forms metadata

| Class / property                  | What it holds                                                                       |
|-----------------------------------|--------------------------------------------------------------------------------------|
| `Form`                            | one object per [form](Forms.md)                                                      |
| `caption[Form]` / `canonicalName[Form]` | display caption and unique canonical name                                     |
| `form[STRING]`                    | the form with the given canonical name                                              |
| `currentForm[]` / `activeForm[]`  | native properties giving the canonical name of the current and the active form      |

`NoForm` is a single dedicated subclass of `Form`, with the canonical name `_NOFORM`, used by the profiler to attribute work done outside any form.

### Actions and properties metadata

`ActionOrProperty` is the abstract base for both properties and actions; `Property` and `Action` are its concrete subclasses.

| Property                                                    | What it holds                                                                  |
|------------------------------------------------------------|--------------------------------------------------------------------------------|
| `canonicalName[Property]` / `canonicalName[Action]`        | the unique canonical name                                                      |
| `canonicalName[ActionOrProperty]`                          | the canonical name regardless of the concrete subclass                        |
| `caption[ActionOrProperty]`                                | display caption                                                                |
| `annotation[ActionOrProperty]`                             | the annotation                                                                 |
| `class[ActionOrProperty]`                                  | the implementing Java class                                                    |
| `parent[ActionOrProperty]`                                 | the `PropertyGroup` the element belongs to                                     |
| `number[ActionOrProperty]`                                 | sort order                                                                     |
| `propertyCanonicalName[STRING]` / `actionCanonicalName[STRING]` | the property / action with the given canonical name                      |
| `actionOrPropertyCanonicalNameWithPostfix[STRING]`         | the element whose canonical name, with an `_action` / `_property` postfix appended, matches the argument (so a property and an action with the same name stay distinct) |

Properties additionally carry storage and statistics attributes.

| Property                                                | What it holds                                                                   |
|---------------------------------------------------------|---------------------------------------------------------------------------------|
| `dbName[Property]`                                      | the database column name                                                        |
| `tableSID[Property]`                                    | the SID of the table the property is stored in                                  |
| `stored[Property]`                                      | flag: the property is stored in the database                                    |
| `loggable[Property]` / `userLoggable[Property]`         | flags: change logging is enabled by the application / by the user               |
| `userMaterialized[Property]`                            | flag: the user requested materialization                                        |
| `isSetNotNull[Property]`                                | flag: a `NOT NULL` constraint is set                                            |
| `disableInputList[Property]`                            | flag: the value-completion list is disabled                                     |
| `select[Property]`                                      | the in-cell selection control (`SelectType`: `Button`, `ButtonGroup`, `Dropdown`, `List`, `No`) |
| `complexity[Property]`                                  | the computation complexity estimate                                            |
| `stats[Property]`                                       | the estimated row count                                                        |
| `quantity[Property]` / `notNullQuantity[Property]`      | the total and the non-`NULL` value counts                                      |
| `lastRecalculate[Property]`                             | the time the materialized value was last recalculated                          |

`maxStatsProperty[]` caps the statistics for a user-loggable property: enabling user logging on a property whose `stats[Property]` exceeds the cap is rejected. `webServerUrl[]` holds the web-server address. Both are shown on the `options` form.

`getPropertyDependencies[Property]` and `getPropertyDependents[Property]` fill the local properties `propertyDependencies[INTEGER]` / `propertyDependents[INTEGER]` with the properties a given property reads from and the properties that read from it.

### Property draws and column preferences

A `PropertyDraw` is a single placement of a property or action on a form (one form column or panel cell).

| Property                                  | What it holds                                                                  |
|-------------------------------------------|--------------------------------------------------------------------------------|
| `actionOrProperty[PropertyDraw]`          | the displayed `ActionOrProperty`                                               |
| `sid[PropertyDraw]`                       | the placement identifier within the form                                      |
| `caption[PropertyDraw]`                   | display caption                                                               |
| `form[PropertyDraw]`                      | the form the placement belongs to                                            |
| `groupObject[PropertyDraw]`               | the `GroupObject` the placement is drawn against                             |
| `propertyDraw[Form, STRING]`              | the placement with the given form and SID                                    |
| `show[PropertyDraw]` / `show[PropertyDraw, CustomUser]` | the visibility status (`PropertyDrawShowStatus`: `Show`, `Hide`), globally and per user |

The display of a column can be tuned globally and overridden per user. Each preference comes in two parallel forms — `[PropertyDraw]` for the application-wide value and `[PropertyDraw, CustomUser]` for the per-user override:

| Property                  | What it holds                                            |
|---------------------------|----------------------------------------------------------|
| `columnCaption`           | the column caption                                       |
| `columnPattern`           | the value-formatting pattern                             |
| `columnWidth`             | the column width                                         |
| `columnFlex`              | the column flex factor                                   |
| `columnOrder`            | the column position                                      |
| `columnSort`              | the sort priority among columns                          |
| `columnAscendingSort`     | the sort direction                                       |
| `inGrid`                  | flag: the column is shown in the grid                    |

`hasUserPreferences[GroupObject]` and `hasUserPreferences[GroupObject, CustomUser]` flag a group object whose column layout has been customized globally or for a given user. When such preferences exist, a newly added placement is set to `Hide` so it does not appear until explicitly enabled, and the preferences are reset if every shown column is removed from the form.

`GroupObject` is one object group on a form; it carries its own display settings, again in a global and a per-user form: `fontSize`, `isFontBold`, `isFontItalic`, `pageSize` (rows per page), and `headerHeight`.

`FormGrouping` is a saved grouping over a group object's placements: `name[FormGrouping]`, `groupObject[FormGrouping]`, `itemQuantity[FormGrouping]`, and per-placement `groupOrder` / `sum` / `max` / `pivot` settings.

### Physical model

The physical-model classes mirror the database [tables](Tables.md), their keys, and their columns.

| Class / property                          | What it holds                                                                |
|-------------------------------------------|------------------------------------------------------------------------------|
| `Table`                                   | one object per table                                                         |
| `sid[Table]` / `table[STRING]`            | the table SID and the reverse lookup                                        |
| `rows[Table]`                             | the table row count                                                         |
| `skipVacuum[Table]`                       | flag: skip vacuuming the table                                              |
| `TableKey`                                | one object per table key; `class` / `classSID` / `name`, and `quantity` / `quantityTop` row estimates |
| `TableColumn`                             | one object per stored column, joined to its `property[TableColumn]`         |
| `quantity` / `notNullQuantity` / `percentNotNull` | total values, non-`NULL` values, and the non-`NULL` share of a column |
| `sparseColumns[Table]`                    | the count of columns on the table whose `percentNotNull` is below `50`      |
| `DropColumn`                              | one object per column scheduled for deletion; `sidTable` / `sid` / `time` / `revision` |

Service actions run maintenance on a table or column.

| Action                                                      | What it does                                                             |
|-------------------------------------------------------------|--------------------------------------------------------------------------|
| `recalculateClasses[Table]` / `checkClasses[Table]`         | recalculate / verify the stored class values of the table                |
| `recalculateStats[Table]`                                   | recalculate the table statistics                                         |
| `pack[Table]`                                               | physically remove deleted rows                                           |
| `recalculateMaterializations[TableColumn]`                  | recalculate the materialized column                                      |
| `recalculateMaterializationsWithDependencies[TableColumn]` / `recalculateMaterializationsWithDependents[TableColumn]` | the same, also covering the columns it reads / the columns that read it |
| `recalculateColumnsMaterializations[Table]`                 | recalculate every materialized column of the table                       |
| `checkMaterializations[TableColumn]`                        | verify the materialized column against a fresh computation               |
| `drop[DropColumn]` / `dropAllColumns[]`                     | physically drop one scheduled column / all scheduled columns             |

`disableClasses`, `disableStatsTable` / `disableStatsTableColumn`, and `disableMaterializations` are flags that exclude a table or column from the corresponding recalculation.

### Forms

| Form                  | What it shows                                                                                   |
|-----------------------|-------------------------------------------------------------------------------------------------|
| `physicalModel`       | tables with their keys and columns, the deleted-column list, and the maintenance actions above  |
| `navigatorElements`   | the navigator tree by `parent[NavigatorElement]`                                                 |
| `forms`               | a form's group objects, its property draws, the per-user column preferences, and the groupings  |
| `properties`          | properties as a flat table and as a tree under their property groups                            |
| `actions`             | actions as a flat table and as a tree under their property groups                               |

All five forms are placed in a `metadata` navigator folder under the system `Administration` area.

### Language

- [`RECURSION` operator](../language/RECURSION_operator.md) — computes the `level` depth properties over the group and navigator trees.
- [`INTERNAL` operator](../language/INTERNAL_operator.md) — backs the physical-model service actions implemented in Java.

### See also

- [`System modules`](System_modules.md) — the general inventory of platform modules.
- [`Navigator`](Navigator.md) — the navigator tree that `NavigatorElement` mirrors.
- [`Forms`](Forms.md) — what a form is and how `Form` / `PropertyDraw` / `GroupObject` map to it.
- [`Tables`](Tables.md) — the physical tables that `Table` / `TableKey` / `TableColumn` mirror.
- [`Indexes`](Indexes.md) — table indexes.
- [`Materializations`](Materializations.md) — materialized columns recalculated by the service actions.
- [`System`](System_System.md) — the root module pulled in automatically.
- [`Authentication`](System_Authentication.md) — users and contacts, pulled in via `REQUIRE`; per-user form preferences are keyed by `CustomUser`.
- [`Security`](System_Security.md) — sets access permissions on the navigator elements, properties, and actions reflected here.
- [`Service`](System_Service.md) — service actions over the tables and materialized columns reflected here.
