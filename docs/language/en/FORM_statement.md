---
slug: "/FORM_statement"
title: 'FORM statement'
---

The `FORM` statement creates a [form](../paradigm/Forms.md). 

## Syntax

```
FORM name [caption] formOptions
    formBlock1
    ...
    formBlockN
;
```

After specifying the form name and caption, form options `formOptions` are specified in an arbitrary order

```
imageSetting
LOCALASYNC
```

After the form options, the blocks of the form `formBlock1 ... formBlockN` are described in the arbitrary order: 

```
OBJECTS ... 
TREE ...
PROPERTIES ...
FILTERS ...
[EXTEND] FILTERGROUP ...
ORDERS ...
PIVOT ...
[EVENTS] ...
HINTNOUPDATE LIST propertyId1, ..., propertyIdN
HINTTABLE LIST propertyId1, ..., propertyIdN
REPORT propertyExpression
REPORTFILES reportPath1, ..., reportPathN
FORMEXTID extID
EDIT className OBJECT objectName
LIST className OBJECT objectName 
```

Where each `reportPath` has one of the following syntaxes:

```
TOP propertyExpression
groupObjectName propertyExpression
```

## Description

The `FORM` statement declares a new form and adds it to the current [module](../paradigm/Modules.md). In addition, this statement is used to describe [the form structure](../paradigm/Form_structure.md), as well as its [static](../paradigm/Static_view.md) and partially [interactive](../paradigm/Interactive_view.md) (except [form design](../paradigm/Form_design.md)) views. At the beginning of the statement, name and captions are specified, followed by form options and the declaration containing an arbitrary number of blocks describing the structure of the form. They can be used in any order, provided that each block is declared after the blocks whose elements it uses. Each block can be used any number of times.

## Parameters

