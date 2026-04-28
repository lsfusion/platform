---
title: 'Custom formula (FORMULA)'
---

The *custom formula* operator allows you to create a [property](Properties.md) that calculates a defined formula in SQL. You can specify different implementations of the formula for different SQL servers.

Using this operator is recommended only if the task cannot be accomplished using other operators, and only if it is known for certain which specific SQL servers can be used, or if the syntax constructs used comply with one of the latest SQL standards.

### Determining the result class

By default, the result class of the custom operator is a [common ancestor](Built-in_classes.md#commonparentclass) of all its operands. If necessary, the developer can specify this class explicitly.

### Referencing parameters

A custom formula contains references to its parameters within its SQL text; the property's arguments are substituted in place at the points the formula refers to them. Both positional and named references are available — the exact notation and the rules for the resulting property's arity belong to the [`FORMULA` operator](FORMULA_operator.md) article.

### Table-valued formulas

A custom formula is not limited to producing one scalar value per call — it may also describe an entire table that the property maps onto. The mode makes it possible to:

- map an lsFusion property directly to an external table in the database;
- reuse SQL table-valued functions as lsFusion properties;
- unnest rows, JSON documents, or array values into row-keyed properties.

The rules for how the property's parameters relate to the underlying table belong to the [`FORMULA` operator](FORMULA_operator.md) article.

### `NULL` handling

Custom formulas integrate with the platform's standard `NULL`-propagation behaviour: by default a `NULL` argument short-circuits the formula and the property yields `NULL` directly, and on non-`NULL` arguments the formula is required to produce a non-`NULL` result.

This default can be loosened in two nested ways. The smaller relaxation declares that the formula may return `NULL` even when all of its arguments are non-`NULL`. The larger relaxation goes further and lets the formula receive `NULL` arguments itself — useful for SQL functions like `COALESCE` whose whole point is to act on `NULL` — and once it does, the formula has full control over what to return for any input, so the smaller relaxation has no additional effect on top of it.

These options apply to scalar formulas only; for table-valued formulas the `NULL` behaviour is determined entirely by the underlying SQL expression and by the table it materialises.

### Language

To declare a property using a custom formula, use the [`FORMULA` operator](FORMULA_operator.md).

### Examples

```lsf
// a property with two parameters: a rounded number and the number of decimal places
round(number, digits) = FORMULA 'round(CAST(($1) as numeric),$2)';

// a property that converts the value passed as an argument to a 15-character string.
toString15(str) = FORMULA BPSTRING[15] 'CAST($1 AS character(15))';

// a property with two different implementations for different SQL dialects
jumpWorkdays = FORMULA NULL DATE PG 'jumpWorkdays($1, $2, $3)', MS 'dbo.jumpWorkdays($1, $2, $3)';

// table-valued formula — the INTEGER row parameter is not used in the text and
// becomes a key column of the table returned by jsonb_array_elements
array (JSON json, INTEGER row) = FORMULA JSON value 'jsonb_array_elements($json)';
```
