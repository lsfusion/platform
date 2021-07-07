---
title: 'In a print view (PRINT)'
---

This operator creates an action that [opens a form](Open_form.md) in the [print](Print_view.md) view.

### Format {#format}

In this operator, you can specify the format that form data will be converted to before being shown to the user:  **DOC**, **DOCX**, **XLS**, **XLSX**, **PDF**, **HTML**, **RTF**.

By default, if the format is specified in the print view, when the client receives a file in a specified format, it attempts to open this file using standard OS means (that is, using the program associated with the specified format). If necessary, however, in addition to the format, you can specify a property that will contain a file with the generated report. In this case, no data will be sent to the client and all data processing will be done exclusively on the server.

### Interactive print view {#interactive}

Apart from the export in a specified format, the print view allows to display information to the user in the following *interactive* modes:

-   `PREVIEW` - the form on the client side will be shown in a new preview window, so that the user can decide on the format to export this form to or send it to the printer. This mode is used by default if other modes/formats are not defined.
-   `NOPREVIEW` - the form will be automatically sent to the printer. In this and the previous modes, you can specify a property that will be used for to determine the printer that will be used to print the form (if you don't do it, the default printer will be used).
-   `MESSAGE` - the form will be shown as a message. In this case, it is assumed that the form consists of a single object group. Accordingly, the shown message consists of a header with all the properties having an [empty](Static_view.md#empty) [display group](Form_structure.md#drawgroup), and a table in which rows are object collections of this only object group and columns are properties that are not displayed in the header (i.e. for which the display group exists and is equal to the only object group of the form being opened). The operator working in this mode is essentially a generalization of the [message display](Show_message_MESSAGE_ASK.md) operator. It is worth noting that this operator mode is used in [constraints](Constraints.md) (when the user is shown objects for which a constraint was violated).

Similarly to the [interactive view](In_an_interactive_view_SHOW_DIALOG.md), the interactive modes of the print view enable the developer to set options for  [flow management](In_an_interactive_view_SHOW_DIALOG.md#flow) and [form location](In_an_interactive_view_SHOW_DIALOG.md#location) (their behavior is identical to the corresponding options in the form opening operator in the interactive view).


:::info
Сurrent implementation: in the `MESSAGE` mode, the form is always shown as a window (the "asynchronous window" option is not supported in this case); in the asynchronous `PREVIEW` mode the form is always shown as a tab; in the synchronous `PREVIEW` mode - as a window. The `NOPREVIEW` mode is always asynchronous and the form location in it does not make sense (since no forms are shown to the user directly)
:::

### Language

To open the form in the print view, [`PRINT` operator](PRINT_operator.md) is used.

### Examples

```lsf
FORM printOrder
    OBJECTS o = Order
    PROPERTIES(o) currency, customer

    OBJECTS d = OrderDetail
    PROPERTIES(d) idSku, price
    FILTERS order(d) == o
;

print (Order o)  {
    PRINT printOrder OBJECTS o = o; // printing

    LOCAL file = FILE ();
    PRINT printOrder OBJECTS o = o DOCX TO file;
    open(file());

    //v 2.0-2.1 syntax
    LOCAL sheetName = STRING[255]();
    sheetName() <- 'encrypted';
    PRINT printOrder OBJECTS o = o XLS SHEET sheetName PASSWORD 'pass';

    //v 2.2 syntax
    //PRINT printOrder OBJECTS o = o XLS SHEET 'encrypted' PASSWORD 'pass';
}
```
