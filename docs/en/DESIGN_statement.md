---
title: 'DESIGN statement'
---

The `DESIGN` statement changes [form design](Form_design.md).

## Syntax

The syntax consists of nested blocks of *design statements*. The outer block, beginning with the keyword `DESIGN`, defines a [form](Forms.md) whose design will change: 

    DESIGN formName [caption] [CUSTOM] {
        designStatement1
        ...
        designStatementN
    }

Each `designStatement` describes one design statement. Design statements are of the following types: 

    NEW name [insertPos] [{...}];
    MOVE selector [insertPos] [{...}];  
    selector [{...}];   
    REMOVE selector;
    propertyName = value;

The first three statements – *create* (`NEW`), *move* (`MOVE`), and *modify* – may in turn contain nested blocks of design statements. The design statements *remove* (`REMOVE`) and *change property value* (`=`) are simple single statements. Each design statement must end with a semicolon if it does not contain a nested statement block.

<a className="lsdoc-anchor" id="selector"/>

Each `selector` can be one of the following types:

    componentName
    PROPERTY(formPropertyName)
    FILTERGROUP(filterGroupName)
    PARENT(selector)
    GROUP([propertyGroupSelector][,groupObjectTreeSelector])
    noGroupObjectTreeContainerType
    groupObjectTreeContainerType(groupObjectTreeSelector)

In turn, `groupObjectTreeSelector` can be one of two types:

    groupObjectSelector
    TREE treeSelector

## Description

