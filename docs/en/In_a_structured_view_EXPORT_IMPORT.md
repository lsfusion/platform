---
title: 'In a structured view (EXPORT, IMPORT)'
---

This operator creates an action that [opens a form](Open_form.md) in the [structured](Structured_view.md) view.

### Format {#format}

In this operator, you can define the format that all form data will be converted to:  **XML**, **JSON**, **DBF**, **CSV**, **XLS**. The generated file(s) in this format is then written to the specified property.

Form export is a general case of the  [data export operator](Data_export_EXPORT.md).

### Form import {#importForm}

Form import is an operation that is opposite to opening the form in a structured view. The import operator accepts files in a structured format, then parses them and saves the data to the properties of the set form in such a way that when this form is exported back into the imported format, it would recreate the original file.

Since the import operator is essentially an "input operator", the following constraints apply to the form being imported:

-   All form objects must belong to  [numeric](Built-in_classes.md#inheritance) or [concrete](User_classes.md#abstract) [user](User_classes.md) classes. Object groups must consist of exactly one object (this constraint is caused by the fact that all the used formats are essentially lists — that is, mappings of numbers to values).

-   Properties and [filters](Form_structure.md#filters) on the form should be [changeable](Property_change_CHANGE.md) by a given value (that is, as a rule, be [primary](Data_properties_DATA.md)). Before importing, any existing changes to the imported properties in the current session are canceled.

During import, filters change to the [default values](Built-in_classes.md#defaultvalue) of value classes of these filters.

When importing data into objects of numeric classes, 0-based numbering is used. In case of [hierarchical](Structured_view.md#hierarchy) formats, numbering is "end-to-end" (that is, when the object group being imported is encountered for the second and subsequent times, object numbering in it starts from the position that the previous one stopped at).

When importing from XLS and CSV without headers (with the `NOHEADER` option), the platform automatically attempts to convert data to the necessary type. If it fails, a `NULL` value is written to the property. Importing from other formats requires correct types. For example, if a string is required during data import from JSON, and the JSON file contains a number (without quotes), the platform will generate an error.

If a property (object group) is not found during import, it is ignored (that is, its value remains equal to `NULL`).

Form import is a general case of the [data import](Data_import_IMPORT.md) operator.

### Language

To open the form in the structured view, use the [`EXPORT` operator](EXPORT_operator.md). To import a form, use the [`IMPORT` operator](IMPORT_operator.md).

### Examples

```lsf
FORM exportSku
    OBJECTS st = Store

    OBJECTS s = Sku
    PROPERTIES(s) id, name, weight
    FILTERS in(st, s)
;

exportSku (Store store)  {
    // uploading to DBF all Sku for which in (Store, Sku) is specified for the desired warehouse
    EXPORT exportSku OBJECTS st = store DBF CHARSET 'CP866';
    EXPORT exportSku XML;
    EXPORT exportSku OBJECTS st = store CSV ',';
}
```

```lsf

date = DATA DATE (INTEGER);
sku = DATA BPSTRING[50] (INTEGER);
price = DATA NUMERIC[14,2] (INTEGER);
order = DATA INTEGER (INTEGER);
FORM import
    OBJECTS o = INTEGER // orders
    OBJECTS od = INTEGER // order lines
    PROPERTIES (o) dateOrder = date // importing the date from the dateOrder field
    PROPERTIES (od) sku = sku, price = price // importing product quantity from sku and price fields
    FILTERS order(od) = o // writing the top order to order

;

importForm()  {
    INPUT f = FILE DO {
        IMPORT import JSON FROM f;
        SHOW import; // showing what was imported

        // creating objects in the database
        FOR DATE date = date(INTEGER io) NEW o = Order DO {
            date(o) <- date;
            FOR order(INTEGER iod) = io NEW od = OrderDetail DO {
                price(od) <- price(iod);
                sku(od) <- GROUP MAX Sku sku IF name(sku) = sku(iod); // finding sku with this name
            }
        }
    }
}
```
