---
title: 'Filters and sortings block'
---

The filter and order block of the [`FORM` statement](FORM_statement.md) – add [filters](Form_structure.md#filters) and [orderings](Form_structure.md#sort) to the form structure; add [filter groups](Interactive_view.md#filtergroup) to the interactive form view.

## Fixed filter block {#fixedfilters}

### Syntax

    FILTERS expression1, ..., expressionN

### Description

The fixed filters block adds filters that will be automatically applied when any form data is read. One block can list an arbitrary number of filters separated by a comma .

Each filter is defined with an  [expression](Expression.md) that defines the filtering condition. In all expressions and context-dependent action operators you can use the names of the objects already declared on the form as parameters.

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
    PROPERTIES name(r) SELECTOR // adding the property name of the region, when clicking on which the user can select it

    OBJECTS st = Stock // adding the warehouse object
    PROPERTIES name(st) READONLY // adding the warehouse name
    FILTERS region(st) == r // adding a filter so that only warehouses of the selected region are shown

    OBJECTS s = Sku // adding products
    PROPERTIES READONLY groupName = nameGroup(s), name(s), onStock(st, s) // adding the name of the group of products, assigning it groupName as the name of the property on the form, as well as the name and balance of the product
    FILTERS active(s) // turning it on to show only active products
;
```

## Filter group block {#filtergroup}

### Syntax

    [EXTEND] FILTERGROUP groupName
        FILTER caption1 expression1 [keystroke1] [DEFAULT]
        ...
        FILTER captionN expressionN [keystrokeN] [DEFAULT]

### Description

The filter group block adds a set of filters to the form. A special UI component is then created for them, making it possible to apply one filter at a time. If the keyword `EXTEND` is specified , the component is not created, but used for extension. In one block, you can define a single group of filters consisting of an arbitrary number of filters that will be shown to the user in the order of listing. 

Each filter is defined with an [expression](Expression.md) that defines the filtering condition. In all expressions and context-dependent action operators you can use the names of the objects already declared on the form as parameters.

### Parameters

<a className="lsdoc-anchor" id="filterName"/>
 
- `groupName` 

    Internal name of a filter group [Simple ID](IDs.md#id). If the `EXTEND` keyword is specified, the platform will search the form for the created filter group with the specified name — otherwise a new filter group with the specified name will be created.

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
    FILTERGROUP stockActive // creating a group of filters with one filter, which will be shown as a checkbox by which the user can enable/disable the filter
        FILTER 'Active' active(st) 'F11' // adding filter for active warehouses only, which will be applied by pressing F11
    FILTERGROUP skuAvailability // creating a new filter group in which the user can select one of the filters using the drop-down list
        FILTER 'Is on stock' onStock (st, s) 'F10' DEFAULT // adding a filter that will display only products on stock, which will be selected by pressing F10 and will be automatically selected when the form is opened
;

// ...

EXTEND FORM onStock
    EXTEND FILTERGROUP skuAvailability
        FILTER 'Negative balances' onStock (st, s) < 0 'F9' // adding filter by expression
;
```


## Order block {#sort}

### Syntax

    ORDER formPropertyName1 [DESC] 
          ...
          formPropertyNameN [DESC]

### Description

An order block adds orderings to the form that will be automatically applied when any data are read on it. One block can list an arbitrary number of properties on the form separated by a comma in any sequence. These properties must be added to the form in advance.

### Parameters

- `formPropertyName1, ..., formPropertyNameN`

    Names of properties or form actions specifying the order.

- `DESC`

    Keyword. Specifies reverse order. By default, ascending order is used.

### Examples

```lsf
EXTEND FORM onStock // extending the previously created form with balances
    ORDERS name(s) // enabling ordering by warehouse name in the warehouse list
    ORDERS groupName, onStock(st, s) DESC // enabling ordering in ascending order of the group name, and inside in descending order of the balance in the warehouse
                                            // it should be noted that the property is the property name on the form groupName, not just the property name nameGroupSku
;
```
