---
slug: "/Print_view"
title: 'Print view'
---

Print view of the [form](Forms.md) consists of a set of templates (*reports*). When data is substituted in these templates, a *document* is formed in graphic (with pixel positioning) or pseudographic (with cell positioning) format. This document is displayed to the user using a special graphic component, inside which the user can navigate through pages, zoom in/out within pages and print the contents of the component or export it to various formats (for example, PDF or Excel). If necessary, it is possible to skip the display of the component to the user and send the document for printing or save it into a selected file in the specified format.

For each report, a set of the form [object groups](Form_structure.md#objects) that it will display is determined.

### Report hierarchy

Similar to an object group, each report has a *parent* report, so all reports form a hierarchy. The report hierarchy should:

-   include [the hierarchy of object groups](Static_view.md), i.e. if a group of objects of one report is a child of a group of objects of the other report, then the first report must match the second one or be its child as well
-   within one report, each group of objects must have exactly one child.

### Building report hierarchy {#buildhierarchy}

Based on the report hierarchy restrictions, only "chains" of object groups can be included in one report (i.e., G1, G2, G3, ... Gn, where G2 is the only linear child object of G1, G3 is the only child of G2, etc.). Thus, the decision on how to break object groups into reports comes down to whether to merge an object group with its only child (if there is one) or not. By default, such a merge is performed, however, if necessary, the developer can disable it by specifying the corresponding option (`SUBREPORT`) for a child object group.


:::info
Using this option comes down to whether to display data for a parent object group when the child object group has no data. A merged report is filled with a join of the object groups: an object collection of the parent group with no matching object collection of the child group does not appear in the document. For such object collections to be displayed, the child group is given the `SUBREPORT` option — then the child is built as a separate report, and the parent group displays all its object collections.
:::

### Report hierarchy example

The form is similar to the [example of building an object group hierarchy](Static_view.md#hierarchysample):

```lsf

FORM myForm 'myForm'
    OBJECTS A, B SUBREPORT, C, D, E
    PROPERTIES f(B, C), g(A, C)
    FILTERS c(E) = C, h(B, D)
;
```

The report hierarchy for this form is built as follows:

import ReportHierarchyEnSvg from '../images/ReportHierarchyEn.svg';

<ReportHierarchyEnSvg />

### Generating the document {#generation}

Which server builds the document depends on the client. The desktop client always builds it itself, out of the templates and the data the application server sends it. For the web client the [working parameter](Working_parameters.md) `generateReportsOnWebServer` (`false` by default) chooses between the application server, which builds the document and sends a ready file, and the web server, which is sent the templates and the data and builds the document itself, asking the application server for the classes it needs along the way. Printing into a file is always done by the application server, whatever the client.

`useShowIfInReports` (`true` by default) honours the display condition of a property in the print view: the columns of the properties whose condition is `NULL` are cut out of the template and the ones to the right of them are moved into the freed space, and, in a paginated format, stretched back to the original width of the template afterwards. The condition is taken from the first row of the report, so one that differs from row to row is decided by that row alone. With the parameter off, every column is printed.

Two parameters bound the work of the report generator itself, so that a runaway template ends with an error instead of exhausting the process that builds the document. Both values travel with the generation request, so they apply whether the document is built by the application server, by the desktop client or by the web server: `jasperReportsGovernorMaxPages` (`500` by default) bounds the number of generated pages, and `jasperReportsGovernorTimeout` (`0`) the time spent building the document, in milliseconds. `0` means that this particular bound is not applied at all, so out of the box only the number of pages is bounded.

`jasperReportsIgnorePageMargins` (`true` by default) drops the page margins of the template when the document is exported to a file, and puts one page of the document on one sheet of an Excel workbook.

`useDefaultPrinterInPrintIfNotSpecified` (`false` by default) is used by the desktop client only, and only when the document is printed without naming a printer: the print button of the print view then sends the document straight to the default printer of the operating system instead of opening its dialog. The default printer is looked up on Windows only; elsewhere the button keeps opening the dialog.

### Language

All of the above options, as well as defining the form structure, can be done using the [`FORM` statement](../language/FORM_statement.md).

### Open form

To display the form in print view, the corresponding [open form](Open_form.md) in the [print view](In_a_print_view_PRINT.md) operator is used.

### Examples

```lsf
FORM printOrder
    OBJECTS o = Order
    PROPERTIES(o) currency, customer

    OBJECTS d = OrderDetail
    PROPERTIES(d) idSku, price
    FILTERS order(d) == o
;

print (Order o) {
    PRINT printOrder OBJECTS o = o; // printing

    LOCAL file = FILE ();
    PRINT printOrder OBJECTS o = o DOCX TO file;
    open(file());

    PRINT printOrder OBJECTS o = o XLS SHEET 'encrypted' PASSWORD 'pass';
}
```
