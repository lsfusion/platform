---
title: 'lsFusion Brief'
slug: "/Brief"
---

## Brief on lsFusion elements for AI agents

Format: **very concise**, for understanding and code generation. Detailed description and syntax is retrieved with tools via RAG in docs.

Which branch: `language` — statement / operator syntax; `paradigm` — concepts and the system-module libraries; `how-to` — task recipes. Those three are searched with `lsfusion_retrieve_docs`; when unsure which one, omit the filter and it searches all three and merges them.

`brief` and `rules` are not searched. Each is a small set of articles, named and delivered whole by `lsfusion_get_guidance`: `brief` says what an area offers, `rules` states the constraints on using it. The `rules` map, in the top `Rules` article, says when reading one of its articles becomes mandatory.

## The brief articles — what each area covers

This article is the overview; the detail per area is in the four
articles below, read whole with `lsfusion_get_guidance(brief='<name>')`.

| name | covers |
|---|---|
| `logic` | properties, actions, events, constraints, change sessions |
| `view` | forms, form design, navigator, reports, internationalization |
| `physical` | execution (tables, materializations, indexes), modules, extensions, metaprogramming, element identification |
| `integration` | data import, data export, calling in and out |

## Reading an area's brief (RECOMMENDED)

1. What is listed above is the name to ask for, not a summary to reason
   from. This article does not contain those four articles.

2. The first time a task touches an area, the assistant SHOULD read that
   area's brief before choosing a construct. It is short, and it is what
   prevents inventing a mechanism the platform already has.

3. The brief says WHAT exists. For syntax, concepts and recipes the
   assistant SHOULD use `lsfusion_retrieve_docs` on the `language`,
   `paradigm` and `how-to` branches.

4. Not reading a brief article is not a rule violation. Writing code for
   an area whose `rules` article was not read is — see the top `Rules`
   article.

---

## Domain logic — the core elements (from simpler to more complex)

### Classes
- **Description**: base element — a set of objects. Defines object types; used in the signatures of properties and actions, and as the classes of a form's objects. Inheritance (may be multiple). Built-in vs user classes. Polymorphism via inheritance, `ABSTRACT` + `+=` / `ACTION+`, `MULTI`.
- **Analogy**: OOP classes, with multiple dispatch by parameter classes.
- **Statement** ([`CLASS`](../language/CLASS_statement.md)): `CLASS ABSTRACT name [caption] [imageSetting] [: parent1, ..., parentN];` and `CLASS [NATIVE] name [caption] [imageSetting] [{ objectName1 [objectCaption1] [imageSetting], ... }] [: parent1, ..., parentN];` for a class with static objects

### Properties
- **Description**: compute facts, do not change state. DATA (stored in the database, unless declared `LOCAL` — those live in the session) vs calculated (formulas, aggregates, compositions).
- **Analogy**: math / pure functions. Declarative, close to SQL.
- **Statement** ([`=`](../language/=_statement.md)): `name [caption] [(param1, ..., paramN)] = expression [options];`; a stored one is `= DATA [LOCAL [NESTED [MANAGESESSION | NOMANAGESESSION]]] returnClass [(argumentClass1, ..., argumentClassN)]` ([`DATA`](../language/DATA_operator.md)).

### Actions
- **Description**: change state (DB or external). Dual to properties: properties = what; actions = how it evolves.
- **Analogy**: procedures / methods. Imperative, close to Java.
- **Statement** ([`ACTION`](../language/ACTION_statement.md)): `name [caption] [(param1, ..., paramN)] { actionBody } [options]`
- **Change sessions**: changes do not reach the database as they are made — they collect in the current session until `APPLY` writes them or `CANCEL` drops them; `NEWSESSION` and `NESTEDSESSION` run an action against a session of its own. In a nested session `APPLY` copies the changes to the parent instead of writing them, and during an apply transaction no session is created at all — the action is deferred into the current one.

### Events
- **Description**: time-dependent reactions — a global event runs an action at a moment in the session's life, most often a data change; a form event runs one at a point in the form's life — `INIT`, `APPLY`, `CANCEL`, `CLOSE`, `DROP` — on what the user does, from a key to a tab or a row selection, or on a timer with `ON SCHEDULE`. Main statement `WHEN`; `<- WHEN` declares a calculated event: the value is derived when the property is read, and an explicit write to it wins; session-change analyzers `CHANGED`, `SET`, `DROPPED`, `PREV`, etc. Form events are attached with `ON`: `ON CHANGE`/`ON EDIT`/`ON CONTEXTMENU`/`ON GROUPCHANGE`/`ON CHANGEWYS`. The `BEFORE` / `AFTER` statements add an aspect action that runs before / after another action.
- **Analogy**: DB triggers but broader.
- **Statements** ([`WHEN`](../language/WHEN_statement.md), [`ON`](../language/ON_statement.md)): `WHEN eventClause eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;` and `ON eventClause eventAction;`

### Constraints
- **Description**: time-independent invariants over the database. Checked at the event given by `eventClause`; with none given, on `APPLY`. Kinds: general (`CONSTRAINT`), simple — the consequence `=>`, which takes a `RESOLVE [LEFT] [RIGHT]` clause telling the platform which side to fix automatically, and the definiteness `NONULL [DELETE]`, which has no such clause; the uniqueness of aggregated objects is added by `GROUP AGGR` / `AGGR`.
- **Analogy**: `CHECK` and `NOT NULL` in SQL, except that the condition is an arbitrary property over the whole database.
- **Statement** ([`CONSTRAINT`](../language/CONSTRAINT_statement.md)): `CONSTRAINT [eventClause] constraintExpr [CHECKED [BY propertyId1, ..., propertyIdN]] MESSAGE messageExpr [PROPERTIES outExpr1, ..., outExprM];`

