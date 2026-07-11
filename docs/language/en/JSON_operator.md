---
slug: "/JSON_operator"
title: 'JSON operator'
---

The `JSON` and `JSONTEXT` operators create a [property](../paradigm/Properties.md) that builds JSON from a list of [specified properties](../paradigm/Data_export_EXPORT.md) or, in the general case, from a [form](../paradigm/In_a_structured_view_EXPORT_IMPORT.md).

### Syntax

The operator has two forms, both starting with the `jsonKeyword` choice. The form that builds JSON from a list of properties:

```
jsonKeyword FROM [columnId1 =] propertyExpr1, ..., [columnIdN =] propertyExprN
  [WHERE whereExpr]
  [ORDER orderExpr1 [DESC], ..., orderExprL [DESC]]
  [TOP topExpr] [OFFSET offsetExpr]
```

The form that builds JSON from a form:

```
jsonKeyword ( formName [OBJECTS objName1 = expr1, ..., objNameK = exprK]
  [FILTERS filterExpr1, ..., filterExprP]
  [TOP topSelect] [OFFSET offsetSelect] )
```

Where `jsonKeyword` is defined as:

```
JSON
JSONTEXT
```

And `topSelect` and `offsetSelect` are each defined either as a single expression or as a per-group-object map:

```
topExpr
topGroupId1 = topPropertyExpr1, ..., topGroupIdT = topPropertyExprT
```

```
offsetExpr
offsetGroupId1 = offsetPropertyExpr1, ..., offsetGroupIdF = offsetPropertyExprF
```

### Description

The `JSON` and `JSONTEXT` operators create a property that builds JSON either from a list of properties (the `FROM` form) or from a form. The result is produced [in a structured view](../paradigm/In_a_structured_view_EXPORT_IMPORT.md): building JSON from a list of properties is a special case of building JSON from a form, so the meaning of column names, the object group hierarchy, and the resulting structure follow the [form export](../paradigm/Structured_view.md) behavior. Unlike the [`EXPORT` operator](EXPORT_operator.md), which is an action that writes the result to a file, this operator is an expression: it returns the built JSON as the property value.

The difference between the two keywords is the class of the returned value. `JSON` returns a value of the `JSON` class (a file holding the JSON document), while `JSONTEXT` returns a value of the `JSONTEXT` class (the JSON document as text).

A property value of the `JSON` or `JSONTEXT` class (for example, one built by another property with this operator) is embedded into the resulting JSON as a nested object or array rather than as a string. This allows assembling a document with nested structures from separate JSON properties.

When building JSON from a form, the `OBJECTS` block fixes form objects to given values: each such object is constrained to equal [the value passed](../paradigm/Open_form.md#params) and [does not participate](../paradigm/Structured_view.md#objects) in building the object group hierarchy. The `FILTERS` block adds further filter conditions to the form before the build.

### Parameters

- `jsonKeyword`

    The keyword choice selecting the class of the result. `JSON` builds a value of the `JSON` class (a file), `JSONTEXT` builds a value of the `JSONTEXT` class (text).

- `propertyExpr1, ..., propertyExprN`

    A non-empty list of [expressions](Expression.md) from whose values the JSON is built. Each expression corresponds to a column of the result.

- `columnId1, ..., columnIdN`

    A list of column IDs in the resulting JSON into which data from the corresponding property will be written. Each list element is either [a simple ID](IDs.md#id) or a [string literal](Literals.md#strliteral). If no ID is specified, it is considered equal to `expr<column number>` by default.

- `whereExpr`

    An [expression](Expression.md) whose value is the condition of the build. If not specified, it is considered equal to the [disjunction](../paradigm/Logical_operators_AND_OR_NOT_XOR.md) of all the listed properties (that is, at least one of the properties must be non-`NULL`).

- `orderExpr1, ..., orderExprL`

    A list of [expressions](Expression.md) by which the built data is sorted. Only properties present in the list `propertyExpr1, ..., propertyExprN` can be used.

- `DESC`

    Keyword. Specifies the reverse sort order for the preceding expression. By default, ascending sort is used.

- `formName`

    The name of the form from which the JSON is built. [Composite ID](IDs.md#cid).

- `objName1 ... objNameK`

    Names of form objects for which fixed values are specified. [Simple IDs](IDs.md#id).

- `expr1 ... exprK`

    [Expressions](Expression.md) whose values determine the fixed values for the form objects.

- `filterExpr1, ..., filterExprP`

    A list of [expressions](Expression.md) used as extra filters added to the form before the build.

- `topExpr`

    [Expression](Expression.md) whose value limits the build to its first records.

- `topGroupId1 ... topGroupIdT`

    Form object group [simple IDs](IDs.md#id) for which the limit is set per group object instead of for the whole build.

- `topPropertyExpr1 ... topPropertyExprT`

    Expressions whose values limit the records of the corresponding group objects.

- `offsetExpr`

    Expression whose value skips that many leading records of the build.

- `offsetGroupId1 ... offsetGroupIdF`

    Form object group simple IDs for which the offset is set per group object instead of for the whole build.

- `offsetPropertyExpr1 ... offsetPropertyExprF`

    Expressions whose values set the offset of the corresponding group objects.

### Examples

```lsf
// builds JSON from a list of properties
MESSAGE JSON FROM code = '1', message = 'OK';
```

```lsf
// the itemsJson property value is embedded into the result as a nested array:
// {"count":3,"items":[{"id":1,"name":"Item 1"},{"id":2,"name":"Item 2"},{"id":3,"name":"Item 3"}]}
itemsJson () = JSON FROM id = i, name = 'Item ' + i WHERE iterate(i, 1, 3) ORDER i;
MESSAGE JSON FROM count = 3, items = itemsJson();
```

```lsf
FORM testF
      OBJECTS j = INTEGER
      PROPERTIES ab = '34'
      OBJECTS i = INTEGER
      PROPERTIES name = 'Name ' + (i AS INTEGER)
;

run() {
    // builds JSON from a form, fixing object j and adding a filter on object i
    MESSAGE JSON (testF OBJECTS j = 4 FILTERS mod(i, 2) = 0);
}
```
