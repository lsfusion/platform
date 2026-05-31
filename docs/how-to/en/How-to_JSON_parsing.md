---
slug: "/How-to_JSON_parsing"
title: 'How-to: JSON parsing'
---

The examples below use the wrapper properties around PostgreSQL's `jsonb_*` functions shipped by the platform in the `Utils` module: `field`, `fieldText`, `array`, `arrayText`, `map`, `mapText`, `arrayElement`. Their signatures and description are collected in [`Utils` → `JSON access properties`](../paradigm/System_Utils.md#json-access); this how-to relies on them and does not restate their interface.

## Example 1

### Task

A JSON value of the following shape is on hand:

```json
{
    "version": "1.0",
    "store": {"id": "S-7", "name": "Main warehouse"},
    "orders": [
        {
            "number": "ORD-1001",
            "customer": {"id": "C-21", "name": "Ivanov"},
            "lines": [
                {"item": "SKU-100", "quantity": 2, "price":  99.50},
                {"item": "SKU-200", "quantity": 1, "price": 250.00}
            ]
        },
        {
            "number": "ORD-1002",
            "customer": {"id": "C-22", "name": "Petrov"},
            "lines": [
                {"item": "SKU-300", "quantity": 5, "price":  12.00}
            ]
        }
    ]
}
```

We need to pull individual values from the top level and from deeply nested nodes — without describing a full form for this JSON.

### Solution

```lsf
showInfo (JSON j) {
    MESSAGE 'version          = ' + fieldText(j, 'version');
    MESSAGE 'store.name       = ' + fieldText(j, 'store', 'name');
    MESSAGE 'orders[1].number = ' + fieldText(array(field(j, 'orders'), 1), 'number');
    MESSAGE 'orders[1].cust   = ' + fieldText(array(field(j, 'orders'), 1), 'customer', 'name');
    MESSAGE 'orders[2] (raw)  = ' + arrayElement(field(j, 'orders'), 2);
}
```

`fieldText(j, 'version')` reads a scalar field at the top level.

`fieldText(j, 'store', 'name')` descends one level deeper in a single call thanks to the two-string-argument overload. The three-argument overload covers three levels; for four and more, use composition through `field` (see below).

Reading a field inside an array element is assembled by composition: `field(j, 'orders')` returns the array as `JSON`, `array(…, 1)` takes its first element (also `JSON`), and `fieldText(…, 'number')` reads that element's field as `STRING`. When another level inside the element is needed, the two-key overload of `fieldText` is used: `fieldText(array(field(j, 'orders'), 1), 'customer', 'name')` corresponds to the path `orders[0].customer.name` (lsFusion indexing is 1-based).

`arrayElement(field(j, 'orders'), 2)` differs from `array(…, 2)` in that it returns the element directly as a `STRING` — the textual representation of jsonb. Useful for logging and debugging.

## Example 2

### Task

