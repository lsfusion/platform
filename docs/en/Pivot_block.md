---
title: 'Pivot table settings block'
---

Pivot table settings block of the [`FORM` statement](FORM_statement.md) manages the initial settings of the [*pivot table* view type](Interactive_view.md#property) in the interactive view of the form.

### Syntax

```
PIVOT 
    pivotSettingsBlock1
    ... 
    pivotSettingsBlockN
```

Where each `pivotSettingsBlocki` is a settings block. These blocks can be listed in any order. Each block can have one of the following syntaxes:

```
COLUMNS colFormPropertyList1, ..., colFormPropertyListM 
ROWS rowFormPropertyList1, ..., rowFormPropertyListK 
MEASURES measureFormPropertyName1, ..., measureFormPropertyNameL
objectGroupId pivotOptions 
```

Each of `colFormPropertyListi` and `rowFormPropertyListj` can describe either a single property on a form or a group of properties on a form:

```
formPropertyName
(formPropertyName1, ..., formPropertyNameX)
```

The `pivotOptions` options can be listed one after another in any order. The following set of options is supported:

```
pivotType
calcType
settingsType
```

### Description

Pivot table settings block allows you to set the initial settings for the form's pivot tables. It can be used to add properties on the form to the corresponding lists of columns (`COLUMNS` block), rows (`ROWS` block), and measures (`MEASURES` block) of the pivot table, as well as to specify initial values for some pivot table options.

### Parameters

- `formPropertyName`

    [Name of the property on a form](Properties_and_actions_block.md#name). 

- `measureFormPropertyName1, ..., measureFormPropertyNameL`

    List of property on a form names. Defines the properties on the form that are added to the measures lists of the corresponding pivot tables.

- `objectGroupId`

    [Object group ID](IDs.md#groupobjectid), to which the options from the described settings block apply.

- `pivotType`

    [String literal](Literals.md#strliteral) that defines the initial display mode of the pivot table. Can be equal to one of the following values:
    
    - `'Table'` (default value)
    - `'Table Bar Chart'`
    - `'Table Heatmap'`
    - `'Table Row Heatmap'`
    - `'Table Col Heatmap'`
    - `'Bar Chart'`
    - `'Stacked Bar Chart'`
    - `'Line Chart'`
    - `'Area Chart'`
    - `'Scatter Chart'`
    - `'Multiple Pie Chart'`
    - `'Horizontal Bar Chart'`
    - `'Horizontal Stacked Bar Chart'`
  
- `calcType`

    Specifying the initial aggregation function. It can be set using one of the keywords:

    - `SUM` - sum of values (default value)
    - `MAX` - maximum of values
    - `MIN` - minimum of values
         
- `settingsType`

    Specifying whether the pivot table settings are shown to the user. It can be specified by one of the keywords:

    - `SETTINGS` - settings are shown (default value)
    - `NOSETTINGS` - settings are not shown
    
### Example

```lsf
FORM PivotTest 
    OBJECTS s = Store
    PROPERTIES(s) name, storeSizeCode, storeSizeName
    PIVOT s 'Bar Chart' NOSETTINGS MAX
        ROWS name(s) COLUMNS storeSizeName(s) MEASURES storeSizeCode(s)    
;
```