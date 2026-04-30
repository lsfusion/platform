---
title: 'FORMULA operator'
---

The `FORMULA` operator creates a [property](Properties.md) that implements a [custom formula](Custom_formula_FORMULA.md).

### Syntax

```
FORMULA [NULL] [className [valueId]] [syntaxType1] text1, ..., [syntaxTypeN] textN [(classId1 [paramId1], ..., classIdK [paramIdK])] [NULL]
```

### Description

The `FORMULA` operator creates a property that executes an arbitrary formula in SQL. It is possible to specify different formulas for different SQL dialects so that these properties are portable between different DBMSs.

`FORMULA` is a [context-independent](Property_operators.md#contextindependent) property operator: it cannot appear inside [expressions](Expression.md). Use it on the right-hand side of an `=` statement or anonymously inside brackets in a [`JOIN` operator](JOIN_operator.md) usage.

#### Two operating modes

The FORMULA operator works in one of two modes depending on whether every property parameter appears in the formula text or not:

**1. Expression mode** (default)

Every parameter is referenced in the formula text. For each combination of input values the formula returns exactly one result — like a mathematical function. This is the most common usage.

```lsf
round(number, digits) = FORMULA 'round(CAST(($1) as numeric), $2)';
```

**2. Table-valued function mode**

Some database functions do not return a single value — they return a *set* of rows. For example, "split a string by delimiter" can yield zero, one, or many results. In lsFusion a property must return exactly one value per unique combination of its parameters, so an extra parameter is needed to pinpoint a specific row in the result set.

When at least one property parameter is **absent** from the formula text, lsFusion automatically switches to table-valued function mode. The unused parameter(s) become additional key dimensions that uniquely identify each row of the result. The `valueName` token specifies which column of the function's output holds the actual value to return.

The two modes are mutually exclusive and are chosen automatically — no extra keyword is needed.

##### How unused parameters identify a row

There are two variants:

**`row INTEGER` — ordinal access**

If there is exactly one unused parameter named `row` with type `INTEGER`, lsFusion automatically assigns sequential integer numbers (1, 2, 3, …) to the rows produced by the function. To retrieve the third element pass `row = 3`; to iterate over all elements use aggregation over `row`.

```lsf
// elements of a JSON array, accessed by 1-based index
array(JSON json, INTEGER row) = FORMULA JSON value 'jsonb_array_elements($json)';

// words of a string split by delimiter, accessed by 1-based index
array(STRING string, STRING delimeter, INTEGER row) = FORMULA STRING value 'unnest(string_to_array($string, $delimeter))';
```

**Any other name/type — key-column access**

In all other cases each unused parameter name must match a column that the function itself returns. lsFusion uses those columns as join keys: pass the desired key value(s) as the parameter(s) to retrieve the corresponding row. Multiple unused parameters form a composite key — each maps independently to the output column of the same name.

The function must naturally produce columns whose names match the parameter names. For example, `jsonb_each` returns pairs `(key, value)` — so the unused parameter `key STRING` maps directly to the `key` column, and `valueName = value` names the column to read back.

```lsf
// value at a given key inside a JSON object
map(JSON json, STRING key) = FORMULA JSON value 'jsonb_each($json)';

// value at a given key as plain text
mapText(JSON json, STRING key) = FORMULA STRING value 'jsonb_each_text($json)';
```

#### Referencing parameters in the formula text

Parameters can be referenced either by **position** (`$1`, `$2`, ...) or by **name** (`$paramName`).

Named references are resolved in the following order:
1. If an explicit parameter list `(...)` is given after the formula text, its names are used.
2. Otherwise the names are taken from the property's own parameter list.

Positional and named references can be mixed freely.

```lsf
// names come from the property's own parameters
substring(STRING string, STRING regexp) = FORMULA NULL STRING PG 'SELECT substring($string, $regexp)';

// names declared explicitly in (...)
toDateTime = FORMULA DATETIME '($date) + ($time)' (DATE date, TIME time);
```

#### NULL semantics

By default, if any parameter value is NULL the formula returns NULL immediately without evaluating the expression (standard arithmetic behaviour: `NULL + 5 = NULL`).

The two `NULL` keywords modify this behaviour (see Parameters section). They are independent and can be specified together.

### Parameters

- `NULL`

    Keyword that loosens the default `NULL`-handling rules of a non-table-valued formula (table-valued mode ignores both positions). Two forms in increasing strength:
    - before `className` — declares that the property may return `NULL` even when all parameter values are non-`NULL`. Without this form (and without the trailing one below), the formula must produce a non-`NULL` result for non-`NULL` arguments — failing this may lead to unpredictable results.
    - at the end (after the optional parameter list) — the formula accepts `NULL` parameter values and is executed over them; without it, a `NULL` argument produces a `NULL` result without calling the formula. This form subsumes the leading one — once the formula sees `NULL` arguments and decides what to return, declaring that the result may be `NULL` adds nothing.

- `className`

    The name of the [builtin class](Built-in_classes.md) of the value returned by the property. If not specified, the resulting class is considered to be the [common ancestor](Built-in_classes.md#commonparentclass) of all property operands. Required for table-valued formulas (see the parameter list below): the value column of the returned table needs an explicit type.

- `valueId`

    Identifier or [string literal](IDs.md#strliteral) placed right after `className`. Names the value column of the table returned by the formula — required for table-valued formulas (see the parameter list below) and ignored for non-table ones. The literal form is typically used to name an existing column in an external table; the identifier form is typically used for the value produced by a set-returning function.

- `syntaxType1, ..., syntaxTypeN`

    Keywords defining SQL dialect types. The following types are currently supported:

    - `PG` - PostgreSQL syntax
    - `MS` - MS SQL Server syntax

  If the dialect type is not specified explicitly, then the corresponding formula text is set as the default text. Each of the types (or the lack of a type) must appear in the operator no more than once.

- `text1, ..., textN`

    [String literals](IDs.md#strliteral), each of which contains a formula in SQL syntax. Formula parameters are referenced as `$1`, `$2`, ... by position, or as `$paramId` when the parameter is named. Positional parameter numbers start from `1`. When several implementations are supplied (one per dialect plus optionally a default), they must all describe the same parameter set — a mismatch in arity across them is rejected at parse time.

- `classId1 [paramId1], ..., classIdK [paramIdK]`

    Optional declaration of the formula's parameter classes (and, for each one, an optional name usable as `$paramId` inside the formula text). When absent, parameter classes and names are inferred from the enclosing property declaration. The resulting property's arity is the larger of `K` and the highest positional index the formula text references; positional references that go beyond `K` silently extend the parameter list with auto-generated names.

    Referenced parameters are passed as inputs into the SQL expression at evaluation time; unreferenced ones become key columns of the returned table (see `valueId` above for the value column) and must match the column names the SQL expression returns. A single unreferenced `INTEGER` parameter named `row` is a shortcut: the platform supplies row numbers for it via SQL's `ROW_NUMBER() OVER ()`, so the idiom works with any table-valued SQL expression.

### Examples

```lsf
// a property with two parameters: a rounded number and the number of decimal places
round(number, digits) = FORMULA 'round(CAST(($1) as numeric),$2)';

// a property that converts the value passed as an argument to a 15-character string.
toString15(str) = FORMULA BPSTRING[15] 'CAST($1 AS character(15))';

// a property with two different implementations for different SQL dialects
jumpWorkdays = FORMULA NULL DATE PG 'jumpWorkdays($1, $2, $3)', MS 'dbo.jumpWorkdays($1, $2, $3)';

// named parameter references: $json / $field use names from the enclosing signature
field (JSON json, STRING field) = FORMULA JSON 'jsonb_extract_path($json, $field)';

// explicit parameter list declaring names for $date / $time references
toDateTime = FORMULA DATETIME '$date + $time' (DATE date, TIME time);

// table-valued formula: `row` is not referenced in the text, so it becomes a key column of the
// table produced by jsonb_array_elements; `value` names the value column feeding the result
array (JSON json, INTEGER row) = FORMULA JSON value 'jsonb_array_elements($json)';

// table-valued formula with a named key column: jsonb_each returns rows of (key, value);
// `key` (unused parameter) and `value` (valueId) align with the column names of that record
map (JSON json, STRING key) = FORMULA JSON value 'jsonb_each($json)';

// external-table mapping: the formula text is the table name, the string literal after
// STRING names the value column, and `key0` is the key column of that external table
country (LONG key0) = FORMULA STRING 'country_name' '_auto_country';

// trailing NULL: the formula itself decides what to return when the argument is NULL
coalesceName = FORMULA TEXT 'COALESCE($1, \'(unknown)\')' (TEXT name) NULL;
```
