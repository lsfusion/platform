---
slug: "/Brief_identification"
title: 'Brief: element identification'
---

## Identifiers and namespaces

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

## Naming

[Naming](../paradigm/Naming.md) recommends starting the name of a system element with a lowercase letter, the name of a class with an uppercase one, and every next word in a name with an uppercase one: `myFirstName`, `MySuperClass`; the [identifier](../language/IDs.md#id) grammar itself does not restrict the case. Uniqueness is required among the elements of one type, not across the whole system: a class and a form may carry the same full name, and so may a property and an action. Within one type there are exceptions too — metacodes may share a full name when they take a different number of parameters, properties and actions when their signatures differ — so a property's full name is not unique on its own.

A *[canonical name](../paradigm/Naming.md#canonicalname)* is the string that uniquely identifies an element among the elements of the same type. For classes, property groups, navigator elements, windows and tables it is the full name; for properties and actions the signature is appended to it — the canonical names of the parameter classes in square brackets.

```
Sale.Document                  // class
Item.gender[Item.Article]      // property: full name and signature
```

The canonical name is what ties an element to its security policy settings and to reflection data. For a property stored in the database it also gives the default name of its field, which the developer can set explicitly instead.

## Finding elements

Given a short name, an element is located by the [search algorithm](../paradigm/Search_.md), which takes into account: the name itself (case-sensitive), module dependency — the candidate must be declared in a module the searching module depends on — the explicitly stated namespace, and in its absence the precedence of the module's own namespace and of the namespaces from `PRIORITY`.

For properties and actions the parameter classes are added to that: the candidates that fit are the ones whose signature matches the classes of the arguments, and the most concrete of them is chosen. The signature can be given in the access itself: `name[AClass](b)`.

If several candidates fit, or none does, the platform raises an error at startup.

## Renaming and migration.script

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
