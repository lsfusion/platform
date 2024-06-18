---
title: 'Object blocks'
---

Object blocks of the [`FORM` statement](FORM_statement.md) - adding [object groups](Form_structure.md#objects) to the form structure, and [object trees](Interactive_view.md#tree) to the interactive form view.
    
## Object block {#objects}

### Syntax

```
OBJECTS groupDeclaration1 [groupOptions1], ...., groupDeclarationN [groupOptionsN]
```

Each `groupDeclaration` is a declaration of an object group consisting of several objects:

```
[groupName =] (objectDeclaration1, ..., objectDeclarationK)
```

or an object group consisting of a single object:

```
objectDeclaration
```

Each `objectDeclaration` declaring an object has the following syntax:

```
[[name] [caption] =] classId objectOptions
```

Object options `objectOptions` can be listed one after another in any order. The following set of options is supported:

```
ON CHANGE actionId(param1, ..., paramM)
ON CHANGE actionOperator 
EXTID objectExtID
```

After the declaration of each object group, the group options `groupOptions` can be listed in any order:

```
viewType
insertPosition
defaultObjectsType
PAGESIZE pageSize 
IN propertyGroup
EXTID extID
EXTKEY
SUBREPORT [subReportExpression]
BACKGROUND backgroundExpr
FOREGROUND foregroundExpr
```

### Description

A single `OBJECTS` block can contain several comma-delimited declarations of [object groups](Interactive_view.md#objects). An object group can contain just one object or several ones. In case of a single object, you can use simplified syntax without specifying the name of an object group and using parentheses. 

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

### Object options

- `ON CHANGE actionName(param1, ..., paramM)`

    Specifying an [action](Actions.md) that will be called when the current value of the object changes.

    - `actionID`

        [Action ID](IDs.md#propertyid).

    - `param1, ..., paramM`
    
        A list of object names on the form that will be used as action parameters. The number of these objects must be equal to the number of action parameters. The name of the object is defined with a simple ID.

- `ON CHANGE actionOperator`

    Creating an [action](Actions.md) that will be called when the current value of the object changes.

    - `actionOperator`

        [Context-dependent action operator](Action_operators.md#contextdependent). You can use the names of already declared objects on the form as parameters.

- `EXTID objectExtID`

    Specifying the name that will be used for [export/import](Structured_view.md#extid) of this form object. Used only in the [structured](Structured_view.md) view.

    - `objectExtID`

        String literal.

### Object group options

- `viewType`

    The [default view](Interactive_view.md#property) for properties of this object group. Specified by one of the following ways:

    - `PANEL`

        Keyword that, when specified, selects the *panel* view type
  
    - `TOOLBAR`

        Keyword that, when specified, selects the *toolbar* view type

    - `GRID`

        Keyword that, when specified, selects the *table column* view type. Used by default.

    - `PIVOT [pivotOptions]`

        When the `PIVOT` keyword is specified, the *pivot table* view type is selected. Options for this view type `pivotOptions` can be specified one after another in any order.

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
 
    - `MAP [tileProvider]`

        When the `MAP` keyword is specified, the *map* view type is selected. By default, this view uses OpenStreetMap maps. It is possible to use Google or Yandex maps. To do this you need to include the `Geo.lsf` module in the project, then obtain an API key for Google or Yandex and specify it in `Administration > Application > Settings > Navigation`.

        - `tileProvider`
      
            String literal that specifies the map source. Possible options:
      
            - `'openStreetMap'` (default value)
            - `'google'`
            - `'yandex'`
         
    - `CALENDAR`

        Keyword that, when specified, selects the *calendar* view type.

    - `CUSTOM renderFunction [HEADER expr]`

        When the `CUSTOM` keyword is specified, the custom view type is selected. 

        - `renderFunction`

            A string literal specifying the name of the JavaScript function that is responsible for displaying the data. This function must be located in a .js file included in the project resources and loaded for use on the client. It should return a JavaScript object that contains three functions: 
      
            - `render(element, controller)`
            - `update(element, controller, list, options)`
            - `clear(element)` (optional)

            A more detailed description of the mechanism can be found in the article [How-to: Custom Components (Objects)](How-to_Custom_components_objects.md).

        - `expr`

            Expression whose value must be an object of the JSON class. It is used to pass data that does not depend on the values of the described object group.

<a className="lsdoc-anchor" id="insertPosition"/>

- `insertPosition`

    Specifying the insertion position of the object group within the list of object groups. Most often used together with the [form extension mechanism](Form_extension.md). It can be specified in one of the following ways:

    - `AFTER groupName`
    - `BEFORE groupName`

      The object group will be added to the form structure directly before (keyword `BEFORE`) or after (keyword `AFTER`) the specified object group. If the group before (after) which it is to be added is in the tree, it must be the first (last) in that tree.

        - `groupName`

            Object group name. Simple ID.
      
    - `FIRST`

        Keyword indicating that the object group will be added to the beginning of the list.
  
    - `LAST`

        Keyword indicating that the object group will be added to the end of the list. Unlike default addition, object groups inserted using `LAST` will always be positioned after all object groups added in the order of declaration.

    - `DEFAULT`

        Keyword indicating that the object group is added in the order of declaration. This is the default value.
  
- `defaultObjectsType`

    Specifying which object collection from the added object group will be current after the active filters are changed. Specified by one of the keywords:

    - `FIRST`– specifies that the first object collection (according to the current order) will be the [default objects](Interactive_view.md#defaultobject)
    - `LAST` - last object collection
    - `PREV` - the previous (or closest possible) object collection
    - `NULL` - none (reset)

    If this option is not specified, the platform determines the option to be used depending on the current filters.

- `PAGESIZE pageSize`

    Specification of the number of readable objects in the table. By default, the quantity is determined dynamically depending on the size of the component in the user interface and equals to `3 * <number of visible rows in the table>`. A value of `0` means that all objects must be read.

    - `pageSize`

        Number of objects read. [Integer literal](Literals.md#intliteral).

- `IN propertyGroup`

    Specifying the [property and action group](Groups_of_properties_and_actions.md) that the object group belongs to. Used only in the [hierarchical](Structured_view.md#hierarchy) view.

    - `propertyGroup`
    
        The property and action group name. [Composite ID](IDs.md#cid).

- `EXTID extID`

    Specifying the name to be used for [export/import](Structured_view.md#extid) of this object group. Used only in the structured view.

    - `extId`

        String literal.

- `EXTKEY`

    When keyword `EXTKEY` is specified the values of objects and properties of this object group are represented in a structured view as key-value pairs, where the key is the value of the object (set of objects) and the value is the property values. By default, they are represented as an array with lists of property values.

- `SUBREPORT [subReportExpression]`

    Specifies that you need to generate a separate [report](Print_view.md) file for this object group while [building the report hierarchy](Print_view.md#buildhierarchy).

    - `subReportExpression`

        The [expression](Expression.md) whose value will be used as the name of the  [report](Print_view.md) file for the created object group. You can use the names of already declared objects on the form as parameters. It is assumed that the values of these objects will be [passed](Open_form.md#params) when the form is opened [in the print view](In_a_print_view_PRINT.md) (if it's not done, they will be considered equal `NULL`).

- `BACKGROUND backgroundExpr`

    Specifying the background color of property cells belonging to this object group.
    
    - `backgroundExpr`

        Expression whose value determines the background color.

- `FOREGROUND foregroundExpr`

    Specifying the foreground color of property cells belonging to this object group.

    - `foregroundExpr`

        Expression whose value determines the foreground color.

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
    // declaring a group of objects, consisting of 2 objects of the Date class with the appropriate captions,
    // which will always be displayed as a panel
    OBJECTS interval = (dateFrom 'Date (from)' = DATE, dateTo 'Date (to)' = DATE) PANEL, 
            i = Item // adding a list of products
    // adding to the form the properties of the date objects values, with which the user can select dates
    PROPERTIES VALUE(dateFrom), VALUE(dateTo) 
    // adding the product name and the property with the product turnover for the date interval
    PROPERTIES name(i), revenue(i, dateFrom, dateTo) 
;
```


```lsf
// creating a form for printing a price tag for a product
labelFile = DATA STRING[100] (Item);
printLabelFile (Item i)= OVERRIDE labelFile(i), 'MyModule_printLabel_i.jrxml' IF i IS Item;
FORM printLabel 'Price tag printing'
    OBJECTS i = Item  // adding the product for which the price tag will be printed
    // marking that a file whose name is stored in the printLabelFile property should be used as a template 
    // (it is assumed that the i value will be passed in the OBJECTS block)
    REPORT printLabelFile(i)       
    // for example, the user can input myLabel1.jrxml there, then the system will use a file named myLabel1.jrxml
    //  ... other properties required for printing
;
```
  

## Object tree block {#tree}

### Syntax

```
TREE [name] groupDeclaration1 [parentBlock1], ...., groupDeclarationN [parentBlockN] [insertPosition]
```

Each `groupDeclaration` is a declaration of an object group that is fully analogous to the [declaration in the object block](#objects) described above. Each `parentBlock` can be described in one of two ways:

```
PARENT parentExpr
(PARENT parentExpr1, ..., parentExprK)
```

The first option is used if an object group for which the block is specified consists of a single object, the second one is used for groups of two and more objects.


### Description

*Object tree block* lets you create an [object tree](Interactive_view.md#tree). The first specified object group will form a list of top-level objects, each of which will have a child list of objects of the second specified object group and so  on.

Use the `PARENT` block to create [hierarchical object groups](Interactive_view.md#treegroup). To do that, specify a property that will define the parent element for an object (or several objects if an object group contains several ones).

### Parameters

<a className="lsdoc-anchor" id="treeName"/>

- `name`

    The name of the object tree being created. [Simple ID](IDs.md#id). 

- `parentExpr`

    Expression that defines a hierarchy for a group of objects consisting of a single object. This expression must create a property that has exactly one parameter and returns the parent object for the object passed as input (or `NULL` if the passed object is at the top level).

- `parentExpr1, ..., parentExprK`

    A list of expressions that define a hierarchy for an object group consisting of multiple objects. These expressions should create properties with a number of parameters equal to the number of objects in the group. Each of these properties should return one of the parent objects for the object collection passed as input (or `NULL` if the passed object collection is at the top level). The first property should return the first object of the parent object collection, the second property - the second object, and so on.

- `insertPosition`

    Specifying the insertion position of tree object groups in the list of object groups. It has syntax fully analogous to the [same option in the object block](#insertPosition).

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