A JSON value of the same shape as in [example 1](#example-1). We need to walk every order row by row and every line within each order.

### Solution

```lsf
walkOrders (JSON j) {
    LOCAL report = TEXT ();
    report() <- '';
    FOR JSON ord = array(field(j, 'orders'), INTEGER o) DO {
        report() <- report() + 'order ' + fieldText(ord, 'number')
                              + ' / ' + fieldText(ord, 'customer', 'name') + ':\n';
        FOR JSON line = array(field(ord, 'lines'), INTEGER l) DO
            report() <- report() + '  - ' + fieldText(line, 'item')
                                  + ' x ' + fieldText(line, 'quantity')
                                  + ' @ ' + fieldText(line, 'price') + '\n';
    }
    MESSAGE report();
}
```

`FOR JSON ord = array(field(j, 'orders'), INTEGER o)` is the iterator: the parameter `o` is declared in place as `INTEGER` and walks every array index; for each `o` the `array(…, o)` property returns the element as `JSON`, and `ord` is bound to that value for one iteration of the body. The index `o` itself is also available inside the body — useful for numbering, for example.

The nested `FOR JSON line = array(field(ord, 'lines'), INTEGER l) DO` works on the same principle, except the array is now `lines` inside the current order. `ord` stays in scope of the inner loop, so its fields can be used in conditions or on the right-hand side.

The same pattern without `FOR` works inside any scalar expression. To count the total number of lines across all orders, it is enough to enumerate the `(o, l)` pairs and sum one:

```lsf
totalLines (JSON j) = GROUP SUM 1
    IF array(field(array(field(j, 'orders'), INTEGER o), 'lines'), INTEGER l);
```

The `GROUP SUM` condition assembles the full path `j → orders → array → lines → array`; `o` and `l` are declared in place and walk independently. One is counted for each `(o, l)` pair for which the corresponding line exists.

The same way, `GROUP CONCAT` can assemble the report itself with a single declarative aggregation — no `LOCAL`, no `FOR`:

```lsf
ordersReport (JSON j) =
    GROUP CONCAT
        fieldText(array(field(j, 'orders'), INTEGER o), 'number')
          + ' / ' + fieldText(array(field(j, 'orders'), o), 'customer', 'name')
          + ' :: ' + fieldText(array(field(array(field(j, 'orders'), o), 'lines'), INTEGER l), 'item')
          + ' x ' + fieldText(array(field(array(field(j, 'orders'), o), 'lines'), l), 'quantity')
          + ' @ ' + fieldText(array(field(array(field(j, 'orders'), o), 'lines'), l), 'price'),
        '\n'
        ORDER o, l;
```

The `(o, l)` pairs are enumerated exactly as in `GROUP SUM`, and the body is the expression joined by the `'\n'` separator. `ORDER o, l` fixes the row order of the report: by order index first, then by the line index inside it. The result is a single `STRING`, ready to feed to `MESSAGE` or to write into a property.

This form produces a flat report — every output line belongs to one `(order, order-line)` pair, with no grouped header per order. If a per-order header is needed, either fall back to the imperative variant above, or wrap `GROUP CONCAT` in a heavier composition (for example, two levels of aggregation through an auxiliary property).

Walking a dictionary-shaped value (key → value) goes through `map` / `mapText`. The key parameter is declared in place — exactly the same way as the `INTEGER` index of `array`:

```lsf
listMeta (JSON j) {
    LOCAL out = TEXT ();
    out() <- '';
    FOR STRING v = mapText(field(j, 'store'), STRING k) DO
        out() <- out() + k + ' -> ' + v + '\n';
    MESSAGE out();
}
```

Here `mapText(field(j, 'store'), STRING k)` returns one row per `(k, v)` pair of the `store` JSON object: the key parameter `k` is the field name as a string, the body is the field value as a `STRING`.

## Example 3

### Task

The input is a flat JSON array of objects:

```json
[
    {"name": "The Captain's Daughter",  "year": 1836, "price":  8.50},
    {"name": "Eugene Onegin",           "year": 1833, "price": 11.25},
    {"name": "A Hero of Our Time",      "year": 1840, "price":  9.75}
]
```

We do not want to declare a separate form with staging properties for it — every field is consumed exactly once and immediately written into a new `Book`.

### Solution

```lsf
importBooksFlat 'Import books' () {
    INPUT f = FILE DO NEWSESSION {
        IMPORT JSON FROM f FIELDS
            ISTRING[100] name, INTEGER year, NUMERIC[14,2] price
        DO NEW b = Book {
            name(b)  <- name;
            year(b)  <- year;
            price(b) <- price;
        }
        APPLY;
    }
}
```

`IMPORT JSON FROM f FIELDS …` expects a flat JSON array of objects: the object keys match the field names in the list (`name`, `year`, `price`), and their values are cast to the declared types. The `DO` body runs for every row of the array in turn — inside it, `name`, `year`, `price` are available as plain parameters with the values of the current row.

Unlike the form-based variant, staging properties are not needed here; the [`imported[INTEGER]`](../language/IMPORT_operator.md) property is also absent, because there is no explicit iteration — the `DO` part plays that role.

`FIELDS … DO` is the right choice when the values are needed exactly once and validation does not require multiple passes. If a flow needs to validate references first, then create objects in bulk, and only then fill their properties — switch to the form-based variant or to intermediate `LOCAL` properties (see [Example 5](#example-5)).

## Example 4

### Task

The JSON is still flat at the top level, but every row contains a nested object:

```json
[
    {"number": "ORD-2001", "customer": {"id": "C-101", "name": "Turgenev"}},
    {"number": "ORD-2002", "customer": {"id": "C-102", "name": "Lermontov"}},
    {"number": "ORD-2003", "customer": {"id": "C-103", "name": "Gogol"}}
]
```

For each array element we need to create an `Order` and unpack the nested `customer` object into two properties — `customerName` and `customerId`.

### Solution

```lsf
CLASS Order 'Order';
number       'Number'      = DATA STRING[50]   (Order);
customerName 'Customer'    = DATA ISTRING[100] (Order);
customerId   'Customer ID' = DATA STRING[50]   (Order);

importOrders 'Import orders' () {
    INPUT f = FILE DO NEWSESSION {
        IMPORT JSON FROM f FIELDS
            STRING[50] number, JSON customer
        DO NEW o = Order {
            number(o)       <- number;
            customerName(o) <- fieldText(customer, 'name');
            customerId(o)   <- fieldText(customer, 'id');
        }
        APPLY;
    }
}
```

The `customer` field is declared in `FIELDS` as `JSON` — on each iteration the entire nested object arrives in that parameter as a `JSON` value. From there inside `DO` it is unpacked with the usual `fieldText` wrappers: `fieldText(customer, 'name')`, `fieldText(customer, 'id')`. For deeper nesting — `fieldText(customer, 'address', 'city')` and so on.

This pattern replaces a full form-based import as long as the nesting is limited to objects inside the array rows. Once the JSON contains nested arrays that also need to be iterated, going without a form or an outer `FOR JSON … = array(...)` (see [Example 2](#example-2)) becomes considerably harder.

## Example 5

### Task

We have the `Book` class and its form.

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[100] (Book);
year 'Year' = DATA INTEGER (Book);
price 'Price' = DATA NUMERIC[14,2] (Book);

FORM books 'Books'
    OBJECTS b = Book
    PROPERTIES(b) name, year, price, NEW, DELETE
;

NAVIGATOR {
    NEW books;
}
```

We need a button that loads a list of books from a JSON file of the following shape:

```json
{
    "books": [
        {"name": "Crime and Punishment",   "year": 1866, "price": 14.50},
        {"name": "The Brothers Karamazov", "year": 1880, "price": 18.99},
        {"name": "Notes from Underground", "year": 1864, "price":  6.25}
    ]
}
```

### Solution

```lsf
importBookName  = DATA LOCAL ISTRING[100]   (INTEGER);
importBookYear  = DATA LOCAL INTEGER        (INTEGER);
importBookPrice = DATA LOCAL NUMERIC[14,2]  (INTEGER);

FORM importBooks
    OBJECTS books = INTEGER
    PROPERTIES(books) importBookName  EXTID 'name',
                      importBookYear  EXTID 'year',
                      importBookPrice EXTID 'price'
;

importBooksFromJSON 'Import from JSON' () {
    INPUT f = FILE DO NEWSESSION {
        IMPORT importBooks JSON FROM f;

        FOR importBookName(INTEGER i) NEW b = Book DO {
            name(b)  <- importBookName(i);
            year(b)  <- importBookYear(i);
            price(b) <- importBookPrice(i);
        }
        APPLY;
    }
}

EXTEND FORM books
    PROPERTIES() importBooksFromJSON
;
```

The `importBooks` form mirrors the JSON shape: for the `books` array there is an `OBJECTS books = INTEGER` group, and under it three properties whose names are mapped to JSON keys via `EXTID`. The `INTEGER` is a synthetic per-row key supplied by the platform.

[`IMPORT … JSON FROM`](../language/IMPORT_operator.md) reads the file and fills the local properties — `importBookName(i)`, `importBookYear(i)`, `importBookPrice(i)` — for every row `i`.

`FOR importBookName(INTEGER i)` walks every row whose imported name is not `NULL` and creates a `Book` object for each. The system `imported[INTEGER]` property should not be used for iteration here — unlike with flat formats (`IMPORT XLS`, `IMPORT CSV`), it is not set under `IMPORT … JSON FROM`; the "this row came from the file" role is played by any non-empty staging property instead.

The object creation runs in `NEWSESSION` so the import does not accidentally apply pending edits sitting on the `books` form itself. `APPLY` commits the changes; if a constraint fails, it shows the error text to the user on its own.
