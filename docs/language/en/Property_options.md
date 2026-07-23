---
slug: "/Property_options"
title: 'Property options'
---

When a [property](../paradigm/Properties.md) is declared in the [`=` statement](=_statement.md) a set of *property options* can be specified at the end of the declaration 

## Syntax

Options are listed one after another in arbitrary order, separated by spaces or line feeds:

```
propertyOption1 ... propertyOptionN
```

The following set of options is supported (the syntax of each option is indicated on a separate line):

```
IN groupName
viewType
customView
ON eventType { actionOperator }
CHANGEKEY key [SHOW | HIDE]
CHANGEMOUSE key [SHOW | HIDE]
STICKY | NOSTICKY
syncType
MATERIALIZED [dbName]
TABLE tableName
INDEXED [dbName] [indexType]
COMPLEX | NOCOMPLEX
PREREAD
HINT | NOHINT
NONULL [DELETE] eventClause
AUTOSET
CHARWIDTH width [FLEX | NOFLEX]
PATTERN patternStr
REGEXP rexpr [message] 
ECHO
DEFAULTCOMPARE compare
EVENTID eventId
LAZY [WEAK | STRONG] [WAIT | NOWAIT]
EXTID extId
imageSetting
annotationSetting
```

## Description and parameters

- `IN groupName`

    Specifying the [group of properties and actions](../paradigm/Groups_of_properties_and_actions.md) to which the property belongs. If the option is not specified, then the property will belong by default to the group `System.private`.

    - `groupName`

        Group name. [Compound ID](IDs.md#cid).

<a className="lsdoc-anchor" id="persistent"/>

- `MATERIALIZED [dbName]`

    Keyword marking the property as [materialized](../paradigm/Materializations.md). These properties will be stored in the database's [table](../paradigm/Tables.md) fields.

    - `dbName`

        [String literal](Literals.md#strliteral) that specifies the physical field (column) name in the database. If omitted, the name is generated automatically.

- `TABLE tableName`

    Specifies the [table](../paradigm/Tables.md) where the property will be stored. The number of table keys must be equal to the number of property arguments, and the argument classes must match the table key classes. If no table is specified, the property will automatically be placed in the "nearest" existing table in the system.

    - `tableName`
    
        Table name. Composite ID. 

<a className="lsdoc-anchor" id="indexed"/>

- `INDEXED`

    Keyword. If specified, an [index](../paradigm/Indexes.md) by this property is created. It can be used only for a [materialized](../paradigm/Materializations.md) property. Similar to using the [`INDEX` statement](INDEX_statement.md).

    - `dbName`

        String literal that specifies the physical index name in the database. If omitted, the name is generated automatically.

    - `indexType`

        Optional choice of a special index type. If omitted, a usual index is created.

        - `LIKE`

            Keyword. For string properties, keeps the usual index and additionally tries to create a specialized GIN index for `LIKE` operations.

        - `MATCH`

            Keyword. Creates an index for `MATCH` operations.

            - for a single field of type `TSVECTOR`, only the specialized GIN index by that field is created
            - for string properties, the usual index is kept and specialized indexes for `MATCH` and `LIKE` are additionally attempted
            - the string `MATCH` index is built on `to_tsvector` with the current full-text search language

            Specialized `LIKE` / `MATCH` indexes are created only when the current DB adapter has the corresponding trigram/full-text support enabled. The usual index is created regardless of that support.

- `COMPLEX | NOCOMPLEX`

    Keywords marking the property's calculation as complex (`COMPLEX`) or explicitly not (`NOCOMPLEX`). A complex property is always read in advance. If neither is specified, the property is complex only when it depends, directly or transitively, on a complex property.

- `PREREAD`

    Keyword that makes the property's value be read in advance during change processing, instead of being recomputed inline.

- `HINT | NOHINT`

    Keywords controlling automatic incremental caching of a property's changes. `HINT` forces this caching for the property when applicable; `NOHINT` disables it for the property and the properties depending on it. Without either, the platform decides automatically (heuristically).

- `NONULL [DELETE] eventClause`

    Adds a [definiteness](../paradigm/Simple_constraints.md) constraint. If this constraint is violated as a result of some changes for some objects, either the corresponding message will be displayed, or, if `DELETE` is specified, such objects will be deleted.

    - `DELETE`

        Keyword that, if specified, then when the property becomes `NULL`, objects that are property arguments will be deleted.

    - `eventClause`

        [Event type description block](Event_description_block.md). Describes the event by which the property will be checked for `NULL`.

- `AUTOSET`

    Keyword marking a [data property](../paradigm/Data_properties_DATA.md) with an object parameter and an object value for automatic setting on object creation. When an object is created with the `AUTOSET` option (for example, `NEW ... AUTOSET`) and its class matches the property's parameter class or a subclass, the platform sets this property for it to the current object of the value class, if one is available.

### Interactive view block

- `viewType`

    Specifying the type of [property view](../paradigm/Interactive_view.md#property) when added to the form.

    - `GRID` - table column
    - `TOOLBAR` - toolbar
    - `PANEL` - panel

  It is similar to specifying the `viewType` option in the [property block](Properties_and_actions_block.md) of the [`FORM` statement](FORM_statement.md). Thus, if this option is not specified either in the property options or in the property block directly on the form, the [default view](../paradigm/Interactive_view.md#property) of the property display group on the form is used.

- `customView`

    Specifying a custom view of the property value when the property is added to the form. It is similar to specifying the `customView` option in the [property block](Properties_and_actions_block.md) of the [`FORM` statement](FORM_statement.md), which can override it.

- `ON eventType { actionOperator }`

    Specifying an action that will be the default handler of a certain [form event](../paradigm/Form_events.md) for all the interactive views of this property. Can be overridden in the property block of the `FORM` statement.

    - `eventType`

        Type of form event. Specified by one of the following options:

        - `CHANGE` - occurs when the user tries to change the value of a property.
        - `CHANGEWYS` - occurs when the user tries to change the value of the specified property using a special input mechanism. 
        - `GROUPCHANGE` - occurs when the user tries to change the property value for all objects in the table (group editing).  
        - `EDIT` - occurs when the user tries to edit the object that is the value of the specified property. 
        - `CONTEXTMENU [caption]` - the user has selected the specified item in the property context menu on the form. If necessary, you can also define the `caption` of this menu item ([string literal](Literals.md#strliteral)). If it is not specified, then, by default, it will be the same as the action caption.

    - `actionOperator`

        [Context-dependent action operator](Action_operators.md#contextdependent). An operator that defines the action executed on an event. You can use the parameters of the property itself as operator parameters.

- `imageSetting`

    Icon settings for the property. This option allows you to configure the icon manually. It can have one of the following forms:

    - `IMAGE [imageLiteral]`

        [Manual icon specification](../paradigm/Icons.md#manual) for the property. If `imageLiteral` is not provided, the [automatic assignment](../paradigm/Icons.md#auto) mode is enabled.

        - `imageLiteral`

            String literal whose value defines the icon.

    - `NOIMAGE`

        Keyword indicating that the property should have no icon.

- `annotationSetting`

Property annotation. Begins with `@@`. The following annotations are supported:

    - `@@deprecated`
    - `@@deprecated(since)`
    - `@@deprecated(since, message)`

        Marks the property as deprecated and not recommended for use.
        The plugin displays such properties as strikethrough.

      - `since`
          String literal indicating the platform version since which the property is considered deprecated.

      - `message`
          String literal providing an explanation of why the property is marked as deprecated.

### `DESIGN` statement default values block

- `CHARWIDTH width [FLEX | NOFLEX]`

    Specifying the [number of characters](../paradigm/Form_design.md#valueWidth) of the property value that should be visible to the user. Sets the value for the default design, can be overridden in a `DESIGN` statement.

    - `width`

        Number of characters. Integer literal. 

    - `FLEX`  

        Keyword. If specified, the extension coefficient of the property value is automatically set equal to its base size.

    - `NOFLEX`

        Keyword. If specified, the extension coefficient of the property value is automatically set equal to zero.

- `PATTERN patternStr`

    Specifies the formatting pattern for the property value. The syntax for defining the pattern is similar to [DecimalFormat](https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html) or [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) depending on the value type.

    - `patternStr`

      String literal defining the formatting pattern. May be a [localizable](../paradigm/Internationalization.md) string.

- `REGEXP rexpr [message]`

    Specifying a regular expression to which the property value should correspond after editing. Sets the value for the default design and can be overridden in the `DESIGN` statement.

    - `rexpr`

        String literal that describes the regular expression. Rules are similar to the rules [accepted in Java](http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html). May be a localizable string.

    - `message`

        String literal describing the message that will be shown to the user if they enter a value that does not match the regular expression. May be a localizable string. If not specified, a default message will be displayed.

- `ECHO`

    A keyword that causes asterisk `*` characters to be displayed instead of a property value. Used, for example, for passwords. Can be overridden in the `DESIGN` statement. 

- `CHANGEKEY key [SHOW | HIDE]`

    Specifies a [key combination](../paradigm/Form_events.md#keyboard) which triggers editing of this property. Sets the value for the default design and can be overridden in the `DESIGN` statement.

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
                  - `auto`, `only` -> isPreview
                  - `no` -> !isPreview
                  - `all` -> true
    
              - `dialog = dialogValue`
                  Checks whether CHANGEKEY should be executed in a dialog window. Supported `dialogValue` values:
                  - `auto`, `all` -> true
                  - `only` -> isDialog
                  - `no` -> !isDialog
    
              - `window = windowValue`
                  Checks whether CHANGEKEY should be executed in a modal window. Supported `windowValue` values:
                  - `auto`, `all` -> true
                  - `only` -> isWindow
                  - `no` -> !isWindow
    
              - `group = groupValue`
                  Checks whether the object group matches. Supported `groupValue` values:
                  - `auto`, `all` -> true
                  - `only` -> equalGroup
                  - `no` -> !equalGroup
    
              - `editing = editingValue`
                  Checks whether CHANGEKEY should be executed in property editing mode. Supported `editingValue` values:
                  - `auto` -> !(isEditing() && getEditElement().isOrHasChild(Element.as(event.getEventTarget())))
                  - `all` -> true
                  - `only` -> isEditing
                  - `no` -> !isEditing
    
              - `showing = showingValue`
                  Checks whether the property is currently visible on the form (for properties with `hide` in design). Supported `showingValue` values:
                  - `auto`, `only` -> isShowing
                  - `all` -> true
                  - `no` -> !isShowing
    
              - `panel = panelValue`
                  Checks whether the property is located in a panel. Supported `panelValue` values:
                  - `auto` -> !isMouse || !isPanel
                  - `all` -> true
                  - `only` -> isPanel
                  - `no` -> !isPanel
    
              - `cell = cellValue`
                  Checks whether the property is located in a table cell. Supported `cellValue` values:
                   - `auto` -> !isMouse || isCell
                   - `all` -> true
                   - `only` -> isCell
                   - `no` -> !isCell
    
    
              For all options except `priority`, the default value is `auto`.

    - `SHOW`

        Keyword. When specified, the key combination will be displayed in the property caption. Used by default.

    - `HIDE`

        Keyword. When specified, the key combination will not be displayed in the property caption. 

- `CHANGEMOUSE key [SHOW | HIDE]`

    Specifies the mouse key combination that triggers the start of property editing. Sets the default value for the design, which can be overridden in the `DESIGN` instruction.

    - `key`

    [String literal](Literals.md#strliteral), describing a mouse key combination. Syntax:
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

- `STICKY` | `NOSTICKY`

    Keywords. `STICKY` indicates that the property in the table will be pinned to the left and remain visible when scrolling to the right. `NOSTICKY` removes this pinning. By default, `STICKY` or `NOSTICKY` is determined heuristically.

- `syncType`

    Determines whether the property's actions are executed asynchronously:

    - `WAIT` — asynchronous execution is disabled.
    - `NOWAIT` — asynchronous execution is enabled.

    If omitted, the actions are executed synchronously.

- `DEFAULTCOMPARE compare`

    Specifies a [default filter](../paradigm/Interactive_view.md#userfilters) type for the property.

    - `compare`

        Default filter type. [String literal](Literals.md#strliteral). Can be one the following values: `=`, `>`, `<`, `>=`, `<=`, `!=`, `=*`, `=@`. The default value is `=` for all data types except case-insensitive string types, for which the default value is `=@` or `=*` (if `System.defaultCompareForStringContains` is enabled, then except all string types regardless of case sensitivity). Default value `=@` or `=*` depends on `System.defaultCompareSearchInsteadOfContains` (true - `=@`, false - `=*`, true is by default). Can be overridden in the `DESIGN` statement.

- `EVENTID eventId`

    Specifies special input mode for the property.
  
    - `eventId`
        
        String literal. Now only `SCANNER` value is supported. Enables special keydown handling mode to detect GS (group separator) input.

- `LAZY [WEAK | STRONG] [WAIT | NOWAIT]`

	Specifies the caching level of a property (the property value is cached on the application server if reading is done for all fixed parameters). 
	`WEAK` and `STRONG` specify what is evicted from the cache when the property changes.
	`WEAK` means that upon any change the property depends on, all its cached values are evicted.
	`STRONG` means that the cache will not be entirely cleared upon any property change, but instead, an event will be triggered, and specific values will be cleared. It requires the set of changed values to be enumerable.
	Default value is `WEAK`.
	`WAIT` and `NOWAIT` specify when the cache is invalidated.
	With `WAIT` the invalidation is synchronous with applying the change: after the change is applied, no read will return the previous value from the cache. Use it for properties whose values the logic makes decisions on (for example, checking the existence of an object by a unique value).
	With `NOWAIT` the invalidation is deferred and performed periodically (the period is set by the `flushAsyncValuesCaches` setting, 1 second by default), so for a short time after the change reads can still return the previous value. Use it where such short staleness is harmless.
	Default value is `NOWAIT`. For `STRONG` the invalidation is always synchronous, so `NOWAIT` cannot be specified with it.

- `EXTID extId`

    Specifying the name to be used for [export/import](../paradigm/Structured_view.md#extid) of the property when it is added to the form. It is similar to specifying the `EXTID` option in the [property block](Properties_and_actions_block.md) of the [`FORM` statement](FORM_statement.md), which can override it.

    - `extId`

        String literal.

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
