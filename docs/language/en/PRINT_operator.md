---
slug: "/PRINT_operator"
title: 'PRINT operator'
---

The `PRINT` operator creates an [action](../paradigm/Actions.md) that [opens a form](../paradigm/In_a_print_view_PRINT.md) in [print view](../paradigm/Print_view.md).

### Syntax

```
PRINT [executionType]
      formSpec
      [FILTERS fexpr1, ..., fexprM]
      [printOptions]
```

`formSpec` takes one of the two forms below.

```
name [OBJECTS objName1 = expr1 [NULL], ..., objNameN = exprN [NULL]]
```

or:

```
classFormType className = expr [NULL]
```

`printOptions` takes one of the two forms below.

Message mode:

```
MESSAGE [syncType] [messageType]
        [TOP topLimit] [OFFSET offsetLimit]
```

Interactive mode:

```
[format [SHEET sheetExpr] [PASSWORD passwordExpr] [TO filePropertyId]]
[previewMode]
[syncType]
[TO printerExpr]
```

Where `topLimit` and `offsetLimit` are each defined as:

```
limitExpr
```

or:

```
groupName1 = limitExpr1, ..., groupNameK = limitExprK
```

### Description

The `PRINT` operator creates an action that opens the specified form in print view. In the `OBJECTS` block, [equality filters](../paradigm/Open_form.md#params) on form objects are added; the `FILTERS` clause adds further filter expressions.

The operator has two top-level modes — the *interactive mode* (a preview window, direct printing, or export to a file) and the *message mode* (a popup message).

### Parameters

- `name`

    Form name. [Composite ID](IDs.md#cid).

- `classFormType`

    Determines which form of the class is printed:

    - `LIST` — the class's list form.
    - `EDIT` — the class's edit form.

- `className`

    Name of the user class whose list or edit form is printed. Composite ID.

- `executionType`

    Determines where the report is rendered and printed:

    - `CLIENT` — on the client. Used by default.
    - `SERVER` — on the server. Meaningful only for the interactive mode and only when the report should reach a printer attached to the server.

- `objName1, ..., objNameN`

    Names of form objects for which equality filter values are specified. [Simple IDs](IDs.md#id).

- `expr, expr1, ..., exprN`

    [Expressions](Expression.md) whose values determine the equality filter values for the corresponding form objects.

- `NULL`

    Specifies that the passed value may be `NULL`.

- `fexpr1, ..., fexprM`

    Filter expressions added to the form.

#### Message mode options

- `MESSAGE`

    Keyword selecting the message mode.

- `messageType`

    Sets how the message panel is rendered on the client:

    - `LOG` — message in the `System.log` window.
    - `INFO` — informational message.
    - `SUCCESS` — success message.
    - `WARN` — warning message.
    - `ERROR` — error message.
    - `DEFAULT` — plain message. Used by default.

- `topLimit`, `offsetLimit`

    Number of leading rows shown/skipped respectively when rendering the message. Each is either a single value applied to every group object on the form, or a per-object-group map giving an individual value for the named group objects.

- `limitExpr`, `limitExpr1, ..., limitExprK`

    Expressions whose values are the integer row limits or offsets.

- `groupName1, ..., groupNameK`

    Names of group objects on the form whose limits or offsets are specified individually. Simple IDs.

#### Interactive mode options

- `format`

    Sets the [export format](../paradigm/In_a_print_view_PRINT.md#format) of the generated file:

    - `PDF` — exported to a PDF file.
    - `XLS`, `XLSX` — exported to an Excel file.
    - `DOC`, `DOCX` — exported to a Word file.
    - `RTF` — exported to an RTF file.
    - `HTML` — exported to an HTML file.

    If omitted, no file is produced; the report is rendered through the interactive mode (see `previewMode` and `executionType` for the resulting behavior).

- `sheetExpr`

    Expression whose value is the sheet name in the resulting file. Used only with the `XLS` and `XLSX` formats.

- `passwordExpr`

    Expression whose value is the password that sets read-only protection on the resulting file. Used only with the `XLS` and `XLSX` formats.

- `filePropertyId`

    [Property ID](IDs.md#propertyid) to which the generated file is written. The property must have no parameters and its value must be of a file class. When given, the report is built on the server and the file is written to the property without any client interaction; otherwise the file is sent to the client and opened by the operating system. Has no effect with `SERVER` (the report is sent directly to a server-side printer instead). May appear only when `format` is specified.

- `previewMode`

    Selects how the generated report is delivered on the client:

    - `PREVIEW` — the report is shown to the user (in a preview window, or opened in the OS-associated program when `format` is specified). Used by default.
    - `NOPREVIEW` — the report is sent directly to the printer.

    Has no effect with `SERVER`.

- `printerExpr`

    Expression whose value is the name of the target printer. If not specified, the default printer is used or the printer-selection dialog is offered, depending on the platform configuration.

#### Common options

- `syncType`

    Determines when the surrounding action continues:

    - `WAIT` — after the user closes the preview window or message on the client.
    - `NOWAIT` — immediately after the form data has been prepared on the server. Used by default.

    Has no effect with `SERVER` or with `TO filePropertyId`.

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
    PRINT printOrder OBJECTS o = o;                                       // interactive preview

    LOCAL file = FILE ();
    PRINT printOrder OBJECTS o = o DOCX TO file;                          // write into a file property
    open(file());

    PRINT printOrder OBJECTS o = o XLS SHEET 'encrypted' PASSWORD 'pass'; // XLS with sheet name and password

    PRINT LIST Order = o;                                                 // print the class's list form

    PRINT printOrder OBJECTS o = o FILTERS price(d) > 100 MESSAGE WARN TOP 10; // top-10 high-price details as a warning message
}
```
