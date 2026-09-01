---
title: 'lsFusion Brief'
slug: "/Brief"
---

## Brief on lsFusion elements for AI agents

This article is the TOP of the `brief` branch: the model in outline, plus the
map that says which article to open. It is deliberately not a short version of
those articles. What is here is enough to classify a task and answer a rough conceptual
question.

Which branch: `language` — statement / operator syntax; `paradigm` — concepts and the system-module libraries; `how-to` — task recipes. Those three are searched with `lsfusion_retrieve_docs`; when unsure which one, omit the filter and it searches all three and merges them.

`brief` and `rules` are not searched. Each is a small set of articles, named and delivered whole by `lsfusion_get_guidance`: `brief` says what an area offers, `rules` states the constraints on using it. The `rules` map, in the top `Rules` article, says when reading one of its articles becomes mandatory.

## The model

- A **class** is a set of objects. It types everything else: a property or
  action is signed by the classes of its parameters.
- A **property** computes a fact and changes nothing. It is either stored
  (`DATA`) or calculated from an expression.
- An **action** changes state. Properties say what is true; actions say how it
  evolves.
- A **form** describes objects and the properties shown for them, and the same
  description serves the interactive view, the printed one and the structured
  one.
- Changes do not reach the database as they are made. They collect in a
  **change session** until `APPLY` writes them or `CANCEL` drops them.
- A project is composed of **modules**; how data is stored and made fast is the
  physical model, and exchange with anything outside is integration.

Two misreadings worth pre-empting, because everything downstream depends on
them:

- A class does not map to a table. A table holds property values; its key
  fields hold object ids and are typed by the parameter classes of those
  properties.
- The system modules are a library of ordinary properties and actions, not
  built-in language operators. Only `System` is implicit; the rest need
  `REQUIRE` before their declarations are reachable.

## The brief articles — which one to open

Four articles, read whole with `lsfusion_get_guidance(brief='<name>')`. Match
on either column: the intent describes the task in the user's terms, the
anchors are what the code looks like.

| name | open it when the task is to | language anchors |
|---|---|---|
| `logic` | compute, store, change, aggregate, constrain or react to data | `CLASS`, `DATA`, `=`, `GROUP`, `PARTITION`, `AGGR`, `<-`, `NEW`, `FOR`, `WHILE`, `WHEN`, `<- WHEN`, `CONSTRAINT`, `NEWSESSION`, `APPLY` |
| `view` | show data to a person — a form, its layout, a menu, a report — or localize what they read | `FORM`, `OBJECTS`, `PROPERTIES`, `FILTERS`, `SHOW`, `DIALOG`, `DESIGN`, `PRINT`, `NAVIGATOR`, `WINDOW`, `{id}` captions |
| `physical` | control storage or speed, organize modules, extend or generate declarations, or rename something that already exists | `TABLE`, `MATERIALIZED`, `INDEX`, `MODULE`, `REQUIRE`, `NAMESPACE`, `EXTEND`, `META`, `@`, `migration.script` |
| `integration` | move data in or out, or let another system call in | `IMPORT`, `EXPORT`, `JSON FROM`, `EXTERNAL`, `INTERNAL`, `FORMULA`, HTTP endpoints |

Where they overlap — read both:

- a form event (`ON` inside a `FORM`) is `logic` for what it does and `view`
  for where it lives;
- import or export of a FORM is `integration` for the exchange and `view` for
  the form it goes through;
- a `MATERIALIZED` property is `logic` for what it computes and `physical` for
  what that costs;
- extending an element is `physical` for the mechanism and the area of the
  extended element for the meaning.

Running the result — launch settings, the user interface, the
[security policy](../paradigm/Security_policy.md), the scheduler, backups,
monitoring, logs, the profiler — has no brief article. Search the `paradigm`
branch for it.

## Mini map for AI

1. `CLASS` → object type (a set of objects).
2. `= DATA` or `=` expression → property.
3. `{ ... }` without `=` → action (imperative).
4. `FORM` → UI / query / report definition.
5. `SHOW / DIALOG / PRINT` → open or print a form.
6. `EXPORT / IMPORT` → external formats for properties and forms.
7. `GROUP / PARTITION` → value aggregates.
8. `GROUP AGGR / AGGR` → aggregated objects.
9. `NEWSESSION / APPLY / canceled()` → session control.
10. `EXTEND / ABSTRACT / += / +{` → extension and polymorphism points.
11. `WHEN`, `<- WHEN` → events on a data change; `ON` inside a `FORM` → a form
    event.
12. `Utils` / `Time` / `System` / `Authentication` → system modules; string,
    number and date functions, `currentUser`, `currentDate` live there.
