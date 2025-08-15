---
title: 'Property options'
---

When a [property](Properties.md) is declared in the [`=` statement](=_statement.md) a set of *property options* can be specified at the end of the declaration 

## Syntax

Options are listed one after another in arbitrary order, separated by spaces or line feeds:

```
propertyOption1 ... propertyOptionN
```

The following set of options is supported (the syntax of each option is indicated on a separate line):

```
IN groupName
viewType
ON eventType { actionOperator }
CHANGEKEY key [SHOW | HIDE]
CHANGEMOUSE key [SHOW | HIDE]
MATERIALIZED
TABLE tableName
INDEXED [LIKE | MATCH]
NONULL [DELETE] eventClause
AUTOSET
CHARWIDTH width [FLEX | NOFLEX]
REGEXP rexpr [message] 
ECHO
DEFAULTCOMPARE [compare]
```

## Description and parameters

- `IN groupName`

    Specifying the [group of properties and actions](Groups_of_properties_and_actions.md) to which the property belongs. If the option is not specified, then the property will belong by default to the group `System.private`.

    - `groupName`

        Group name. [Compound ID](IDs.md#cid).

<a className="lsdoc-anchor" id="persistent"/>

- `MATERIALIZED`

    Keyword marking the property as [materialized](Materializations.md). These properties will be stored in the database's [table](Tables.md) fields.

- `TABLE tableName`

    Specifies the [table](Tables.md) where the property will be stored. The number of table keys must be equal to the number of property arguments, and the argument classes must match the table key classes. If no table is specified, the property will automatically be placed in the "nearest" existing table in the system.

    - `tableName`
    
        Table name. Composite ID. 

<a className="lsdoc-anchor" id="indexed"/>

- `INDEXED`

    Keyword. If specified, an [index](Indexes.md) by this property is created. Similar to using the [`INDEX` statement](INDEX_statement.md). 

    - `LIKE`

        Keyword. If specified, creates GIN index instead of the usual index.

    - `MATCH`

        Keyword. If specified, creates GIN index and GIN index with to_tsvector instead of the usual index.

- `NONULL [DELETE] eventClause`

    Adds a [definiteness](Simple_constraints.md) constraint. If this constraint is violated as a result of some changes for some objects, either the corresponding message will be displayed, or, if `DELETE` is specified, such objects will be deleted.

    - `DELETE`

        Keyword that, if specified, then when the property becomes `NULL`, objects that are property arguments will be deleted.

    - `eventClause`

        [Event type description block](Event_description_block.md). Describes the event by which the property will be checked for `NULL`.

### Interactive view block

- `viewType`

    Specifying the type of [property view](Interactive_view.md#property) when added to the form.

    - `GRID` - table column
    - `TOOLBAR` - toolbar
    - `PANEL` - panel

  It is similar to specifying the `viewType` option in the [property block](Properties_and_actions_block.md) of the [`FORM` statement](FORM_statement.md). Thus, if this option is not specified either in the property options or in the property block directly on the form, the [default view](Interactive_view.md#property) of the property display group on the form is used.

- `ON eventType { actionOperator }`

    Specifying an action that will be the default handler of a certain [form event](Form_events.md) for all the interactive views of this property. Can be overridden in the property block of the `FORM` statement.

    - `eventType`

        Type of form event. Specified by one of the following options:

        - `CHANGE` - occurs when the user tries to change the value of a property.
        - `CHANGEWYS` - occurs when the user tries to change the value of the specified property using a special input mechanism. 
        - `GROUPCHANGE` - occurs when the user tries to change the property value for all objects in the table (group editing).  
        - `EDIT` - occurs when the user tries to edit the object that is the value of the specified property. 
        - `CONTEXTMENU [caption]` - the user has selected the specified item in the property context menu on the form. If necessary, you can also define the `caption` of this menu item ([string literal](Literals.md#strliteral)). If it is not specified, then, by default, it will be the same as the action caption.

    - `actionOperator`

        [Context-dependent action operator](Action_operators.md#contextdependent). An operator that defines the action executed on an event. You can use the parameters of the property itself as operator parameters.

### `DESIGN` statement default values block

- `CHARWIDTH width [FLEX | NOFLEX]`

    Specifying the [number of characters](Form_design.md#valueWidth) of the property value that should be visible to the user. Sets the value for the default design, can be overridden in a `DESIGN` statement.

    - `width`

        Number of characters. Integer literal. 

    - `FLEX`  

        Keyword. If specified, the extension coefficient of the property value is automatically set equal to its base size.

    - `NOFLEX`

        Keyword. If specified, the extension coefficient of the property value is automatically set equal to zero.

- `REGEXP rexpr [message]`

    Specifying a regular expression to which the property value should correspond after editing. Sets the value for the default design and can be overridden in the `DESIGN` statement.

    - `rexpr`

        A string literal that describes the regular expression. Rules are similar to the rules [accepted in Java](http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html) .

    - `message`

        String literal describing the message that will be shown to the user if they enter a value that does not match the regular expression. If not specified, a default message will be displayed.

- `ECHO`

    A keyword that causes asterisk `*` characters to be displayed instead of a property value. Used, for example, for passwords. Can be overridden in the `DESIGN` statement. 

- `CHANGEKEY key [SHOW | HIDE]`

    Specifies a [key combination](Form_events.md#keyboard) which triggers editing of this property. Sets the value for the default design and can be overridden in the `DESIGN` statement.

    - `key`

        [String literal](Literals.md#strliteral) describing the key combination. Syntax:
          ```
          keyStroke [;(modeKey=modeValue;)*]
          ```
    
          - `keyStroke`
              String representation of a key combination. The definition principle is similar to the way the parameter is specified in the Java class method KeyStroke.getKeyStroke(String). (http://docs.oracle.com/javase/7/docs/api/javax/swing/KeyStroke.html#getKeyStroke(java.lang.String)).
    
          - `(modeKey=modeValue;)*`
              Options specifying the execution conditions for keyStroke. The following options are supported:
    
              - `priority = priorityValue`
                  Priority, an integer value. If multiple properties meet the CHANGEKEY conditions, the one with the higher priority will be executed.
                  If the priority is not set, it is equal to the sequential number of the property in the form. Additionally, in any case, 1000 is added to the priority value if the object group matches.
    
              - `preview = previewValue`
                  All events are checked for execution twice: first with isPreview = true, then with isPreview = false. Supported `previewValue` values:
                  - `AUTO`, `ONLY` -> isPreview
                  - `NO` -> !isPreview
                  - `ALL` -> true
    
              - `dialog = dialogValue`
                  Checks whether CHANGEKEY should be executed in a dialog window. Supported `dialogValue` values:
                  - `AUTO`, `ALL` -> true
                  - `ONLY` -> isDialog
                  - `NO` -> !isDialog
    
              - `window = windowValue`
                  Checks whether CHANGEKEY should be executed in a modal window. Supported `windowValue` values:
                  - `AUTO`, `ALL` -> true
                  - `ONLY` -> isWindow
                  - `NO` -> !isWindow
    
              - `group = groupValue`
                  Checks whether the object group matches. Supported `groupValue` values:
                  - `AUTO`, `ALL` -> true
                  - `ONLY` -> equalGroup
                  - `NO` -> !equalGroup
    
              - `editing = editingValue`
                  Checks whether CHANGEKEY should be executed in property editing mode. Supported `editingValue` values:
                  - `AUTO` -> !(isEditing() && getEditElement().isOrHasChild(Element.as(event.getEventTarget())))
                  - `ALL` -> true
                  - `ONLY` -> isEditing
                  - `NO` -> !isEditing
    
              - `showing = showingValue`
                  Checks whether the property is currently visible on the form. Supported `showingValue` values:
                  - `AUTO`, `ONLY` -> isShowing
                  - `ALL` -> true
                  - `NO` -> !isShowing
    
              - `panel = panelValue`
                  Checks whether the property is located in a panel. Supported `panelValue` values:
                  - `AUTO` -> !isMouse || !isPanel
                  - `ALL` -> true
                  - `ONLY` -> isPanel
                  - `NO` -> !isPanel
    
              - `cell = cellValue`
                  Checks whether the property is located in a table cell. Supported `cellValue` values:
                   - `AUTO` -> !isMouse || isCell
                   - `ALL` -> true
                   - `ONLY` -> isCell
                   - `NO` -> !isCell
    
    
              For all options except `priority`, the default value is `AUTO`.

    - `SHOW`

        Keyword. When specified, the key combination will be displayed in the property caption. Used by default.

    - `HIDE`

        Keyword. When specified, the key combination will not be displayed in the property caption. 

- `CHANGEMOUSE key [SHOW | HIDE]`

    Specifies the mouse key combination that triggers the start of property editing. Sets the default value for the design, which can be overridden in the `DESIGN` instruction.

    - `key`

    [String literal](Literals.md#strliteral)describing a mouse key combination. Syntax:
        ```
        keyStroke [;(modeKey=modeValue;)*]
        ```

  	    - `keyStroke`
  		    String representation of a mouse key combination. Currently, the only supported value is `DBLCLK` — double click.
  		
  	    - `(modeKey=modeValue;)*`
  		    The syntax is identical to that of `CHANGEKEY`.		

    - `SHOW`

        Keyword indicating that the mouse key combination should be displayed in the property header. This is the default behavior.

    - `HIDE`

        Keyword indicating that the mouse key combination should not be displayed in the property header.

- `DEFAULTCOMPARE [compare]`

    Specifies a [default filter](Interactive_view.md#userfilters) type for the property.

    - `compare`

        Default filter type. [String literal](Literals.md#strliteral). Can be one the following values: `=`, `>`, `<`, `>=`, `<=`, `!=`, `CONTAINS`, `LIKE`. The default value is `=` for all data types except case-insensitive string types, for which the default value is `CONTAINS`. If `System.defaultCompareForStringContains` is enabled, default value is `CONTAINS` for all string data regardless of case sensitivity. Can be overridden in the `DESIGN` statement.

## Examples

```lsf
// property defined by the context-independent DATA property operator
cost 'Cost' (i) = DATA NUMERIC[12,3] (Item);

// property defined by expression
weightedSum 'Weighted amount' (a, b) = 2*a + 3*b;

// the caption of this property will be 'diff' and the parameters will be (a, b)
diff = a - b;

// property defined by DATA operator with additional property options
teamName 'Team name' = DATA BPSTRING[30](Team) IN baseGroup TABLE team; 
```
