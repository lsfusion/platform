---
title: 'FORMULA operator'
---

The `FORMULA` operator creates a [property](Properties.md) that implements a [custom formula](Custom_formula_FORMULA.md).

### Syntax

    FORMULA [NULL] [className] [syntaxType1] text1, ..., [syntaxTypeN] textN

### Description

The `FORMULA` operator creates a property that executes an arbitrary formula in SQL. It is possible to specify different formulas for different SQL dialects so that these properties are portable between different DBMSs. 

This property operator cannot be used inside [expressions](Expression.md).

### Parameters

- `NULL`

    Keyword specifying that the property being created may return `NULL` if all parameter values are non-`NULL`. If not specified, then the property must be defined so that for non-`NULL` parameters it will always return a non-`NULL` value (failure to fulfill this condition may lead to unpredictable results)

- `className`

    The name of the [builtin class](Built-in_classes.md) of the value returned by the property. If not specified, the resulting class is considered to be the [common ancestor](Built-in_classes.md#commonparentclass) of all property operands.

- `syntaxType1, ..., syntaxTypeN`

    Keywords defining SQL dialect types. The following types are currently supported:

    - `PG` - PostgreSQL syntax
    - `MS` - MS SQL Server syntax

  If the dialect type is not specified explicitly, then the corresponding formula text is set as the default text. Each of the types (or the lack of a type) must appear in the operator no more than once.

-   `text1, ..., textN`

    [String literals](IDs.md#strliteral), each of which contains a formula in SQL syntax. The notation `$1`, `$2` etc. is used to pass property parameters to the formula, where the number denotes the property parameter number. Parameter numbers start from `1`. The number of parameters in the created property will be equal to the maximum parameter number specified in the description of the formula.

### Examples

```lsf
// a property with two parameters: a rounded number and the number of decimal places
round(number, digits) = FORMULA 'round(CAST(($1) as numeric),$2)';  

// a property that converts the value passed as an argument to a 15-character string.
toString15(str) = FORMULA BPSTRING[15] 'CAST($1 AS character(15))';
   
// a property with two different implementations for different SQL dialects
jumpWorkdays = FORMULA NULL DATE PG 'jumpWorkdays($1, $2, $3)', MS 'dbo.jumpWorkdays($1, $2, $3)'; 
```
