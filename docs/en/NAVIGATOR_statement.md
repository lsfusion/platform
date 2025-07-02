---
title: 'NAVIGATOR statement'
---

The `NAVIGATOR` statement - managing and configuring the [navigator](Navigator.md).

### Syntax

The syntax consists of nested blocks of *navigator statements*. The outer statement block is described after the keyword `NAVIGATOR`:

```
NAVIGATOR {
    navigatorStatement1 
    ...
    navigatorStatementN
}
```

Each `navigatorStatement` describes a single navigator statement, and at the end it may contain a nested block of navigator statements. There are three types of navigator statements:  

```
NEW elementDescription [options] [{ ... }];
MOVE name [caption] [options] [{ ... }];
name [caption] [options] [{ ... }];
SCHEDULE PERIOD intPeriod [FIXED] action;
```

where `elementDescription` in the `NEW` statement describes the type of element to be created, and can be in one of three types:

```
FOLDER name [caption] 
FORM [[name] [caption] =] formName
ACTION [[name] [caption] =] actionName
[[name] [caption] =] formElseActionName
```

A navigator statement must end with a semicolon if it does not contain a nested block of additional statements.

Navigator element options (`options`) can be listed one after the other in arbitrary order. The following set of options is supported:

```
WINDOW windowName [PARENT]
insertPosition
imageSetting
CLASS classExpr
HEADER headerExpr
SHOWIF showIfExpr
```

### Description

The `NAVIGATOR` statement allows to modify the navigator. Each navigator statement block enclosed in braces allows to change the descendants of a particular [navigator element](Navigator.md), which we will call the *current element*. In the outer block that follows the `NAVIGATOR` keyword, the current element is the root system folder `System.root`. There are three types of navigator statements:

-   The *create statement* (`NEW`) allows to create a new navigator element, making it a child of the current element. The created navigator elements can be of three types: folder, form element, and action element. The type of element to be created is specified by the keywords `FOLDER`, `FORM`, and `ACTION`. If the type is not specified, a form with the given name is searched first. If no form is found, the action is searched. The navigator statement block contained in this statement (if any) describes the descendants of the element being created.
-   The *move statement* (`MOVE`) allows to move an existing element to the current navigator element, making it a child element. Prior to this, the navigator element being moved is deleted from its previous location. The navigator statement block contained in this statement describes the descendants of the element being added. 
-   The *modify statement* allows to change the specified navigator element, which must be a descendant (not necessarily a child) of the current element. The navigator statement block contained in this statement describes the descendants of the specified element.

The move and modify statements allow to change the caption and options of a navigator item.

The hierarchy described within a single `NAVIGATOR` statement can have an arbitrary nesting level and describe any number of elements at each level.

### Parameters

