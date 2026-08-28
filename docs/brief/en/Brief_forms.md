---
slug: "/Brief_forms"
title: 'Brief: forms'
---

## Form structure

A form is the universal element describing which objects and which properties for them are read. It is declared by the [`FORM` statement](../language/FORM_statement.md) and consists of blocks:

- `OBJECTS` — the form's objects with their classes; objects in one pair of parentheses form an *object group*, whose sets are their cartesian product;
- `PROPERTIES` — the properties and actions the form's objects are passed to;
- `FILTERS` — condition properties: only the object sets with a non-`NULL` value remain;
- `ORDERS` — the ordering of the object sets;
- `FILTERGROUP` — filters the user picks one active filter from;
- `TREE` — several object groups shown as one tree table.

```lsf
FORM orders
    OBJECTS o = Order
    PROPERTIES(o) date, number
    OBJECTS d = OrderDetail
    PROPERTIES(d) nameSku, quantity
    FILTERS order(d) = o
;
```

**Analogy**: a `SELECT` over several row sets at once, each with its own selection. In detail — [Form structure](../paradigm/Form_structure.md).

## Interactive view

In the [interactive view](../paradigm/Interactive_view.md) the user works with the open form: picks current objects, changes data properties, runs actions. Data is read as needed and changes are visible immediately. An object group is shown as a table or a panel, and only the groups listed in a `TREE` block are joined into one tree; the user can add filters and orderings of their own.

A property is displayed in exactly one object group — its *display group*, by default the last group whose objects it takes as arguments. A property that has no display group at all — one with no parameters, for instance — goes into the form's top-level `PANEL`, or into `TOOLBARBOX` in the `TOOLBAR` view.

Which view is used is set not in the form but in the operator that opens it, so one form serves all of them. Where the components land on screen is the [form design](../paradigm/Form_design.md), see [Brief: form design](Brief_design.md).

## Print view

The [print view](../paradigm/Print_view.md) is one of the two [static](../paradigm/Static_view.md) ones: all data is read at the moment the form is opened, with no feedback. The object groups are arranged into a hierarchy — a parent group's properties are output once for each of its own object sets, with the matching child sets under it, and then again for the next one. A file comes out of it only when a format is given; without one the report is rendered interactively — shown to the user, sent to the printer, or delivered as a message. The object groups are arranged into a hierarchy — the properties of a parent group are output once, those of a child group for all of its object sets.

It produces **PDF**, **DOC**, **DOCX**, **XLS**, **XLSX**, **HTML**, **RTF**: a preview, printing on a printer, or a file. The layout is described by a [report design](../paradigm/Report_design.md) — a `.jrxml` template found by the form's canonical name; where no template is found the platform generates an automatic design from the form structure ([Brief: reports](Brief_reports.md)).

## Structured view

The [structured view](../paradigm/Structured_view.md) is the second static view, beside the print one: it reads all the data at the moment the form opens and takes the hierarchy of its object groups — a parent group's properties once for each of ITS OWN object sets, with the matching child sets nested under it — but writes it as data rather than as a page — **JSON**, **XML**, **CSV**, **DBF**, **XLS**, **XLSX**, **TABLE**.

A form is both the shape the data is written in and the shape it is read back from. Reading back takes the same formats, except that Excel has one keyword, `XLS`, covering both `.xls` and `.xlsx` files. What each direction means is in [Brief: data export](Brief_export.md) and [Brief: data import](Brief_import.md).

## Opening a form (SHOW, DIALOG, PRINT, EXPORT)

The form opening operator creates an action that opens the form in the chosen view.

| Operator | View | Purpose |
| -------- | ---- | ------- |
| [`SHOW`](../language/SHOW_operator.md) | interactive | show the form to the user |
| [`DIALOG`](../language/DIALOG_operator.md) | interactive | the same plus returning the current value of an object as the entered value |
| [`PRINT`](../language/PRINT_operator.md) | print | a report: preview, printing, or a file |
| [`EXPORT`](../language/EXPORT_operator.md) | structured | exporting the form's data to a file |

The [`IMPORT` operator](../language/IMPORT_operator.md) does not open the form: it parses a file and writes the values into the form's properties, so that exporting the form back would recreate that file ([form import](../paradigm/In_a_structured_view_EXPORT_IMPORT.md#importForm)).

```lsf
selectSku (OrderDetail d) { DIALOG skus OBJECTS s INPUT DO sku(d) <- s; }
printOrder (Order o) { PRINT printOrder OBJECTS o = o XLSX TO orderFile; }
```

## Passing objects and the opening mode

The `OBJECTS` block of the opening operator passes object values to the form: the passed value becomes the current object in the interactive view, and a filter for equality to it in a static one. By default a `NULL` among the passed values cancels the action; the `NULL` keyword after the value allows it, and a `DIALOG` object marked `INPUT` or `CHANGE` allows it automatically.

`SHOW` and `DIALOG` also set the form location — `FLOAT`, `DOCKED`, `EMBEDDED`, `POPUP`, `IN`. `SHOW` takes `WAIT` / `NOWAIT`; a `DIALOG` has no such option and runs synchronously whenever its result is consumed — by a `DO` / `ELSE` continuation or by a `CHANGE` write-back.

In detail — [Opening a form](../paradigm/Open_form.md). A form is also opened by picking a [navigator](../paradigm/Navigator.md) item, see [Brief: navigator](Brief_navigator.md).
