---
title: 'Object blocks'
---

Object block of the [FORM statement](FORM_statement.md)  adds [objects](#objects) (including object groups) to the form structure, as well as [object trees](#tree) to the interactive form view.

## Object block {#objects}

### Syntax

    OBJECTS groupDeclaration1 [groupOptions1], ...., groupDeclarationN [groupOptionsN]

Each `groupDeclaration` is a declaration of an object group consisting of several objects:

    [groupName =] (objectDeclaration1, ..., objectDeclarationK)

 or an object group consisting of a single object:

    objectDeclaration

Each `objectDeclaration` declaring an object has the following syntax:

    [[name] [caption] =] classId [ON CHANGE actionId(param1, ..., paramM) | { actionOperator } ]

The declaration of each object group may be followed by a set of options called `groupOptions`:

    viewType
    INIT | FIXED
    PAGESIZE pageSize 
    AFTER groupName
    BEFORE groupName
    defaultObjectsType
    IN propertyGroup
    EXTID extID
    SUBREPORT [subReportExpression]

### Description

A single `OBJECTS` block can contain several comma-delimited declarations of [object groups](Interactive_view.md#objects). An object group can contain just one object or several ones. In case of a single object, you can use simplified syntax without specifying the name of an object group and using parentheses. The declaration of an object group may be followed by the options of this group. They are listed one after another in an arbitrary order.

### Parameters

<a className="lsdoc-anchor" id="groupName"/>

- `groupName`

    Name of an object group. [Simple ID](IDs.md#id). Must be specified if you need to access an object group consisting of several objects. If an object group consists of a single object, the name of the object group will be equal to the name of the object and doesn't need to be specified.

- `name`

    Object name. Simple ID. Must be specified if the object class is a [built-in class](Built-in_classes.md). If the object class is a [custom class](User_classes.md), the name doesn't need to be specified. In this case, it will be equal to the name of the class object. 

- `classId`

    [Object class ID](IDs.md#classid). 

- `caption`

    Caption of the object being added. [String literal](Literals.md#strliteral). If the caption is not specified, the class caption will become the object caption.

- `ON CHANGE actionName(param1, ..., paramM) | { actionOperator }`

    Specifying an [action](Actions.md) that will be called when the current object value is changed.

    - `actionID`

        [Action ID](IDs.md#propertyid).

    - `param1, ..., paramM`
    
        A list of object names on the form that will be used as action parameters. The number of these objects must be equal to the number of action parameters. The name of the object is defined with a [simple ID](IDs.md#id).

    - `actionOperator`

        [Context-dependent action operator](Action_operators.md#contextdependent). You can use the names of already declared objects on the form as parameters.

### Object group options

- `viewType`

    [Default view](Interactive_view.md#property) for an object group. It is specified with one of the keywords:
    
    - `PANEL` - *panel* view.
    - `TOOLBAR` - *toolbar* display mode.
    - `GRID` - *table column* view;. Used by default.

- `PAGESIZE pageSize`

    Specification of the number of readable objects in the table. By default, the quantity is determined dynamically depending on the size of the component in the user interface and equals to `3 * <number of visible rows in the table>`. A value of `0` means that all objects must be read.

    - `pageSize`

        Number of objects read. [Integer literal](Literals.md#intliteral).

- `AFTER` groupName
- `BEFORE` groupName

    Specifying that the object tree should be added to the form structure immediately before (keyword `BEFORE`) or after (keyword `AFTER`) of a specified object group. Typically used with the [form extension](Form_extension.md) mechanism . If a group is added before the group in a tree, then this group should the first in this tree. Accordingly, if a group is added after the group in a tree, this group should be the last in this tree.

    - `groupName`

        [Object group name](#groupName). 

- `defaultObjectsType`

    Specifying which object collection from the added object group will be current after the change of the active filters:

    - `FIRST`– specifies that the first object collection will be the [default objects](Interactive_view.md#defaultobject)
    - `LAST` - last one
    - `PREV` - previous one

  If this option is not specified, the platform determines the option to be used depending on the current filters.

- `IN propertyGroup`

    Specifying the [property/action group](Groups_of_properties_and_actions.md) that the object group belongs to. Used only in the [hierarchical](Structured_view.md#hierarchy) view.

    - `propertyGroup`
    
        The property group name. [Composite ID](IDs.md#cid).

- `EXTID extID`

    Specifying the name to be used for [export/import](Structured_view.md#extid) operations performed by this object group. Used only in the [structured](Structured_view.md) view.

    - `extId`

        String literal.

- `SUBREPORT [subReportExpression]`

    Specifies that you need to generate a separate [report](Print_view.md) file for this object group while [building the report hierarchy](Print_view.md#buildhierarchy).

    - `subReportExpression`

        The [expression](Expression.md) whose value will be used as the name of the  [report](Print_view.md) file for the created object group. You can use the names of already declared objects on the form as parameters. It is assumed that the values of these objects will be [passed](Open_form.md#params) when the form is opened [in the print view](In_a_print_view_PRINT.md) (if it's not done, they will be considered equal `NULL`).

### Examples

```lsf
CLASS Shipment;
// declaring the delivery form
FORM shipments 'Deliveries'
    OBJECTS s = Shipment // adding one object of the shipment class
                        PAGESIZE 100 // indicating that the table should always contain 100 rows

    // ... adding properties and filters to the form
;

// Declaring a form that will display the turnover of the product for a specified interval
name = DATA STRING[100] (Item);
revenue = DATA NUMERIC[16,2] (Item, DATE, DATE);

FORM revenues 'Product turnovers'
    OBJECTS interval = (dateFrom 'Date (from)' = DATE, dateTo 'Date (to)' = DATE) PANEL, // declaring a group of objects, consisting of 2 objects of the Date class with the appropriate captions, which will always be displayed as a panel
            i = Item // adding a list of products
    PROPERTIES VALUE(dateFrom), VALUE(dateTo) // adding to the form the properties of the date objects values, with which the user can select dates
    PROPERTIES name(i), revenue(i, dateFrom, dateTo) // adding the product name and the property with the product turnover for the date interval
;
```


```lsf
// creating a form for printing a price tag for a product
labelFile = DATA STRING[100] (Item);
printLabelFile (Item i)= OVERRIDE labelFile(i), 'MyModule_printLabel_i.jrxml' IF i IS Item;
FORM printLabel 'Price tag printing'
    OBJECTS i = Item               // adding the product for which the price tag will be printed
    REPORT printLabelFile(i)       // marking that a file whose name is stored in the printLabelFile property should be used as a template (it is assumed that the i value will be passed in the OBJECTS block)
                                   // for example, the user can input myLabel1.jrxml there, then the system will use a file named myLabel1.jrxml
//  ... other properties required for printing
;
```
  

## Object tree block {#tree}

### Syntax

    TREE [name] groupDeclaration1 [parentBlock1], ...., groupDeclarationN [parentBlockN] [treeOptions]

Each `groupDeclaration` is a declaration of an object group that is similar to the declaration in an object block described above. Each `parentBlock` can be described in one of the following ways:

    PARENT propertyId
    (PARENT propertyId1, ..., propertyIdK)

The first option is used if an object group for which the block is specified consists of a single object, the second one is used for groups of two and more objects.

The `treeOptions` options set may be specified after the declaration of each object tree.

    AFTER groupName
    BEFORE groupName

### Description

*Object tree block* lets you create an [object tree](Interactive_view.md#tree). The first specified object group will form a list of top-level objects, each of which will have a child list of objects of the second specified object group and so  on.

Use the `PARENT` block to create [hierarchical object groups](Interactive_view.md#treegroup). To do that, specify a property that will define the parent element for an object (or several objects if an object group contains several ones).

### Parameters

<a className="lsdoc-anchor" id="treeName"/>

- `name`

    The name of the object tree being created. [Simple ID](IDs.md#id). 

- `propertyId`

    [ID of the property](IDs.md#propertyid) defining the hierarchy for an object group consisting of a single object. The specified property must have a single parameter and return the parent object of the passed object as its value (or `NULL`  if the passed object is the top one).

- `propertyId1, ..., propertyIdK`

    A list of property ID's defining the hierarchy for an object group consisting of several objects. All specified properties must have the same number of parameters as the number of objects in the object group. Each of these properties must return one of the parent objects of the passed objects as a value (or `NULL` if the passed object collection is the top one). The first property should return the first parent object, the second property - the second object, etc.  on.

### Object tree options

- `AFTER groupName`
- `BEFORE groupName`

    Specifying that the object tree should be added to the form structure immediately before (keyword `BEFORE`) or after (keyword `AFTER`) of a specified object group. Typically used with the [form extension](Form_extension.md) mechanism . If a group is added before the group in a tree, then this group should the first in this tree. Accordingly, if a group is added after the group in a tree, this group should be the last in this tree.

- `groupName`

    [Object group name](#groupName). 

### Examples

```lsf
CLASS SkuGroup;
name = DATA ISTRING[100] (SkuGroup);
active = DATA BOOLEAN (SkuGroup);
parent = DATA SkuGroup (SkuGroup) AUTOSET;

CLASS Sku;
name = DATA ISTRING[100] (Sku);
skuGroup = DATA SkuGroup (Sku);


FORM skus 'Sku'
    TREE groupTree g=SkuGroup PARENT parent(g)
    PROPERTIES READONLY name(g)
    FILTERS active(g)

    OBJECTS s = Sku
    PROPERTIES(s) name
    FILTERS skuGroup(s) == g
;

CLASS Group1;
name = DATA STRING[100] (Group1);

CLASS Group2;
name = DATA STRING[100] (Group2);

CLASS Group3;
name = DATA STRING[100] (Group3);

in = DATA BOOLEAN (Group1, Group2);
in = DATA BOOLEAN (Group2, Group3);

FORM groups
    TREE groups g1 = Group1, g2 = Group2, g3 = Group3
    PROPERTIES READONLY name(g1), name(g2), name(g3)
    FILTERS in(g1, g2), in(g2, g3)
;
```
