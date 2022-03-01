---
title: 'JSON operator'
---

The `JSON` operator is property that creates JSON from [specified properties](Data_export_EXPORT.md) or, in common case, from [form](In_a_structured_view_EXPORT_IMPORT.md). 

## Syntax

    JSON ( FROM [columnId1 =] propertyExpr1, ..., [columnIdN = ] propertyExprN [WHERE whereExpr] [ORDER orderExpr1 [DESC], ..., orderExprL [DESC]] )
    JSON ( formName [OBJECTS objName1 = expr1, ..., objNameK = exprK]

## Description

The `JSON` operator is property that creates JSON from the specified properties or form.

When exporting a form in an `OBJECTS` block, it is possible to add extra filters to check for the equality of the objects on the form with [the values passed](Open_form.md#params). These objects [will not participate](Structured_view.md#objects) in building the object group hierarchy.

## Parameters

### Source of export

- `formName`

    The name of the form from which you want to export data. [Composite ID](IDs.md#cid).

- `objName1 ... objNameK`

    Names of form objects for which filtered (fixed) values are specified. [Simple IDs](IDs.md#id).

- `expr1 ... exprK`

    [Expressions](Expression.md) whose values determine the filtered (fixed) values for form objects.

- `propertyExpr1, ..., propertyExprN`

    List of [expressions](Expression.md) from whose values the data is exported

- `columnId1, ..., columnIdN`

    A list of column IDs in the resulting JSON into which data from the corresponding property will be exported. Each list element is either [a simple ID](IDs.md#id) or a [string literal](Literals.md#strliteral). If no ID is specified, it is considered equal to `expr<Column number>` by default.

- `whereExpr`

    An expression whose value is a condition for the export. If not specified, it is considered equal to the [disjunction](Logical_operators_AND_OR_NOT_XOR.md) of all exported properties (that is, at least one of the properties must be non-`NULL`).

- `orderExpr1, ..., orderExprL`

    List of [expressions](Expression.md) by which the exported data is sorted. Only properties present in the list `propertyExpr1, ..., propertyExprN` can be used

- `DESC`

    Keyword. Specifies reverse sort order. By default, ascending sort is used.

## Examples

```lsf
FORM testF 
      OBJECTS j = INTEGER
      PROPERTIES ab='34'
      OBJECTS i = INTEGER
      PROPERTIES name = 'Name ' + (i AS INTEGER)
;

run() {
	MESSAGE JSON (testF OBJECTS j=4 FILTERS mod(i,2) = 0);
}
```

```lsf
MESSAGE JSON (FROM code = '1', message = 'OK');
```