- `name`

    Form name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](../paradigm/Naming.md#namespace).

- `caption`

    Form caption. [String literal](Literals.md#strliteral). If the caption is not defined, the form's name will be its caption.

### Form options (`formOptions`)

- `imageSetting`

    Icon settings for the form. This option allows you to configure the icon manually. It can have one of the following forms:

    - `IMAGE [imageLiteral]`

        [Manual icon specification](../paradigm/Icons.md#manual) for the form. If `imageLiteral` is not provided, the [automatic assignment](../paradigm/Icons.md#auto) mode is enabled.

        - `imageLiteral`

            String literal whose value defines the icon.

    - `NOIMAGE`

        Keyword indicating that the form should have no icon.

- `LOCALASYNC`

    Keyword indicating that [local events](../paradigm/Events.md#type) handling will be performed after changes are displayed on the form.

### Form blocks (`formBlock1 ... formBlockN`) {#blocks}

- `OBJECTS ...`

    Adds objects to the form. [Object block syntax](Object_blocks.md) .

- `TREE ...`

    Adds an object tree to the form. [Syntax of the object tree block](Object_blocks.md#tree).

- `PROPERTIES ...`

    Adds [properties](../paradigm/Properties.md) and [actions](../paradigm/Actions.md) to the form. [Syntax of the property and action block](Properties_and_actions_block.md).

- `FILTERS ...`

    Adds fixed filters to the form. [Syntax of the fixed filters block](Filters_and_sortings_block.md#fixedfilters).

- `[EXTEND] FILTERGROUP ...`

    Adds a group of filters to the form or extends an existing one. [Syntax of a filter group block](Filters_and_sortings_block.md#filtergroup).

- `ORDERS ...`

    Adds sorting options to the form. [Syntax of the order block](Filters_and_sortings_block.md#sort).

- `PIVOT ...`

    Defines the initial settings for the [pivot table view type](../paradigm/Interactive_view.md#property). [Syntax of the pivot block](Pivot_block.md).

- `[EVENTS] ...`

    Defines actions that are executed on specific events. [Syntax of the event block](Event_block.md).

- `HINTNOUPDATE LIST propertyId1, ..., propertyIdN`

    Marks that the cached values of the listed properties should not be updated on changes in the session of this form.

    - `propertyId1, ..., propertyIdN`

        List of [property IDs](IDs.md#propertyid).

- `HINTTABLE LIST propertyId1, ..., propertyIdN`

    Marks that the changes of the listed properties should be materialized into a temporary table when the form reads its data. The list is defined in the same way as in the `HINTNOUPDATE` block.

- `FORMEXTID extID`

    Specifying the name to be used to [export/import](../paradigm/Structured_view.md#extid) this form. Used only in the [structured](../paradigm/Structured_view.md) view.

    - `extId`

        String literal.

- `REPORT propertyExpression`

    Specifying the property whose value will be used as the name of the [report](../paradigm/Print_view.md) file for [an empty](../paradigm/Static_view.md#empty) group. You can use the names of already declared objects on the form as parameters. It is assumed that the values of these objects will be [passed](../paradigm/Open_form.md) when the form is opened [in the print view](../paradigm/In_a_print_view_PRINT.md) (if it doesn't happen, they will be considered equal to `NULL`).

    - `propertyExpression`

        [Expression](Expression.md).

- `REPORTFILES reportPath1, ..., reportPathN`

    Specifying the properties whose values will be used as the names of the report files for the object groups of the form. The keyword `REPORTS` can be used as a synonym of `REPORTFILES`.

    - `TOP`

        Keyword. When specified, the property is set for the empty group. Fully equivalent to the `REPORT` block.

    - `groupObjectName`

        The name of the object group for which the property is set. Defined with a [simple ID](IDs.md#id). The object group is then marked as a separate report. Fully equivalent to the `SUBREPORT` option of the [object block](Object_blocks.md).

    - `propertyExpression`

        [Expression](Expression.md).

- `EDIT className OBJECT objectName`

    Sets the current form as the [edit](../paradigm/Interactive_view.md#edtClass) form for all objects of the specified class.

    - `className`
    
        The name of the [custom](../paradigm/User_classes.md) class. When editing objects of this class, the created form will be opened. Defined with a [composite ID](IDs.md#cid).

    - `objectName`
    
        The name of the form object that will be used for editing. Defined with a [simple ID](IDs.md#id).

- `LIST className OBJECT objectName`

    Sets the current form as the [list form](../paradigm/Interactive_view.md#edtClass) for the object of the specified class. 

    - `className`
    
        The name of the [custom](../paradigm/User_classes.md) class whose objects will be listed using the created form. Defined with a composite ID.

    - `objectName`
    
        The name of the form object whose current value will be used as the object being selected. Defined with a simple ID.

## Examples

```lsf
CLASS Document;

// declaring the Documents form
FORM documents 'Documents'
    // Adding one object of the Document class. The object will be available by this name 
    // in the DESIGN, SHOW, EXPORT, DIALOG, etc. operators.
    OBJECTS d = Document 


    // ... adding properties and filters to the form

    // marking that this form should be used when it is necessary to select a document, 
    // while the d object should be used as a return value
    LIST Document OBJECT d 
;

CLASS Item;

// declaring the Product form
FORM item 'Product'
    // adding an object of the Item class and marking that it should be displayed
    // in the panel (i.e., only one value is visible)
    OBJECTS i = Item PANEL 

    // ... adding properties and filters to the form

    // marking that this form should be used when it is necessary to add or edit a product
    EDIT Item OBJECT i 
;

// declaring a form with a list of Products
FORM items 'Products'
    OBJECTS i = Item

    // ... adding properties and filters to the form

    // adding buttons that will create and edit the product using the item form
    PROPERTIES(i) NEWSESSION NEW, EDIT 
;

CLASS Invoice;
CLASS InvoiceDetail;

// declaring the invoice print form
FORM printInvoice
    OBJECTS i = Invoice // adding an object of the invoice class for which printing will be executed

    // ... adding properties and filters to the form
;

// splitting the form definition into two statements (the second statement can be transferred to another module)
EXTEND FORM printInvoice
    // adding invoice lines, each of which will be used in the report as a detail
    OBJECTS d = InvoiceDetail 

    // ... adding properties and filters to the form

    // setting the report file for the object group d, marking it as a separate report
    REPORTFILES d 'printInvoiceDetail.jrxml'
;
// declaring an action that will open the invoice print form
print (Invoice invoice)  { PRINT printInvoice OBJECTS i = invoice; } 
```
