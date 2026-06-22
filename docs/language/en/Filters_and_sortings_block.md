---
slug: "/Filters_and_sortings_block"
title: 'Filter and sorting blocks'
---

The filter and order blocks of the [`FORM` statement](FORM_statement.md) – adding [filters](../paradigm/Form_structure.md#filters) and [orderings](../paradigm/Form_structure.md#sort) to the form structure, as well as [filter groups](../paradigm/Interactive_view.md#filtergroup) and [user filters](../paradigm/Interactive_view.md#userfilters) to the interactive form view.

## Fixed filters block {#fixedfilters}

### Syntax

```
FILTERS expression1 [filterType1], ..., expressionN [filterTypeN]
```

Options `filterType` can be listed after each expression. The following set of options is supported:

```
USER | FIXED
```

### Description

The fixed filters block adds filters that will be automatically applied when any form data is read. One block can list an arbitrary number of filters separated by a comma.

Each filter is defined with an [expression](Expression.md) that defines the filtering condition. In all expressions you can use the names of the objects already declared on the form as parameters.

A filter for a property being added to the form can also be defined with the `FILTER` option in the [property and action block](Properties_and_actions_block.md).

### Parameters

- `expression1, ..., expressionN`

    List of filter expressions.

- `filterType1, ..., filterTypeN`

    Optional filter types for corresponding expressions.

- `USER | FIXED`

    Keywords defining the filter type:
    `FIXED` is a fixed filter (default);
    `USER` is a user filter. Property must be added to the form in advance.

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

## Filter group block {#filtergroup}

### Syntax

```
[EXTEND] FILTERGROUP groupName [nullType]
    FILTER caption1 expression1 [binding1 ... bindingK] [DEFAULT]
    ...
    FILTER captionN expressionN [binding1 ... bindingK] [DEFAULT]
```

Where each `binding` has the following syntax:

```
[bindingType] bindingLiteral [showType]
```

### Description

The filter group block adds a set of filters to the form. A special UI component is then created for them, making it possible to apply one filter at a time. If the keyword `EXTEND` is specified, the component is not created, but used for extension. In one block, you can define a single group of filters consisting of an arbitrary number of filters that will be shown to the user in the order of listing.

Each filter is defined with an [expression](Expression.md) that defines the filtering condition. In all expressions you can use the names of the objects already declared on the form as parameters.

### Parameters

<a className="lsdoc-anchor" id="filterName"/>

- `groupName` 

    Internal name of a filter group. [Simple ID](IDs.md#id). If the `EXTEND` keyword is specified, the platform will search the form for the created filter group with the specified name — otherwise a new filter group with the specified name will be created.

- `nullType`

    Whether the group contains the `(All)` filter, which allows no filters to be applied. One of:

    - `NULL` - the `(All)` filter is added to the group (default value)
    - `NONULL` - the `(All)` filter is not added to the group; when the group is declared, the first filter becomes selected by default (unless the `DEFAULT` option specifies another one)

- `caption1, ..., captionN`

    Captions that will be shown in the user interface for the corresponding filter being added. Each caption is defined with a [string literal](Literals.md#strliteral).

- `expression1, ..., expressionN`

    Expressions describing filters.

- `binding1 ... bindingK`

    Bindings whose triggering by the user selects the corresponding filter in the group. Bindings can be specified in any number and order.

    - `bindingType`

        Binding kind. One of:

        - `KEY` - a keyboard shortcut (default value)
        - `MOUSE` - a mouse event

    - `bindingLiteral`

        String literal defining the binding. For a keyboard shortcut the definition method is similar to that for a parameter in the Java class method [Keystroke.getKeystroke(String)](http://docs.oracle.com/javase/7/docs/api/javax/swing/KeyStroke.html#getKeyStroke(java.lang.String)). The string format and additional options are the same as for the `CHANGEKEY` and `CHANGEMOUSE` options in [action options](Action_options.md).

    - `showType`

        Whether the binding is shown in the filter caption in the user interface. One of:

        - `SHOW` - shown (default value)
        - `HIDE` - not shown

- `DEFAULT`

    A keyword specifying that the filter being added must be selected automatically when the form is opened. Can be specified for one filter in the group only.


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
ORDERS [FIRST] expression1 [orderType1] [DESC], ..., expressionN [orderTypeN] [DESC]
```

Options `orderType` can be listed after each expression. The following set of options is supported:

```
USER | FIXED
```

### Description

An order block adds orderings to the form that will be automatically applied when any data are read on it. One block can list an arbitrary number of properties on the form separated by a comma in any sequence.

An ordering for a property being added to the form can also be defined with the `ORDER` option in the [property and action block](Properties_and_actions_block.md).

### Parameters

- `FIRST`

    Keyword. Specifies that these sorts will be applied first, before all others.

- `expression1, ..., expressionN`

    List of order expressions. As a rule, the [name of a property or action on the form](Properties_and_actions_block.md#name) specifying the order is used as the expression. An arbitrary [expression](Expression.md) can only define a fixed order.

- `orderType1, ..., orderTypeN`

    Sort options for the corresponding expression.

- `DESC`

    Keyword. Specifies reverse order. By default, ascending order is used.

- `USER | FIXED`

    Keywords that define the sort type:
    `USER` is a user sort (default for names of properties on the form) that can be overridden from the UI. Property must be added to the form in advance.
    `FIXED` is a fixed sort defined only in form code (default for other expressions).

### Examples

```lsf
EXTEND FORM onStock // extending the previously created form with balances
    ORDERS name(s) // enabling ordering by warehouse name in the warehouse list
    ORDERS groupName, onStock(st, s) DESC // enabling ordering in ascending order of the group name, and inside
                                          // in descending order of the balance in the warehouse
                                          // it should be noted that the property is the property name on the 
                                          // form groupName, not just the property name nameGroupSku
    ORDERS name(s) FIXED // fixed sort that is not replaced by user sorting
;
```
