---
slug: "/Type_conversion"
title: 'Type conversion'
---

The *type conversion* operator creates a [property](Properties.md) that converts an object of one [built-in class](Built-in_classes.md) to an object of another built-in class. The result belongs to the target built-in class. If type conversion is not possible, the property value will be `NULL`.

### Supported conversions

Which conversions are meaningful depends on the built-in class [families](Built-in_classes.md#inheritance) of the source and target classes:

- Within the family of numbers, the value is kept as far as the target class allows; when the value does not fit in the target class, the result is `NULL`.
- Between numbers and strings, the value is converted between its numeric and textual forms; when the source string cannot be read as a number of the target class, the result is `NULL`.
- Strings also convert to and from the `JSON`, `JSONTEXT`, `XML`, and `HTML` classes.

### String and file types

String types can be converted to human-readable file types (`CSVFILE`, `XMLFILE`, `JSONFILE`, `HTMLFILE`, etc.), and vice versa - human-readable file types can be converted to string types. A file of a specific type can also be converted to a file of dynamic type (`FILE`, `NAMEDFILE`) and back, keeping the file content; the dynamic-type file additionally carries the extension, and `NAMEDFILE` also the name.


:::info
Converting dynamic-type files (`FILE`) to strings and vice versa is prohibited in the current implementation, but if necessary this can be done via an intermediate human-readable type - for example, by first converting to `CSVFILE`, and only then to `FILE` (the resulting file [will have the extension](Built-in_classes.md#extension) CSV)
:::

### Implicit conversions

Apart from the explicit conversion operator, the platform converts a value to another class implicitly in several contexts, without any conversion written by the developer:

- When values of different but compatible classes are combined — by addition or subtraction (see the [arithmetic operators](Arithmetic_operators_plus_minus_etc.md)), by the [selection operators](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md), or by the [extremum operators](Extremum_MAX_MIN.md) — each value is converted to their [common ancestor](Built-in_classes.md#commonparentclass), which becomes the result class; in particular, the number classes widen this way (an `INTEGER` combined with a `NUMERIC` becomes that `NUMERIC`).
- In [string concatenation](String_operators_plus_CONCAT_SUBSTRING.md), an operand that is not a string is converted to a string.
- When a value is written to a property of a different but compatible class, it is converted to that class.
- A file of a specific type is converted to a file of dynamic type (`FILE`, `NAMEDFILE`) implicitly — for example during [data import](Data_import_IMPORT.md) without a specified format, or when [working with an external system](Access_to_an_external_system_EXTERNAL.md); the resulting extension is determined as for the explicit conversion.

### Language

To implement conversion, the [type conversion operator](../language/Type_conversion_operator.md) is used.

### Examples

```lsf
itemCount = DATA INTEGER (Store);
itemCountToString(s) = BPSTRING[10](itemCount(s));

barcode = DATA STRING[15] (Item);
longBarcode(Item i) = LONG(barcode(i));
```