- `name`

    Navigator element name. In the create statement, it is [a simple ID](IDs.md#id) and must be unique within the current [namespace](Naming.md#namespace), while in the rest of the statements it is a [composite ID](IDs.md#cid). The name does not have to be specified when creating a form element or action element. Here the form name or action name is used as the element name.

- `caption`

    Navigator element caption. [String literal](Literals.md#strliteral). In the create statement, if the caption is not specified, the caption will be the name of the created element. In the rest of the statements it modifies the existing caption.

- `actionName`

    The [action ID](IDs.md#propertyid) for which the navigator element will be created. The action must not take any parameters.

- `formName`

    The form name for which the navigator element will be created. Composite ID.

- `formElseActionName`

    The form name or action ID for which the navigator element will be created. The platform will first attempt to find a form with that name, and if that fails, it will search for an action with that ID. The action must not take any parameters.

- `SCHEDULE PERIOD intPeriod [FIXED] action`

    Creates a scheduler that executes `action` every `intPeriod` seconds. `FIXED` indicates that the period to the next action is counted from the start of the current action. By default, the period is counted from the end of the current action.

### Options

- `WINDOW windowName [PARENT]`

    Specifying the [window](Navigator_design.md) in which the descendants of this navigator element will be displayed (unless another window is specified for them), and the element itself, if the keyword `PARENT` is specified.

    - `windowName`

        Window name. Composite ID.

    - `PARENT`

        Keyword indicating that the current element will also be displayed in the specified window.

- `insertPosition`
 
    Specifying the insertion position of the navigator element. It can be specified in one of the following ways:

    - `BEFORE elementName`
    - `AFTER elementName`

        Specifying that the element must be added or moved just before (keyword `BEFORE`) or after (keyword `AFTER`) the specified navigator element. The specified element must be a child of the current element. 

        - `elementName`

            Navigator element name. Composite ID.

    - `FIRST`

        Keyword indicating that the navigator element should be added or moved to the beginning of the list of child elements of the current element.
       
    - `LAST`

        Keyword indicating that the navigator element should be added or moved to the end of the list of child elements of the current element. Unlike default addition, elements inserted using `LAST` will always be positioned after all elements added in the order of insertion.

    - `DEFAULT`

        Keyword indicating that the navigator element should be added or moved in the order of insertion to the list of child elements of the current element. This is the default value.

    If the option is specified in the modify statement, then the element itself must also be a direct child of the current element. Otherwise, the `MOVE` statement should be used.

- `imageSetting`

    Configuring the display of the navigator element's icon. By default, the icon or its absence is controlled by the [parameters](Working_parameters.md) `settings.defaultNavigatorImageRankingThreshold`, `settings.defaultNavigatorImage` and others. This option allows you to manually configure the icon display. It can have one of two forms:
 
    - `IMAGE [fileExpr]`

        Specifying the relative path to the image file that will be displayed as the navigator element's icon. If `fileExpr` is not specified, the default icon display mode is activated.

        - `fileExpr`

            [Expression](Expression.md) whose value specifies the path to the image file. The path is specified relative to the `images` directory.

      - `NOIMAGE`

          Keyword indicating that the navigator element should have no icon.

- `CLASS cssClassExpr`

    Specifying the name of the CSS class for the DOM element created for the navigator element component in HTML. This can be used to apply custom styles.

    - `cssClassExpr`

        Expression, whose value determines the class name.

- `HEADER headerExpr`

    Specifying a property that determines the dynamic header of the navigator element. If the return value of this property is `NULL`, the title of the navigator element will be automatically hidden.
 
    - `headerExpr`

        Expression whose value determines the dynamic header of the navigator element.
        
- `SHOWIF showIfExpr`

    Specifying a property that determines visibility of the navigator element. If the return value of this navigator element is `NULL`, then navigator element will not be displayed.

    - `showIfExpr`

        Expression whose value determines visibility of the navigator element.

### Examples

```lsf
FORM items;
FORM stocks;
FORM legalEntities;
FORM shipments;
hello()  { MESSAGE 'Hello world'; }
hi()  { MESSAGE 'Hi'; }

NAVIGATOR {
    // creating a new navigator folder and making all its descendants appear in a window with a vertical toolbar
    NEW FOLDER catalogs 'Directories' WINDOW toolbar { 
        // creating a form element for the items form in the folder, the default element name is the form name
        NEW items; 
    }
    catalogs {  // navigator element editing statement
        // creating a stocksNavigator form element for the stocks form and adding the last element
        // to the catalogs folder
        NEW FORM stocksNavigator 'Warehouses' = stocks; 
        // creating a form element named legalEntities in the catalogs folder right after the items element
        NEW legalEntities AFTER items; 
        NEW shipments;
    }
    // creating another folder, the elements of which will also be displayed in a window with a vertical toolbar
    NEW FOLDER documents 'Documents' WINDOW toolbar { 
        // the folders themselves will be displayed in the root window, and when the user selects one of them
        // in a window with a vertical toolbar the descendants of this particular folder will be shown
        NEW ACTION hi;   // creating an action element
        NEW ACTION h=hello;   // creating an action element
        // the statement to move the shipments element from the catalogs folder to the document folder
        // before the hello element
        MOVE shipments BEFORE h; 
    }
}
```