### Aggregations
- **Description**: aggregated objects that the platform creates and deletes by rule — `AGGR` creates the object when the aggregated expression becomes not `NULL` and deletes it when it becomes `NULL` again, acting on changes rather than on data that already exists; `GROUP AGGR` returns the object of the group. See [Brief: properties](Brief_logic.md#properties) and [Brief: constraints](Brief_logic.md#constraints).
- **Operator** ([`AGGR`](../language/AGGR_operator.md)): `AGGR [eventClause] aggrClass WHERE aggrExpr [NEW [newEventClause]] [DELETE [deleteEventClause]]`

## View logic — the same model, shown to a user or to another system

### Forms
- **Description**: universal data/UI element. `OBJECTS` (groups), `PROPERTIES` (what to show / actions), `FILTERS` (row filter). Views: interactive (`SHOW`), print (`PRINT`), structured (`EXPORT`/`IMPORT`). Extensible via `EXTEND FORM`.
- **Analogy**: SQL query but broader (many tables at once).
- **Statement** ([`FORM`](../language/FORM_statement.md)): `FORM name [caption] formOptions formBlock1 ... formBlockN;`
- **Design** (`DESIGN`): the interactive view is laid out from components the platform generates out of the form structure; `DESIGN` moves, hides and configures them.

### Navigator and windows
- **Navigator (`NAVIGATOR`)**: menu / routing binding forms and actions.
- **Windows (`WINDOW`)**: areas of the screen, each showing its own navigator elements.

## Physical model — how a project is built and how it runs

Three parts: development — the modules a project is made of and everything about writing them; execution — telling the platform how to store and process the data so it performs; management — running the result.

### Modules
- **Description**: unit of reuse (one `.lsf` file); contains classes/properties/actions/forms/events/constraints; dependencies via `REQUIRE`.
- **Analogy**: package / assembly.
- **Statement** ([module header](../language/Module_header.md)): `MODULE name;` `[REQUIRE moduleName1, ..., moduleNameN;]` `[PRIORITY namespaceName1, ..., namespaceNameM;]` `[NAMESPACE namespaceName;]`

### System modules (standard library)

The platform loads twelve of them itself, `System`, `Utils`, `Time`, `Authentication` among them — but loading is not depending: `REQUIRE` is what makes a module's declarations reachable, and only `System` is implicit. Which twelve those are and what each module is for is in [Brief: modules](Brief_physical.md#modules).

### Development and integration
- **Identification, naming, migration**: an element is named by `<namespace>.<name>`, and a property or action by that plus the signature of its parameter classes; renaming it, or moving it to another namespace, changes that name — moving it to another module under the same `NAMESPACE` does not — and `migration.script` carries what a given kind of change preserves — a `PROPERTY` entry keeps the settings, the DATA of a property needs `STORED PROPERTY`.
- **Extensions**: an already declared class, property, action or form is extended without editing its declaration — usually from another module, which is how a project is composed out of modules, but within one module too, where it is a way to order initialization.
- **Metaprogramming** (`META`, `@`): parameterized metacode generates repeated declarations instead of copying them.
- **Integration**: four directions — an external system calls in over HTTP; an internal one calls in through the Java API or straight against the SQL tables; `EXTERNAL` calls out to an external system; `INTERNAL` and `FORMULA` call out to an internal one, in the same JVM or database.
- **Internationalization**: a caption is plain text unless it carries an `{id}` fragment, which is resolved against the ResourceBundle files of the current locale; reverse translation can insert that id for the author. User data is not translated.

### Execution — telling the platform how to store and process
- **Materializations** (`MATERIALIZED`): a calculated property can be stored in a table field instead of computed on read; the platform keeps it up to date as the data it depends on changes, and recomputes it on demand when the definition itself changed.
- **Tables (`TABLE`)**: where stored values live — non-local `DATA` properties and materialized calculated ones; declared by the classes of its keys, and such a property without an explicit table is placed automatically.
- **Indexes (`INDEX`)**: indexes on materialized properties and table key fields; the indexes on a table's own keys are created automatically.

### Management — running the result
- Launch parameters and working settings, the user interface, the [security policy](../paradigm/Security_policy.md), the interpreter, the scheduler, backups, monitoring, logs, the profiler and the chat. No brief article of its own: retrieve these from the `paradigm` branch.

## Mini map for AI
1. `CLASS` → object type (a set of objects). A class is not mapped to a table: a table holds property values, its key fields hold object ids, and the parameter classes are what those key fields are typed by — a property without an explicit `TABLE` goes to the table whose key classes fit it.
2. `= DATA` or `=` expression → property.
3. `{ ... }` without `=` → action (imperative).
4. `FORM` → UI/query/report definition.
5. `SHOW / DIALOG / PRINT` → open/print form.
6. `EXPORT / IMPORT` → external formats for properties/forms.
7. `GROUP / PARTITION` → value aggregates.
8. `GROUP AGGR / AGGR` → aggregated objects.
9. `NEWSESSION / APPLY / canceled()` → transaction/session control.
10. `EXTEND / ABSTRACT / += / +{` → extension & polymorphism points.
11. `WHEN`, `<- WHEN` → events on a data change; `ON` inside a `FORM` → a form event — a point in the form's life, a user action, or `ON SCHEDULE` on a timer.
12. `Utils` / `Time` / `System` / `Authentication` → standard library (system modules); string, number and date functions, `currentUser`, `currentDate` live there.
