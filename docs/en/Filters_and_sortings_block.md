---
title: 'Filter and sorting blocks'
---

The filter and order blocks of the [`FORM` statement](FORM_statement.md) – adding [filters](Form_structure.md#filters) and [orderings](Form_structure.md#sort) to the form structure, as well as [filter groups](Interactive_view.md#filtergroup) and [user filters](Interactive_view.md#userfilters) to the interactive form view.

## Fixed filters block {#fixedfilters}

### Syntax

```
FILTERS expression1, ..., expressionN
```

### Description

The fixed filters block adds filters that will be automatically applied when any form data is read. One block can list an arbitrary number of filters separated by a comma.

Each filter is defined with an [expression](Expression.md) that defines the filtering condition. In all expressions and context-dependent action operators you can use the names of the objects already declared on the form as parameters.

### Parameters

- `expression1, ..., expressionN`

    List of filter expressions.

### Examples

```lsf
CLASS Stock;
name = DATA ISTRING[100] (Stock);
region = DATA Region (Stock);

CLASS Group;
name = DATA ISTRING[100] (Group);

group = DATA Group(Sku);
nameGroup (Sku s) = name(group(s));

active = DATA BOOLEAN (Sku);

onStock = DATA NUMERIC[10,2] (Stock, Sku);

FORM onStock 'Balances' // creating a form in which the balances of products can be viewed
    OBJECTS r = Region PANEL // adding a region object
    // adding the property name of the region, when clicking on which the user can select it
    PROPERTIES name(r) SELECTOR 

    OBJECTS st = Stock // adding the warehouse object
    PROPERTIES name(st) READONLY // adding the warehouse name
    // adding a filter so that only warehouses of the selected region are shown
    FILTERS region(st) == r 

    OBJECTS s = Sku // adding products
    // adding the name of the group of products, assigning it groupName as the name of the property on the form, 
    // as well as the name and balance of the product
    PROPERTIES READONLY groupName = nameGroup(s), name(s), onStock(st, s) 
    FILTERS active(s) // turning it on to show only active products
;
```

## User filters block {#userfilters}

### Syntax

```
USERFILTERS formProperty1, ..., formPropertyN
```

### Description

The user filters block adds custom filters to the form. These are similar to those that the user can add themselves by pressing `F3`, however they cannot be removed.

Each filter is specified by a [property on a form](Properties_and_actions_block.md#name), which must already have been added to the form previously.

### Parameters

- `formProperty1, ..., formPropertyN`

    List of names of properties on a form for which filters are created.

### Example

```lsf
CLASS Stock;
name = DATA ISTRING[100] (Stock);

FORM stocks 'Stocks'
    OBJECTS st = Stock // add the 'Stock' object group
    PROPERTIES name(st) // add the 'name' property 
    USERFILTERS name(st) // add a user filter for the 'name' property
;
```

## Filter group block {#filtergroup}

### Syntax

```
[EXTEND] FILTERGROUP groupName [NONULL]
    FILTER caption1 expression1 [keystroke1] [DEFAULT]
    ...
    FILTER captionN expressionN [keystrokeN] [DEFAULT]
```

### Description

The filter group block adds a set of filters to the form. A special UI component is then created for them, making it possible to apply one filter at a time. If the keyword `EXTEND` is specified , the component is not created, but used for extension. In one block, you can define a single group of filters consisting of an arbitrary number of filters that will be shown to the user in the order of listing. 

Each filter is defined with an [expression](Expression.md) that defines the filtering condition. In all expressions and context-dependent action operators you can use the names of the objects already declared on the form as parameters.

### Parameters

<a className="lsdoc-anchor" id="filterName"/>

- `groupName` 

    Internal name of a filter group. [Simple ID](IDs.md#id). If the `EXTEND` keyword is specified, the platform will search the form for the created filter group with the specified name — otherwise a new filter group with the specified name will be created.

- `NONULL`

    When the `NONULL` keyword is specified, the `(All)` filter is not added to the group. The `(All)` filter allows no filters to be applied. This option can only be set when declaring `FILTERGROUP` (not in `EXTEND`).

- `caption1, ..., captionN`

    Captions that will be shown in the user interface for the corresponding filter being added. Each caption is defined with a [string literal](IDs.md#strliteral).

- `expression1, ..., expressionN`

    Expressions describing filters.

- `keystroke1, ..., keystrokeN`

    Keyboard shortcuts that, when pressed by the user, will select a corresponding filter in the group. Each keyboard shortcut is defined with a string literal and the definition method is similar to that for a parameter in the Java class method [Keystroke.getKeystroke(String)](http://docs.oracle.com/javase/7/docs/api/javax/swing/KeyStroke.html#getKeyStroke(java.lang.String)).

- `DEFAULT`

    A keyword specifying that the filter being added must be selected automatically when the form is added. Can be specified for one filter in the group only.


### Examples

```lsf
active = DATA BOOLEAN (Stock);

EXTEND FORM onStock // extending the previously created form with balances
    // creating a group of filters with one filter, which will be shown as a checkbox by which 
    // the user can enable/disable the filter
    FILTERGROUP stockActive 
        // adding filter for active warehouses only, which will be applied by pressing F11
        FILTER 'Active' active(st) 'F11' 
    // creating a new filter group in which the user can select one of the filters using the drop-down list
    FILTERGROUP skuAvailability 
        // adding a filter that will display only products on stock, which will be selected by pressing F10 
        // and will be automatically selected when the form is opened
        FILTER 'Is on stock' onStock (st, s) 'F10' DEFAULT 
;

// ...

EXTEND FORM onStock
    EXTEND FILTERGROUP skuAvailability
        FILTER 'Negative balances' onStock (st, s) < 0 'F9' // adding filter by expression
;
```


## Order block {#sort}

### Syntax

```
ORDERS [FIRST]
    formPropertyName1 [DESC] 
    ...
    formPropertyNameN [DESC]
```

### Description

An order block adds orderings to the form that will be automatically applied when any data are read on it. One block can list an arbitrary number of properties on the form separated by a comma in any sequence. These properties must be added to the form in advance.

### Parameters

- `FIRST`

    Keyword. Specifies that these sorts will be applied first, before all others.

- `formPropertyName1, ..., formPropertyNameN`

    Names of properties or form actions specifying the order.

- `DESC`

    Keyword. Specifies reverse order. By default, ascending order is used.

### Examples

```lsf
EXTEND FORM onStock // extending the previously created form with balances
    ORDERS name(s) // enabling ordering by warehouse name in the warehouse list
    ORDERS groupName, onStock(st, s) DESC // enabling ordering in ascending order of the group name, and inside
                                          // in descending order of the balance in the warehouse
                                          // it should be noted that the property is the property name on the 
                                          // form groupName, not just the property name nameGroupSku
;
```