Using the `DESIGN` statement the developer can manage the [design](Form_design.md) of the [interactive form view](Interactive_view.md) by creating, moving, and deleting containers and components, as well as changing their certain properties. By default, a [default design](Form_design.md#defaultDesign)  is created for each form, along with appropriate containers. If necessary, you can recreate the design without the default containers and previously configured settings. This is done using the keyword `CUSTOM`.  

Each block of design statements enclosed in braces alows to modify a particular component and its descendants. Let's call this component the *current component* or the *current container* if we know that the component should be a container in our case. In the external block following the `DESIGN` keyword, the `main` container is the current component. There are the following design statements:

- The *create statement* (`NEW`) allows to create a new container, making it a descendant of the current one. The newly-created container will be the current component in the design statements block contained in this statement.
- The *move statement* (`MOVE`)  allows to make an existing component a direct descendant of the current container. This component is first removed from the previous parent container. The component being moved becomes the current component in the design statements block contained in this statement. 
- The *modify* statement allows to modify the specified component which must be a descendant (not necessarily a child) of the current container. The specified element will be the current component in the design statements block contained in this statement.
- The *remove statement* (`REMOVE`) allows to remove a specified component from the component hierarchy. The component to be removed has to be a descendant of the current container. 
- The *change property value statement* (`=`) allows to change the value of the specified property of the current component.

The component hierarchy described in this statement can have an arbitrary number of nesting levels and describe any number of components and their properties at each level.

To access design components, you can use their names or address property components on the form (`PROPERTY`), the parent component (`PARENT`), property group components (`GROUP`), and other base components/default design components.

## Parameters

### Common parameters

- `formName`

    The name of the form being changed. [Composite ID](IDs.md#cid).

- `caption`

    The new form caption in the interactive view mode. [String literal](Literals.md#strliteral). The form caption doesn't change in the [navigator](Navigator.md).

- `name`

    The name of the container being created. [Simple ID](IDs.md#id).

- `insertPos`

    Component insertion or moving position. Specified with one of the following options:

    - `BEFORE` selector
    - `AFTER` selector 

        Specifies that the component should be added or moved before (`BEFORE`) or after (`AFTER`) the specified components. The specified component must be a child of the current container. 

    - `FIRST`

        A keyword specifying that the component should be added or moved to the first position in the list of the current container's children. 

- `propertyName`

    The name of the component property. The list of existing properties is provided in the tables below.

- `value`

    The value assigned to the corresponding container property. Acceptable value types are provided in the tables below.

### Component properties

|Property name|Description|Value type|Default value|Examples|
|---|---|---|---|---|
|`span`|todo|[Integer literal](Literals.md#intliteral)|`1`|`2`|
|`defaultComponent`|Specifying that this component should get the focus when the form is initialized. Can only be set for one component on the entire form|Extended [Boolean literal](Literals.md#booleanliteral)|`FALSE`|`TRUE`<br/>`FALSE`|
|`activated`|todo|[Logical literal](Literals.md#booleanliteral)|`FALSE`|`TRUE`<br/>`FALSE`|
|`fill`|Similar to the `flex` property, the only difference being that if a zero value is set, the `align` property is set to `START`, otherwise `align` is set to `STRETCH`|`NUMERIC` type literal|`0`|`1.5`|
|`size`|The base component size in pixels (a value of -1 means that the size is undefined)|A pair of [integer literals](Literals.md#intliteral) (width, height)|`(-1, -1)`|`(100, 20)`|
|`height`|The base component height in pixels.|Integer literal|`-1`|`50`|
|`width`|The base component width in pixels.|Integer literal|`-1`|`20`|
|`flex`|Extension coefficient. Value of a property similar to the [CSS flex-grow](http://www.w3schools.com/cssref/css3_pr_flex-grow.asp) property. Defines how much the component should grow in size relative to other components.|[`NUMERIC` type literal](Literals.md#numericliteral)|`0`|`0.25`|
|`shrink`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`alignShrink`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`align`<br/>`alignment`|Component alignment inside the container. Acceptable values: `START` (at the beginning), `CENTER` (in the center), `END` (at the end), `STRETCH` (stretched).|Alignment type|`START`|`STRETCH`|
|`alignCaption`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`marginTop`|Top margin|Integer literal|`0`|`3`|
|`marginRight`|Right margin|Integer literal|`0`|`1`|
|`marginBottom`|Bottom margin|Integer literal|`0`|`4`|
|`marginLeft`|Left margin|Integer literal|`0`|`1`|
|`margin`|Margin. Sets the same value to the following properties: `marginTop`, `marginRight`, `marginBottom`, `marginLeft`|Integer literal|`0`|`5`|
|`captionFont`|The font to be used for displaying caption of the component|String literal|depends on the component|`'Tahoma bold 16'`<br/>`'Times 12'`|
|`font`|The font to be used for displaying the component text — for example, property value, action caption, table text|String literal|depends on the component|`'Tahoma bold 16'`<br/>`'Times 12'`|
|`class`|todo|String literal|NULL|todo|
|`fontSize`|The size of the font to be used for displaying the component text|Numeric literal|depends on the component|`10`|
|`fontStyle`|The style of the font to be used for the component text May contain the words `'bold'` and/or `'italic'`, or an empty string|String literal|`''`|`'bold'`<br/>`'bold italic'`|
|`background`|The color to be used for the component background|[Literal of class `COLOR`](Literals.md#colorliteral)|`#FFFFFF`|`#FFFFCC`<br/>`RGB(255, 0, 0)`|
|`foreground`|The color to be used for the component text|Color|`NULL`|`#FFFFCC`<br/>`RGB(255, 0, 0)`|
|`showIf`|Specifies a condition under which the container will be displayed.|[Expression](Expression.md)|`NULL`|`isLeapYear(date)`<br/>`hasComplexity(a, b)`|

### Container properties

|Property name|Description|Value type|Default value|Examples|
|---|---|---|---|---|
|`caption`|Container header|String literal|`NULL`|`'Caption'`|
|`image`|todo|String literal|`NULL`|todo|
|`collapsible`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`border`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`collapsed`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`horizontal`|Container is horizontal|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`tabbed`|Container is tabbed|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`childrenAlignment`|Alignment of child components inside a container. Acceptable values: `START`, `CENTER`, `END`|Alignment type|`START`|`CENTER`|
|`alignCaptions`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`grid`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`wrap`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`resizeOverflow`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`custom`|todo|String literal|NULL|todo|
|`columns`|Number of columns in a `COLUMNS` type container<br/>**deprecated since version 5, use `lines`**|Integer literal|`1`|`3`|
|`lines`|Number of lines (rows or columns) in container|Integer literal|`1`|`3`|
|`lineSize`|todo|Integer literal|NULL|todo|
|`captionLineSize`|todo|Integer literal|NULL|todo|

### Properties of actions and properties on the form

|Property name|Description|Value type|Default value|Examples|
|---|---|---|---|---|
|`autoSize`|Automatic component size option. Applies to text components only|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`boxed`|Drawing a frame (box) around a component|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`panelCaptionVertical`|Indicates that the captions of property or action components should be drawn above the value on the panel|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelCaptionLast`|Indicates that the value should be drawn on the panel prior to thee property caption|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelCaptionAlignment`|Component caption alignment. Acceptable values: `START` (at the beginning), `CENTER` (in the center), `END` (at the end), `STRETCH` (stretched).|Alignment type|`START`|`STRETCH`|
|`editOnSingleClick`|Specifying that change event should be triggered after the property component is clicked once<br/>**deprecated, use `changeOnSingleClick` instead**|Extended Boolean literal|depends on the property|`TRUE`<br/>`FALSE`|
|`changeOnSingleClick`|Specifying that change event should be triggered after the property component is clicked once|Extended Boolean literal|depends on the property|`TRUE`<br/>`FALSE`|
|`focusable`|Specifying that the property (action) component or a table column can get focus|Extended Boolean literal|changeKey = `NULL`|`TRUE`<br/>`FALSE`|
|`hide`|Specifying that the property (action) component should be always hidden|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`regexp`|The regular expression that the property value must match during input|String literal|`NULL`|`'^((8\|\\+7)[\\- ]?)?(\\(?\\d\{3\}\\)?[\\- ]?)?[\\d\\- ]\{7,10\}$'`|
|`regexpMessage`|The message to be shown to the user if they enter a value that does not match the regular expression|String literal|default message|`'Incorrect phone number format'`|
|`pattern`|Property value formatting template. The syntax of template definition is similar to the [DecimalFormat](https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html) or [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) syntax, depending on the value type|String literal|`NULL`|`#,##0.00`|
|`maxValue`|The maximum numerical value that the property component can have|Integer literal|`NULL`|`1000000`<br/>`5000000000L`|
|`echoSymbols`|Specifying that a set of `*` characters will be displayed instead of the property value. Used for passwords, for example|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`noSort` |No sorting|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`valueSize`|Width and height of the property value cell in pixels|A pair of Integer literals (width, height)|`(-1, -1)`|`(100, 20)`|
|`valueHeight`|Height of the property value cell in pixels|Integer literal|depends on the property|`100`|
|`valueWidth`|Width of the property value cell in pixels|Integer literal|depends on the property|`100`|
|`captionHeight`|Height of the property caption in pixels|Integer literal|`-1`|`100`|
|`captionWidth`|Width of the property caption in pixels|Integer literal|`-1`|`100`|
|`charHeight`|Height of the property value cell in characters (rows).|Integer literal|depends on the property|`2`|
|`charWidth`|Width of the property value cell in characters|Integer literal|depends on the property|`10`|
|`valueFlex` |todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`changeKey`|The key that will trigger the property change event. The definition principle is similar to specifying a parameter in [Keystroke.getKeystroke(String)](https://docs.oracle.com/javase/8/docs/api/javax/swing/KeyStroke.html#getKeyStroke-java.lang.String-)|String literal|`NULL`|`'ctrl F6'`<br/>`'BACK_SPACE'`<br/>`'alt shift X'`|
|`changeKeyPriority`|todo|Integer literal|`NULL`|`'1000'`|
|`changeMouse`|todo|String literal|`NULL`|`'DBLCLK'`|
|`changeMousePriority`|todo|Integer literal|`NULL`|`'1000'`|
|`showChangeKey`|Specifying that the property caption will include that name of the key shortcut that will trigger the change event|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`focusable`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelColumnVertical`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`valueClass`|todo|String literal|NULL|todo|
|`captionClass`|todo|String literal|NULL|todo|
|`caption`|Caption of a property or action|String literal|caption of a property or action|`'Caption'`|
|`tag`|todo|String literal|NULL|todo|
|`imagePath`<br/>`image`|The path to the file with the image to be displayed as an action icon. The path is specified relative to the `images` folder|String literal|`NULL`|`'image.png'`|
|`comment`|Comment of a property or action|String literal|NULL|`'Comment'`|
|`commentClass`|Class of comment of property or action|String literal|NULL|`'comment-class'`|
|`panelCommentVertical`|Indicates that the comment of property or action should be drawn above or below the value on the panel|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelCommentFirst`|Indicates that the comment should be drawn on the panel before property value|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`panelCommentAlignment`|Comment component alignment inside the container. Acceptable values: `START` (at the beginning), `CENTER` (in the center), `END` (at the end), `STRETCH` (stretched).|Alignment type|`START`|`STRETCH`|
|`placeholder`|Placeholder of a property or action|String literal|placeholder of a property or action|`'Placeholder'`|
|`tooltip`|The tip to be shown when the cursor hovers over the caption of a property or action|String literal|Default tooltip|`'Tip'`|
|`toolTip`|Same as tooltip<br/>**deprecated since version 6, use `tooltip`**|String literal|Default tooltip|`'Tip'`|
|`valueToolTip`|The tip to be shown when the cursor hovers over the value of a property|String literal|Default tooltip|`'Tip'`|
|`valueAlignment`|Component value alignment. Acceptable values: `START` (at the beginning), `CENTER` (in the center), `END` (at the end), `STRETCH` (stretched).|Alignment type|`START`|`STRETCH`|
|`clearText`|Specifying that the current text should be reset when input starts|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`notSelectAll`|Specifying that the text is not selected at the start of editing|`FALSE`|`TRUE`<br/>`FALSE`|
|`askConfirm`|Specifies that an attempt to change the property (execute an action) will show a confirmation request|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`askConfirmMessage`|Text of the confirmation request shown when an attempt to change the property (execute the action) is made|String literal|default message|`'Are you sure you want to modify this property?'`|
|`toolbar`|todo|Extended Boolean literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`notNull`|Specifies that in case of a `NULL` property value, the component of this property should be highlighted|Extended Boolean literal|depends on the property|`TRUE`<br/>`FALSE`|
|`select`|todo|String literal|NULL|todo|

### Toolbar properties

|Property name|Description|Value type|Default value|Examples|
|---|---|---|---|---|
|`visible`|Specifying the visibility of the component|Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`showCountQuantity`|Show the row quantity calculation button|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`showCalculateSum`|Show the column sum calculation button|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`showGroup`|Show the grouping report button|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`showPrintGroupXls`|Show the XLS export button|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`showSettings`|Show the table setting button|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|

### Grid properties

|Property name|Description|Value type|Default value|Examples|
|---|---|---|---|---|
|`autosize`|todo|Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`boxed`|Drawing a frame (box) around a component|Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`tabVertical`|Specifying that focus will be moved from top to bottom (not from left to right)|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`quickSearch`|Specifying that the table will support quick element search|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|
|`headerHeight`|Header height in pixels|Integer literal|NULL|`60`|
|`resizeOverflow`|todo|Logical literal|`FALSE`|`TRUE`<br/>`FALSE`|
|`lineWidth`|todo|Integer literal|NULL|`60`|
|`lineHeight`|todo|Integer literal|NULL|`60`|

### Other properties

|Property name|Applies to|Description|Value type|Default value|Examples|
|---|---|---|---|---|---|
|`visible`|custom filter, class tree|Specifying the visibility of the component for setting custom filters (class tree)|Extended Boolean literal|`TRUE`|`TRUE`<br/>`FALSE`|

### `selector` parameters

- `componentName`

    Name of a design component. [Simple ID](IDs.md#id).

- `formPropertyName`

    [Property/action name on the form](Properties_and_actions_block.md#name).

- `filterGroupName`

    The name of [a filter group](Filters_and_sortings_block.md#filterName). [Simple ID](IDs.md#id).

- `propertyGroupSelector`

    The name of a [property group](Groups_of_properties_and_actions.md). [Simple ID](IDs.md#id).

- `groupObjectSelector`

    The name of an [object group on the form](Object_blocks.md#groupName). [Simple ID](IDs.md#id).

- `treeSelector`

    The name of [an object tree on the form](Object_blocks.md#treeName). [Simple ID](IDs.md#id).

- `noGroupObjectTreeContainerType`

    Type of form container:  

    - `BOX` – a common form container
    - `PANEL` – contains components of properties that are displayed in `PANEL` view and display group of which is undefined.
    - `TOOLBARBOX` – a common toolbar container with property components that are displayed in the panel, marked for placement on the `TOOLBAR`, and for which no object group is defined.
    - `TOOLBARLEFT `- the left part of the toolbar
    - `TOOLBARRIGHT` - the right part of the toolbar
    - `TOOLBAR` contains components of properties that are displayed in `TOOLBAR` view and display group of which is undefined.

- `groupObjectTreeContainerType`

    The type of an object group / tree container.

    - All types of containers of the `noGroupObjectTreeContainerType` form (identical semantics)
    - `GRIDBOX` - a table container<br/>**deprecated since version 5, use `GRID`**
    - `GRID` - a table component
    - `TOOLBARSYSTEM` - a system toolbar (number of records, group adjustment, etc.).
    - `FILTERGROUPS` - contains filter group components
    - `USERFILTER` - a component that displays custom filters<br/>**deprecated since version 5, use `FILTERS`**

## Examples

```lsf
DESIGN order { // customizing the design of the form, starting with the default design
               // marking that all changes to the hierarchy will occur for the topmost container
    // creating a new container as the very first one before the system buttons, 
    // in which we put two containers - header and specifications
    NEW orderPane FIRST { 
        fill = 1; // specifying that the container should occupy all the space available to it
        type = SPLITV; // specifying that the container will be a vertical splitter
        MOVE BOX(o) { // moving everything related to the object o to the new container
            PANEL(o) { // configuring how properties are displayed in the object o panel
                horizontal = FALSE; // making all descendants go from top to bottom
                NEW headerRow1 { // creating a container - the first row
                    horizontal = TRUE;
                    MOVE PROPERTY(date(o)) { // moving the order date property
                        // "override" the property caption in the form design (instead of the standard one)
                        caption = 'Date of the edited order'; 
                        // setting a hint for the order date property
                        toolTip = 'Input here the date the order was made'; 
                        background = #00FFFF; // making the background red
                    }
                    MOVE PROPERTY(time(o)) { // moving the order time property
                        foreground = #FF00FF; // making the color green
                    }
                    MOVE PROPERTY(number(o)) { // moving the order number property
                        // setting that the user should preferably be shown 5 characters
                        charWidth = 5; 
                    }
                    MOVE PROPERTY(series(o)); // moving the order series property
                }
                NEW headerRow2 {
                    horizontal = FALSE; // descendants - from top to bottom
                }
                MOVE PROPERTY(note(o));
            }

            size = (400, 300); //specifying that the container o.box should have a base size of 400x300 pixels
        }
        // creating a container that will store various specifications for the order
        NEW detailPane { 
            // marking that this container should be a tab panel, where its descendats are tabs
            tabbed = TRUE;
            MOVE BOX(d) { // adding a container with order lines as one of the tabs in the top panel
                caption = 'Lines'; // setting the caption of the tab panel
                // making the row number column never have focus
                PROPERTY(index(d)) { focusable = FALSE; } 
                GRID(d) {
                    // making sure that by default the focus when opening the form is set to the row table
                    defaultComponent = TRUE; 
                }
            }
            MOVE BOX(s) { // adding a container with sku totals as one of the detailPane tabs
                caption = 'Selection';
            }
        }
    }
}

// splitting the form definition into two statements (the second statement can be transferred to another module)
DESIGN order {
    // removing the container with print and export to xls buttons from the hierarchy, thereby making them invisible
    REMOVE TOOLBARLEFT; 
}
```

The output is the following form:

![](images/DESIGN_instruction.png)
