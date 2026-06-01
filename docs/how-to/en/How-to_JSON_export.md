---
slug: "/How-to_JSON_export"
title: 'How-to: JSON export'
---

## Example 1

### Task

Build a JSON value from a set of literals — for example, an API response: `{"code":"OK","message":"привет"}`.

### Solution

```lsf
respond () {
    LOCAL f = FILE ();
    EXPORT JSON FROM code = 'OK', message = 'привет' TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

The [`EXPORT` operator](../language/EXPORT_operator.md) with the `JSON` format takes a list of named columns and writes the result to the file given after `TO`. The expressions in `FROM` carry no row parameters, so the platform produces exactly one JSON object — `{"code":"OK","message":"привет"}`. Key names come from the left side of `=`; without one, the column is named `exprN`.

Reading the result: `fileToString(f(), 'UTF-8')` (from the [`Utils`](../paradigm/System_Utils.md) module) fills the system local property `resultString[]`, which `MESSAGE` displays.

## Example 2

### Task

Given the `Book` class:

```lsf
CLASS Book 'Book';
name 'Name' = DATA ISTRING[100] (Book);
year 'Year' = DATA INTEGER (Book);
price 'Price' = DATA NUMERIC[14,2] (Book);
```

Export all books to a JSON array of objects with fields `name`, `year`, `price`, sorted by year.

### Solution

```lsf
exportBooks () {
    LOCAL f = FILE ();
    EXPORT JSON FROM name = name(Book b), year = year(b), price = price(b)
        ORDER year(b)
        TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

When at least one expression in `FROM` contains a parameter (`b` is declared as a typed parameter of class `Book`), `EXPORT JSON FROM` iterates over every value of that parameter for which at least one expression in the list is non-`NULL`. The result is an array. `ORDER year(b)` fixes the row order by year.

Result on three books (the sample dataset uses Russian titles — Cyrillic strings stay as-is in the JSON):

```json
[{"name":"Записки из подполья","year":1864,"price":6.25},
 {"name":"Преступление и наказание","year":1866,"price":14.5},
 {"name":"Братья Карамазовы","year":1880,"price":18.99}]
```

## Example 3

### Task

Same books, but only those published from 1870 on, in descending order by year, no more than two records.

### Solution

```lsf
exportRecentBooks () {
    LOCAL f = FILE ();
    EXPORT JSON FROM name = name(Book b), year = year(b), price = price(b)
        WHERE year(b) >= 1870
        ORDER year(b) DESC
        TOP 2
        TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

`WHERE` filters rows before JSON is built; `ORDER … DESC` sets the reverse order; `TOP 2` keeps at most the first two records.

On the same three books, only "Братья Карамазовы" (1880) passes `year >= 1870`, so even with `TOP 2` the array gets one element:

```json
[{"name":"Братья Карамазовы","year":1880,"price":18.99}]
```

## Example 4

### Task

Build a composite JSON: an outer object with metadata (`title`), and inside it an array of books — for example, to send to an external service.

### Solution

```lsf
exportCatalog () {
    LOCAL f = FILE ();
    EXPORT JSON FROM
        title = 'Books catalog',
        books = (JSON FROM name = name(Book b), year = year(b), price = price(b)
                 ORDER year(b))
        TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

The `books = (JSON FROM …)` construct embeds a [`JSON` operator](../language/JSON_operator.md) expression as the value of the `books` column. The outer `EXPORT JSON FROM` builds one object (`title` has no parameters), and the inner `JSON FROM` iterates books and supplies the array:

```json
{"title":"Books catalog",
 "books":[{"year":1864,"price":6.25,"name":"Записки из подполья"},
          {"year":1866,"price":14.5,"name":"Преступление и наказание"},
          {"year":1880,"price":18.99,"name":"Братья Карамазовы"}]}
```

Deeper structures are built the same way — several parallel nested `JSON FROM` expressions inside one `EXPORT`: `items = (JSON FROM …), partners = (JSON FROM …), currencies = (JSON FROM …)`. They run independently and land in their own top-level keys.

## Example 5

### Task

A two-level hierarchy with two kinds of nesting on the same level: the output needs an object with metadata (`title`), a nested **object** `store` (`{id, name}` with no array), and an array of orders, each order having its own array of lines. Classes:

```lsf
CLASS Order 'Order';
number       'Number'   = DATA STRING[50]   (Order);
customerName 'Customer' = DATA ISTRING[100] (Order);

CLASS OrderLine 'Order line';
order     'Order'    = DATA Order        (OrderLine) NONULL DELETE;
item      'Item'     = DATA STRING[50]   (OrderLine);
quantity  'Quantity' = DATA INTEGER      (OrderLine);
linePrice 'Price'    = DATA NUMERIC[14,2] (OrderLine);
```

### Solution

```lsf
exportOrdersNested () {
    LOCAL f = FILE ();
    EXPORT JSON FROM
        title = 'Orders',
        store = (JSON FROM id = 'S-7', name = 'Main warehouse'),
        orders = (JSON FROM
            number = number(Order o),
            customer = customerName(o),
            lines = (JSON FROM
                item = item(OrderLine l),
                quantity = quantity(l),
                price = linePrice(l)
                WHERE order(l) = o
                ORDER item(l))
            ORDER number(o))
        TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

Two different kinds of nesting are assembled here with the same `column = (JSON FROM …)` syntax:

- `store = (JSON FROM id = 'S-7', name = 'Main warehouse')` — the inner `JSON FROM` has **no row parameter**, so it returns **one JSON object** `{"id":"S-7","name":"Main warehouse"}` that becomes the value of the `store` key. No array appears.
- `orders = (JSON FROM …)` — the inner has the parameter `Order o`, so the platform iterates it and returns an **array**. The same trick repeats one level deeper for `lines`: the inner `JSON FROM` over `OrderLine l` uses the outer parameter `o` in `WHERE order(l) = o`. For every outer row the inner expression is re-evaluated with the current `o` bound.

Result:

```json
{"title":"Orders",
 "store":{"name":"Main warehouse","id":"S-7"},
 "orders":[
   {"number":"ORD-001","lines":[
     {"item":"SKU-100","quantity":2,"price":99.5},
     {"item":"SKU-200","quantity":1,"price":250}],"customer":"Иванов"},
   {"number":"ORD-002","lines":[
     {"item":"SKU-300","quantity":5,"price":12}],"customer":"Петров"}]}
```

Note: inside the objects, `lines` and `name` do not appear in declaration order. The `JSON FROM` serializer emits nested JSON values separately from scalar columns. In Example 6 the same result through a form keeps the declaration order. By the JSON specification these are the same structure — key order inside an object is not significant.

## Example 6

### Task

The same JSON as in [Example 5](#example-5), but through a [form](../paradigm/Forms.md) — export a specific form rather than a composition of `JSON FROM` expressions. Convenient when the export and the interactive view should share one and the same field structure.

### Solution

```lsf
GROUP store;

FORM exportOrdersAndLines
    PROPERTIES title = 'Orders'
    PROPERTIES IN store id = 'S-7', name = 'Main warehouse'

    OBJECTS orders = Order
    PROPERTIES(orders) number, customer = customerName
    ORDERS number(orders)

    OBJECTS lines = OrderLine
    PROPERTIES(lines) item, quantity, price = linePrice
    FILTERS order(lines) = orders
    ORDERS item(lines)
;

exportOrdersForm () {
    LOCAL f = FILE ();
    EXPORT exportOrdersAndLines JSON TO f;
    fileToString(f(), 'UTF-8');
    MESSAGE resultString();
}
```

`EXPORT formName JSON` builds JSON by the [form hierarchy](../paradigm/In_a_structured_view_EXPORT_IMPORT.md): the root is an object whose keys are the names of property groups, object groups, or parameterless scalar properties on the form, and whose values are the corresponding nested objects, arrays of rows, or the scalars themselves. Here:

- `title = 'Orders'` is a parameterless scalar property — it lands as a root-level scalar;
- `GROUP store;` declares a [property group](../paradigm/Groups_of_properties_and_actions.md); `PROPERTIES IN store id = 'S-7', name = 'Main warehouse'` puts two scalars inside it — on export this becomes a **nested object** `"store":{"id":"S-7","name":"Main warehouse"}` (no array appears because the group contains no object groups);
- the group `OBJECTS orders = Order` becomes the **array** `orders`; the keys inside an element are `number` and `customer` (the latter renamed from `customerName` via `customer = customerName`);
- the nested group `OBJECTS lines = OrderLine` with `FILTERS order(lines) = orders` lands as a `lines` array inside every `orders` element.

The resulting JSON has the same structure as in Example 5 — same keys, same values, same nesting:

```json
{"title":"Orders",
 "store":{"id":"S-7","name":"Main warehouse"},
 "orders":[
   {"number":"ORD-001","customer":"Иванов","lines":[
     {"item":"SKU-100","quantity":2,"price":99.5},
     {"item":"SKU-200","quantity":1,"price":250}]},
   {"number":"ORD-002","customer":"Петров","lines":[
     {"item":"SKU-300","quantity":5,"price":12}]}]}
```

Key names in the form-based variant come from the names of object groups and form columns; a column can be renamed by the same `EXTID` mechanism as in the reverse [form-based import](How-to_JSON_parsing.md#example-5).
