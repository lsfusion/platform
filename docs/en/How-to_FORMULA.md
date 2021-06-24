---
title: 'How-to: FORMULA'
---

## Example 1

### Task

We have a list of orders.

```lsf
CLASS Order 'Order';
date 'Date' = DATA DATE (Order);
number 'Number' = DATA STRING[30] (Order);

FORM orders 'Purchase orders'
    OBJECTS o = Order
    PROPERTIES(o) date, number, NEW, DELETE
;

NAVIGATOR {
    NEW orders;
}
```

We need to export this list to CSV and keep the date in the ISO format `YYYY-MM-DD`.

### Solution

```lsf
toISO = FORMULA STRING[10] 'to_char($1,\'YYYY-MM-DD\')';

exportToCSV 'Export to CSV' () {
    LOCAL file = FILE ();
    EXPORT CSV FROM toISO(date(Order o)), number(o) TO file;
    open(file());
}

EXTEND FORM orders
    PROPERTIES() exportToCSV
;
```

To solve this task we use the [`FORMULA` operator](FORMULA_operator.md) to create a new property that takes a date and returns its value as a string in the `YYYY-MM-DD` format. The expression contains [`to_char`](https://www.postgresql.org/docs/11/functions-formatting.html) which is a standard PostgreSQL function.

## Example 2

### Task

Similar to [**Example 1**](#example-1). New lines containing quantity and amount have been added to the orders.

```lsf
CLASS OrderDetail;
order = DATA Order (OrderDetail) NONULL DELETE;

quantity 'Qty' = DATA NUMERIC[14,3] (OrderDetail);
sum 'Amount' = DATA NUMERIC[14,2] (OrderDetail);

EXTEND FORM orders
    OBJECTS d = OrderDetail
    PROPERTIES(d) quantity, sum, NEW, DELETE
    FILTERS order(d) = o
;
```

We need to export all the lines from a given order as CSV file in which quantities and amounts are shortened to 3 and 2 characters respectively. In addition, the numbers must be split into triads.

### Solution

```lsf
toString = FORMULA TEXT 'to_char($1,$2)';

exportToCSV 'Export to CSV' (Order o) {
    LOCAL file = FILE ();
    EXPORT CSV FROM toISO(date(o)), number(o), toString(quantity(OrderDetail d), '999 999.999'), toString(sum(d), '999 999.99') WHERE order(d) = o TO file;
    open(file());
}

EXTEND FORM orders
    PROPERTIES(o) exportToCSV
;
```

We create the `toString` property that takes two parameters (numeric value and format) and returns a value of the `TEXT` type. When exporting, we pass the required format as the second parameter.

## Example 3

### Task

Similar to [**Example 2**](#example-2).

We need to add a column that will be marked when the given order number contains only digits.

### Solution

```lsf
onlyDigits = FORMULA NULL BOOLEAN 'CASE WHEN trim($1) ~ \'^[0-9]*$\' THEN 1 ELSE NULL END';

EXTEND FORM orders
    PROPERTIES 'Only numbers' = onlyDigits(number(o))
;
```

Since single quotes are used in the formula, make sure to [escape](https://en.wikipedia.org/wiki/Escape_character) them with a backslash `\`.

Note that the native `BOOLEAN` type allows only 2 values: `TRUE` and `NULL`. Therefore, when composing a logical expression, make sure to convert its negative value to `NULL`. In addition, the platform must explicitly know whether the expression can return an undefined value. This is why the keyword `FORMULA` must be followed by the corresponding marker.

At the database level, the `BOOLEAN` type is stored as numeric value (`1` or `null`), and therefore the properties of this type must also return a numeric value. The developer must check that the return type of the expression matches the specified type. Otherwise, the behavior will be unpredictable (but in most cases a request will simply return an error).

Keep in mind that if any property composed by the `FORMULA` operator receives `NULL` as argument, then the overall result will always be `NULL`.

  
