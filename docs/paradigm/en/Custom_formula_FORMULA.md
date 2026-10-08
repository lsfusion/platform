---
slug: "/Custom_formula_FORMULA"
title: 'Custom formula (FORMULA)'
---

The *custom formula* operator allows you to create a [property](Properties.md) that calculates a defined formula in SQL. You can give a single implementation used for any SQL server, or different implementations for different SQL servers.

Using this operator is recommended only if the task cannot be accomplished using other operators, and only if it is known for certain which specific SQL servers can be used, or if the syntax constructs used comply with one of the latest SQL standards.

### Result class

By default, the result class of the custom operator is a [common ancestor](Built-in_classes.md#commonparentclass) of all its operands. If necessary, the developer can specify this class explicitly.

### Parameter references

A custom formula contains references to its parameters within its SQL text. The property's arguments are substituted in place at the points the formula refers to them. Both positional and named references are available — the exact notation and the rules for the resulting property's arity belong to the `FORMULA` operator article.

### Table-valued formulas

A custom formula is not limited to producing one scalar value per call — it may also describe an entire table that the property maps onto. The mode makes it possible to:

- map an lsFusion property directly to an external table in the database;
- reuse SQL table-valued functions as lsFusion properties;
- unnest rows, JSON documents, or array values into row-keyed properties.

The rules for how the property's parameters relate to the underlying table belong to the `FORMULA` operator article.

### `NULL` handling {#null}

A custom formula is executed in SQL exactly as written, with the SQL values of its arguments, and the platform does not check that it follows the rules below. What the platform determines itself is the condition under which the property is non-`NULL`. That condition is used wherever the property serves as a condition, in a selection operator or in an assignment, whereas a value read as is is the result of the SQL text. The formula must therefore keep to the rules of its form itself, otherwise its value and its condition diverge. Except under the second relaxation below, an argument that is `NULL` by construction (the `NULL` literal, a local property without a value) makes the property `NULL` without executing the formula.

By default the property is non-`NULL` when all its arguments are non-`NULL`, and the formula is required to return a non-`NULL` result for them and `NULL` when any argument is `NULL`. This matches the SQL operators and functions that return `NULL` for a `NULL` argument. A formula built on an SQL function that returns a value for `NULL` input must handle `NULL` arguments itself.

This default can be loosened in two ways. The first declares that the formula may return `NULL` even when all of its arguments are non-`NULL`: the property is then non-`NULL` when the computed value is non-`NULL`, so the formula is executed over `NULL` arguments as well and must return `NULL` for them. The second treats the formula as a union of its arguments — useful for SQL functions like `COALESCE` that combine several possibly `NULL` arguments: the formula is executed over `NULL` arguments, and the property is non-`NULL` when at least one argument is non-`NULL`. The formula must then return a non-`NULL` value, and `NULL` when all arguments are `NULL`.

These options apply to scalar formulas only. For table-valued formulas the `NULL` behaviour is determined entirely by the underlying SQL expression and by the table it materialises.

### Language

To declare a property using a custom formula, use the [`FORMULA` operator](../language/FORMULA_operator.md).

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
