---
title: 'PRINT operator'
---

The `PRINT` operator creates an [action](Actions.md) that [opens a form](In_a_print_view_PRINT.md) in print view. 

### Syntax

    PRINT name 
    [OBJECTS objName1 = expr1, ..., objNameN = exprN]
    [formActionOptions] 

`formActionOptions` - additional options for this action. There are several possible option syntaxes in this operator:

    printFormat [SHEET sheetProperty] [PASSWORD passwordExpr] [TO propertyId]
    [PREVIEW | NOPREVIEW] [syncType] [TO printerExpr]
    MESSAGE [syncType] [TOP n]

### Description

The `PRINT` operator creates an action that prints the specified form. When printing a form in the `OBJECTS` block you can add [additional filters](Open_form.md#params) for form objects to check these objects for equality to the values passed.

### Parameters

- `name`

    Form name. [Composite ID](IDs.md#cid).

- `objName1 ... objNameN`

    Names of form objects for which additional filters are specified. [Simple IDs](IDs.md#id).

- `expr1 ... exprN`

    [Expressions](Expression.md) whose values determine the filtered (fixed) values for form objects.

#### Additional options

- `printFormat`

    The [print format](In_a_print_view_PRINT.md#format) is specified by one of these keywords:

    - `PDF` – the form will be exported to a PDF file.
    - `XLS`, `XLSX` – the form will be exported to a file in one of the specified EXCEL formats.
    - `DOC`, `DOCX` – the form will be exported to a file in one of the specified WORD formats.
    - `RTF` – the form will be exported to an RTF file.
    - `HTML` – the form will be exported to an HTML file.

- `sheetProperty`

    The [ID of the property](IDs.md#propertyid) whose value is used as the name of the sheet in the exported file. The property must not have parameters. It is used for `XLS` and `XLSX` print formats.

- `passwordExpr`

    An expression whose value determines the password that sets the read-only mode for the exported file. Used for `XLS` and `XLSX` print formats.

- `propertyId`

    [Property ID](IDs.md#propertyid) to which the generated file will be written. The property must not have parameters. If a property is not specified, the generated file is sent to the client and opened by the operating system

- `PREVIEW`

    Keyword. If specified, the form is displayed in [preview](In_a_print_view_PRINT.md#interactive) mode. This mode is used by default if no other modes/formats are specified.

- `NOPREVIEW`

    Keyword. If specified, the form is immediately (without preview) sent for printing.

- `printerExpr`

    An expression whose value determines the name of the printer to which the print job will be sent. If no printer with the specified name is found (or specified), the default printer is selected.

- `MESSAGE`

    Keyword. If specified, the form displays data to the user in [message](In_a_print_view_PRINT.md#interactive) mode.

- `TOP n`

    Displays only the first `n` entries. [Integer literal](Literals.md#intliteral).

- `syncType`

    Determines when the created action should be continued:

    - `WAIT` - after the client completes the action (closes the preview/message form). Used by default.
    - `NOWAIT` - after preparation of the information for sending to the client (form data is read).

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
