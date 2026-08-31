---
slug: "/Brief_physical"
title: 'Brief: physical model'
---

- [Execution](#execution)
- [Modules](#modules)
- [Metaprogramming](#metaprogramming)
- [Extensions](#extensions)
- [Element identification](#element-identification)

## Execution

### Stored data and materializations

The database stores [data (`DATA`) properties](../paradigm/Data_properties_DATA.md) — all but the [local](../paradigm/Data_properties_DATA.md#local) ones, which keep their values only within the [session](../paradigm/Change_sessions.md) — and calculated properties marked with the [`MATERIALIZED` option](../language/Property_options.md#persistent): their values sit in a table field, are updated automatically when the data they depend on changes, and are read straight from the database. Separate fields store an object's belonging to a class (`_CLASS_TableName`) and the table fullness flag (`_FULL_TableName`) — the latter only where fullness is not already guaranteed by what the table stores: a table holding class belonging for all descendants of its key class is marked full by itself and gets no such field.

A property can be materialized if and only if the number of object sets with a non-`NULL` value is finite. Materialization moves the cost from reading to writing: the value is not computed on every read, but is updated on every change of the source data. The mechanism is described in [materializations](../paradigm/Materializations.md).

```lsf
sum = GROUP SUM sum(OrderDetail od) BY order(od) MATERIALIZED;
```

**Analogy**: a materialized database view, refreshed as soon as the source data changes.

### Tables and database names

A table is declared by the [`TABLE` statement](../language/TABLE_statement.md):

```
TABLE name [dbName] (className1, ..., classNameN) [FULL | NODEFAULT];
```

The classes set the key fields `key0`, ..., `key(N-1)` — one per class, numbered from zero — the remaining fields hold property values. A property's table is set by the `TABLE` option; without it the property goes into the table closest by key classes, `NODEFAULT` excludes a table from that choice, and with no suitable table an `_auto_...` table is created. `FULL` means the table contains all objects of its key classes, and affects only query execution. The mechanism is described in [tables](../paradigm/Tables.md).

Names depend on the [naming policy](../paradigm/Launch_parameters.md#namingpolicy) (`db.namingPolicy`):

| Policy | Table | Field |
| --- | --- | --- |
| With signature (default) | `NameSpace_TableName` | `NameSpace_PropertyName_Class1_..._ClassN` |
| Without signature | `NameSpace_TableName` | `NameSpace_PropertyName` |
| Short | `TableName` | `PropertyName` |

### Indexes

An index on one property is created by the [`INDEXED` option](../language/Property_options.md#indexed), an index on an arbitrary list of fields of one table by the [`INDEX` statement](../language/INDEX_statement.md):

```
INDEX [dbName] [indexType] field1, ..., fieldN;
```

Only materialized properties can be indexed. A composite index takes both materialized properties and parameters referring to key fields; it must contain at least one materialized property, and all properties in it must be stored in one table and use the same set of parameters. The `LIKE` and `MATCH` types keep the usual index and try to add specialized ones — `LIKE` adds a `LIKE` index, `MATCH` on a string field adds both a `MATCH` and a `LIKE` one — which on string fields happens only when the current DB adapter has trigram / full-text support enabled, while on a single `TSVECTOR` field `MATCH` creates the specialized GIN index alone. A unique index on all keys of a table and indexes on the key suffixes `keyK`, ..., `keyN` are created automatically. The mechanism is described in [indexes](../paradigm/Indexes.md).

```lsf
orderDate = DATA DATE (Order) INDEXED;
INDEX supplier(Sku s, DATE d), s, price(s, d), d;
```

### Recomputing materializations

The [`RECALCULATE` operator](../language/RECALCULATE_operator.md) creates an action that recomputes the stored values of a materialized property from its definition:

```
RECALCULATE [CLASSES | NOCLASSES] propertyId(expr1, ..., exprN) [WHERE whereExpr]
```

Recomputing is reached for when the stored values may have diverged from the definition — after the property definition changed or after a direct data fix in the database. `WHERE` limits the argument sets to recompute, `CLASSES` recomputes only the class data, `NOCLASSES` only the values.

```lsf
recalculateSum() {
    RECALCULATE sum(Order o);
}
```

## Modules

### Modules and order

A *module* is a functionally complete part of a project: declarations of classes, properties, actions, forms, events, constraints ([modules](../paradigm/Modules.md)). One module is one `.lsf` file starting with a [module header](../language/Module_header.md): `MODULE`, `REQUIRE`, `PRIORITY`, `NAMESPACE`.

`REQUIRE` lists the modules the current one [depends](../paradigm/Modules.md#depends) on. The dependency is transitive, cycles are not allowed, and the initialization order is built from it: a module is initialized after all of its dependencies. Every module depends on the `System` module. The dependency also governs visibility: an element can only be found by name in a module that depends on it, so extending someone else's functionality is what the [extension](../paradigm/Extensions.md) technique is for, see [Brief: extensions](Brief_physical.md#extensions).

A [project](../paradigm/Projects.md) is the set of modules and the accompanying files; by default every `.lsf` file on the application server's classpath is taken to be a module, and the `logics.includePaths`, `logics.topModule` and `logics.orderDependencies` launch parameters narrow that set and override the order.

**Analogy**: a package or an assembly.

```lsf
MODULE Sale;
REQUIRE System, Utils, Item;
NAMESPACE Sale;
```

### System modules

*System modules* are shipped with the platform — the standard library, which a project pulls in with `REQUIRE` and does not redefine ([system modules](../paradigm/System_modules.md)). The platform loads twelve of them itself — `System`, `Service`, `Reflection`, `Authentication`, `Security`, `SystemEvents`, `Email`, `Icon`, `Scheduler`, `Time`, `Utils`, `UserEvents` — but being loaded is not being depended on: only `System` is an implicit dependency of every module, and a declaration from any of the others needs a `REQUIRE`.

| Module | What for |
| --- | --- |
| [`System`](../paradigm/System_System.md) | root types, base classes, infrastructure |
| [`Utils`](../paradigm/System_Utils.md) | general-purpose helper properties and actions |
| [`Time`](../paradigm/System_Time.md) | date and time properties and operations |
| [`Authentication`](../paradigm/System_Authentication.md) | users, contacts, sign-in |
| [`Security`](../paradigm/System_Security.md) | roles and access policies |
| [`Service`](../paradigm/System_Service.md) | service actions and server settings |
| [`SystemEvents`](../paradigm/System_SystemEvents.md) | server-lifecycle events |
| [`UserEvents`](../paradigm/System_UserEvents.md) | programmatic access to a form's filters and orders |
| [`Reflection`](../paradigm/System_Reflection.md) | metadata about the navigator, forms, properties, tables |
| [`Scheduler`](../paradigm/Scheduler.md) | scheduled actions |
| [`Email`](../paradigm/System_Email.md) | sending and receiving email |
| [`Icon`](../paradigm/System_Icon.md) | UI icon catalogue |

Auxiliary modules: `Backup`, `Chat`, [`Eval`](../paradigm/Eval_EVAL.md), `Excel`, `Document` / `Word`, `Image` / `OpenCV`, `I18n`, `Integration`, `MasterData`, [`Numerator`](../paradigm/Utils_Numerator.md), `Hierarchy`, `Historizable`, `Geo`, `Printer` / `QZTray` / `Sound` / `Com`, `ProcessMonitor` / `Profiler`, `RabbitMQ` / `WebSocket`, `Messenger` with its `Telegram` / `Slack` / `Viber` / `Whatsapp` / `Skype`, `SQLUtils`, `DefaultData`, `Schedule`.

All of this is ordinary `.lsf` declarations, not language primitives: `lpad`, `currentDate`, `currentUser` are properties, and they should be searched for in the `paradigm` branch.

## Metaprogramming

### META and calling metacode (@)

*Metacode* is a block of lsFusion code with parameters that produces other code when used. It is declared by the [`META` statement](../language/META_statement.md) — the name, the parameter list, a sequence of statements, the closing `END` — and used by the [`@` statement](../language/commat_statement.md), which names the metacode and passes the arguments: every metacode parameter is replaced with the argument passed in all the places it is used.

Besides substitution, the metacode has operations on tokens: `##` concatenates two adjacent tokens into one, and `###` does the same while converting the first character of the second token to uppercase — except when everything to its left came out empty and the token itself does not start with `###`, where the capitalization is skipped, so `prefix###name` with an empty `prefix` yields `name`, not `Name`. They are what the names of the generated elements are built from. The mechanism is described in [metaprogramming](../paradigm/Metaprogramming.md).

**Analogy**: a macro with textual substitution of parameters.

```lsf
META objectProperties(object, caption)
    object##Name 'Name of '##caption = DATA BPSTRING[100](object);
    object##Value 'Value of '##caption = DATA INTEGER (object);
END

@objectProperties(Document, 'the document');   // DocumentName 'Name of the document' = ...
```

### Generated code and call arguments

The body of a metacode consists of module-level [statements](../language/Statements.md), and the `@` statement itself is written at module level: a metacode produces declarations of system elements, not operators inside an action body.

Hence the two typical uses: a family of same-shaped declarations for a class or a prefix passed in, and a set of elements added to an arbitrary form through `EXTEND FORM` with the form name as a parameter ([Brief: extensions](Brief_physical.md#extensions)).

An argument of the call can be a composite ID, a class identifier, a literal, or an empty parameter — but not the value of a property: choosing behaviour by data is done not with a metacode but with abstract properties and actions.

The IDE writes the produced code after the `@` call in braces so that navigation and analysis work on it; the platform ignores that block and generates the code anew, so it must not be edited by hand.

## Extensions

### Extending classes

The [`EXTEND CLASS` statement](../language/EXTEND_CLASS_statement.md) inherits an already declared class from further parent classes and adds new [static objects](../paradigm/Static_objects.md) to it.

Structurally this moves the relations between classes into a separate module: the base module declares the class, and a module depending on it adds a parent, changing nothing in the base one. The mechanism is described in [class extension](../paradigm/Class_extension.md).

```lsf
CLASS Box : Shape;
CLASS Quadrilateral;
EXTEND CLASS Box : Quadrilateral;   // adding inheritance

EXTEND CLASS ShapeType {            // adding a static object
    circle 'Circle'
}
```

### Extending properties and actions

An abstract property or action is a declared extension point: the base module sets the contract with the `ABSTRACT` operator — the parameter classes, the way an implementation is chosen, and the result class, which a property always has and an action only when the operator names one — and other modules add implementations with the [`+=` statement](../language/plus_equals_statement.md) for properties and the [`ACTION+` statement](../language/ACTION_plus_statement.md) for actions. The kinds of selection and the options are covered in [Brief: properties](Brief_logic.md#properties).

Structurally this is the deferred assembly of an ordinary operator: for a property, of a [selection operator](../paradigm/Property_extension.md); for an action, of a [branching or sequence operator](../paradigm/Action_extension.md). The implementations are kept in an ordered list, and a new one is added at its start or at its end.

Hence the main modularity technique: the base module declares the extension point, the modules depending on it add behaviour, and no reverse dependency appears.

### Extending forms

The [`EXTEND FORM` statement](../language/EXTEND_FORM_statement.md) extends a form declared in another module — with the same blocks as the [`FORM`](../language/FORM_statement.md) declaration (see [Brief: forms](Brief_view.md#forms)): objects, the properties and actions shown for them, filters, orders. Separate extension blocks change the elements already on the form; the form's [design](../paradigm/Form_design.md) is likewise set from outside.

An added element can be placed before or after a specific element of the form, or at the start or at the end; for objects this position sets their place in the order of object groups, which a property's display group and the object group a filter applies to depend on. The mechanism is described in [form extension](../paradigm/Form_extension.md).

```lsf
EXTEND FORM items
    PROPERTIES(i) NEWSESSION DELETE     // a delete button
    OBJECTS g = ItemGroup BEFORE i      // the item group before the item
    PROPERTIES(g) READONLY name
    FILTERS itemGroup(i) = g
;
```

There is no separate extension logic for the navigator and the form design: these constructs are extensible by definition ([extensions](../paradigm/Extensions.md)).

## Element identification

### Identifiers and namespaces

The named system elements are properties, actions, user classes, forms, navigator elements, groups of properties and actions, windows, tables, metacodes: each of them is accessed by its name ([element identification](../paradigm/Element_identification.md)).

An element is created in a *namespace*, which the [module](../paradigm/Modules.md) sets with the `NAMESPACE` statement (by default the module's name), while the `PRIORITY` statement lists additional namespaces that take precedence when elements are searched for; both belong to the [module header](../language/Module_header.md).

An access is written as a [simple identifier](../language/IDs.md#id) — `name` — or as a compound one with an explicit namespace — `Sale.Document`. The string `<namespace>.<name>` is the element's *full name* ([naming](../paradigm/Naming.md#namespace)).

**Analogy**: packages and imports, except that the priority namespace is stated once in the module header.

```lsf
MODULE Sale;
REQUIRE System, Utils;
NAMESPACE Sale;

CLASS Document 'Document';   // full name — Sale.Document
```

### Naming

[Naming](../paradigm/Naming.md) recommends starting the name of a system element with a lowercase letter, the name of a class with an uppercase one, and every next word in a name with an uppercase one: `myFirstName`, `MySuperClass`; the [identifier](../language/IDs.md#id) grammar itself does not restrict the case. Uniqueness is required among the elements of one type, not across the whole system: a class and a form may carry the same full name, and so may a property and an action. Within one type there are exceptions too — metacodes may share a full name when they take a different number of parameters, properties and actions when their signatures differ — so a property's full name is not unique on its own.

A *[canonical name](../paradigm/Naming.md#canonicalname)* is the string that uniquely identifies an element among the elements of the same type. For classes, property groups, navigator elements, windows and tables it is the full name; for properties and actions the signature is appended to it — the canonical names of the parameter classes in square brackets.

```
Sale.Document                  // class
Item.gender[Item.Article]      // property: full name and signature
```

The canonical name is what ties an element to its security policy settings and to reflection data. For a property stored in the database it also gives the default name of its field, which the developer can set explicitly instead.

### Finding elements

Given a short name, an element is located by the [search algorithm](../paradigm/Search_.md), which takes into account: the name itself (case-sensitive), module dependency — the candidate must be declared in a module the searching module depends on — the explicitly stated namespace, and in its absence the precedence of the module's own namespace and of the namespaces from `PRIORITY`.

For properties and actions the parameter classes are added to that: the candidates that fit are the ones whose signature matches the classes of the arguments, and the most concrete of them is chosen. The signature can be given in the access itself: `name[AClass](b)`.

If several candidates fit, or none does, the platform raises an error at startup.

### Renaming and migration.script

Renaming an element or moving it to another namespace changes its canonical name, and thereby breaks the element's tie to the data and settings already stored. The correspondence between the old and the new name is written in the `migration.script` file on the application server's CLASSPATH: `V<version number> { ... }` blocks, of which the ones with a version higher than the one stored in the database are applied at startup. The mechanism is described in [migration](../paradigm/Migration.md).

| Change type | What it preserves |
| --- | --- |
| `PROPERTY` | security policy and reflection settings for a property or an action |
| `STORED PROPERTY` | the same, plus renaming the field in the database, that is, the data of a data property |
| `FORM PROPERTY` | table settings for a property or an action on a form |
| `CLASS` | the objects of a user class and the data related to them — but not the canonical names of the data properties that take the class in their signature: those change with it, are not tracked automatically, and need `STORED PROPERTY` entries of their own, or the server renames the orphaned column to `_DELETED_` plus its old database name — dropping it outright if a column by that name is already there — and the new property starts out empty |
| `OBJECT` | the data related to a static object |
| `TABLE` | renaming the table instead of copying the records over |
| `NAVIGATOR` | security policy settings for a navigator element |

```
V0.3.1 {
    STORED PROPERTY Item.gender[Item.Article] -> Item.dataGender[Item.Article]
    CLASS Date.DateInterval -> Date.Interval
}
```
