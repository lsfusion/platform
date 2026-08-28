---
slug: "/Brief_reports"
title: 'Brief: reports'
---

## The print view

A *report* is a template that a document is built from by substituting data into it. A form's [print view](../paradigm/Print_view.md) consists of a set of reports, each given its own set of the form's [object groups](../paradigm/Form_structure.md#objects); how the groups are split between reports and how the `SUBREPORT` option on a group makes it a separate report — a *subreport* — is described in [building the report hierarchy](../paradigm/Print_view.md#buildhierarchy).

What happens to the document is chosen by the options of the [`PRINT` operator](../language/PRINT_operator.md), which come in two forms. `MESSAGE` shows the form to the user as a message: the properties of the root group become its header, and the table under it comes from the FIRST child group — a form with no child group gives an empty table, and every child group after the first is left out. The other form is the [interactive](../paradigm/In_a_print_view_PRINT.md#interactive) one: `previewMode` is either `PREVIEW`, the default, where the report is shown to the user, or `NOPREVIEW`, where it goes straight to the printer (`TO <expression>` picks the printer), while a format — `PDF`, `DOC`, `DOCX`, `XLS`, `XLSX`, `HTML`, `RTF` — together with `TO <property>` writes the document into a file on the server. `SHEET` and `PASSWORD` are available for `XLS` and `XLSX`.

```lsf
FORM printOrder
    OBJECTS o = Order
    PROPERTIES(o) date, nameCustomer

    OBJECTS d = OrderDetail SUBREPORT
    PROPERTIES(d) nameSku, quantity
    FILTERS order(d) = o
;

print (Order o) { PRINT printOrder OBJECTS o = o DOCX TO orderFile; }
```

## Report design

The [report design](../paradigm/Report_design.md) is how a report is laid out in the document. A report is described by a [template](../paradigm/Report_design.md#template) — a [`.jrxml` file](../paradigm/Report_design.md#format) of the JasperReports technology that the platform looks up in the server classpath by a name derived from the form's canonical name; the fields of the template are the form's properties, and their names and types match. The exact naming and what a wrong name costs are in [Rules: reports](../rules/Rules_reports.md).

A report need not have a template of its own: if at least one is not found, the platform builds an [automatic design](../paradigm/Report_design.md#auto) from the form structure. A template can also be set explicitly, by a property holding either a file name or the template file itself: the `REPORT` block of the [`FORM` statement](../language/FORM_statement.md) for the top report, `REPORTS` (the synonym `REPORTFILES`) for the listed object groups, and the expression after `SUBREPORT` in an [object block](../language/Object_blocks.md) for that group.
