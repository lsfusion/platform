---
title: 'Action options'
---

When declaring an [action](Actions.md) in the [`ACTION` statement](ACTION_statement.md) a certain set of *action options* may be specified at the end of the declaration. 

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
STICKY | NOSTICKY
syncType
ASON eventType [ACTION] propertyId
imageSetting
annotationSetting
CONFIRM
EXTID extID
```

## Description and parameters

- `IN groupName`

    Specifying the [group of properties and actions](Groups_of_properties_and_actions.md) to which the action belongs. If the option is not specified, then the action will belong by default to the `System.private` group.

    - `groupName`
    
        A group name. [Composite ID](IDs.md#cid).

### Interactive view block

- `viewType`

    Specifying the type of the [action view](Interactive_view.md#property) when added to the form.

    - `GRID` - table column
    - `TOOLBAR` - toolbar
    - `PANEL` - panel

  It is similar to specifying the viewType option in the [property block](Properties_and_actions_block.md) of the [`FORM` statement](FORM_statement.md). Thus, if this option is not specified either in the action options or in the property block directly on the form, the [default view](Interactive_view.md#property) of the action display group on the form is used.

- `ON eventType { actionOperator }`

    Specifying an action that will be the default handler of a certain [event](Form_events.md) for all the interactive views of this action. Can be overridden in the property block of the `FORM` statement.

    - `eventType`

        A [form event](Form_events.md) type. It is specified by one of the keywords:

        - `CHANGE` - occurs when the user tries to change the value of the specified property
        - `CHANGEWYS` - occurs when the user tries to change the value of the specified property using a special input mechanism. You can read more in the description of the [form events](Form_events.md) 
        - `GROUPCHANGE` - occurs when the user tries to change the property value for all objects in the table (group editing)
        - `EDIT` - occurs when the user tries to edit the object that is the value of the specified property
        - `CONTEXTMENU [caption]` - the user has selected the specified item in the property context menu on the form. If necessary, you can also define the caption of this menu item ([string literal](Literals.md#strliteral)). If it is not specified, then, by default, it will be the same as the action caption.

    - `actionOperator`

        A [context-dependent action operator](Action_operators.md#contextdependent). An operator that defines the action executed on an event. You can use the parameters of the property itself as operator parameters.

- `ASON eventType [ACTION] propertyId`

    Specifies that this action will be the default handler of a certain [event](Form_events.md) for all the interactive views of the specified property or action. Can be overridden in the [property and action block](Properties_and_actions_block.md) of the `FORM` statement. 

    - `eventType`

        Similar to the `ON` block.

    - `propertyId`

        An [ID of the property or action](IDs.md#propertyid) for which the created action will be executed when the specified form event occurs.

    - `ACTION`

        Keyword. If specified, it is considered that the action is set in `propertyId`. If not specified, it is initially considered that a property is defined in `propertyId`; otherwise, if no property is found, it is considered that an action is specified in `propertyId`.

- `imageSetting`

    Icon settings for the action. This option allows you to configure the icon manually. It can have one of the following forms:

    - `IMAGE [imageLiteral]`

        [Manual icon specification](Icons.md#manual) for the action. If `imageLiteral` is not provided, the [automatic assignment](Icons.md#auto) mode is enabled.

        - `imageLiteral`

            [String literal](Literals.md#strliteral) whose value defines the icon.

    - `NOIMAGE`

        Keyword indicating that the action should have no icon.

- `annotationSetting`

Action annotation. Begins with `@@`. The following annotations are supported:

    - `@@noauth`

        Disables authorization check for external requests for this action.

    - `@@api`

        When the API is disabled, allows external requests for this action.

    - `@@deprecated`
    - `@@deprecated(since)`
    - `@@deprecated(since, message)`

        Marks the action as deprecated and not recommended for use.
        The plugin displays such properties as strikethrough.

      - `since`
          String literal indicating the platform version since which the action is considered deprecated.

      - `message`
          String literal providing an explanation of why the action is marked as deprecated.

- `EXTID extID`

    Specifying the name to be used for [access from an external system](Access_from_an_external_system.md#http).

### `DESIGN` statement default values block

- `CHANGEKEY key [SHOW | HIDE]`

    Specifies a [keyboard shortcut](Form_events.md#keyboard) which triggers this action. Sets the value for the [default design](Form_design.md#defaultDesign) and can be overridden in the [`DESIGN` statement](DESIGN_statement.md).

    - `key`

  [String literal](Literals.md#strliteral), that defines a keyboard shortcut. Syntax:
  ```
  keyStroke [;(modeKey=modeValue;)*]
  ```

          - `keyStroke`
              String representation of a key combination. The definition principle is similar to the way the parameter is specified in the Java class method KeyStroke.getKeyStroke(String). (http://docs.oracle.com/javase/7/docs/api/javax/swing/KeyStroke.html#getKeyStroke(java.lang.String)).
    
          - `(modeKey=modeValue;)*`
              Options specifying the execution conditions for keyStroke. The following options are supported:
    
              - `priority = priorityValue`
                  Priority, an integer value. If multiple actions meet the CHANGEKEY conditions, the one with the higher priority will be executed.
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
                  Checks whether the action is currently visible on the form. Supported `showingValue` values:
                  - `AUTO`, `ONLY` -> isShowing
                  - `ALL` -> true
                  - `NO` -> !isShowing
    
              - `panel = panelValue`
                  Checks whether the action is located in a panel. Supported `panelValue` values:
                  - `AUTO` -> !isMouse || !isPanel
                  - `ALL` -> true
                  - `ONLY` -> isPanel
                  - `NO` -> !isPanel
    
              - `cell = cellValue`
                  Checks whether the action is located in a table cell. Supported `cellValue` values:
                   - `AUTO` -> !isMouse || isCell
                   - `ALL` -> true
                   - `ONLY` -> isCell
                   - `NO` -> !isCell
    
    
              For all options except `priority`, the default value is `AUTO`.

    - `SHOW`

      Keyword. When specified, the key combination will be displayed in the action caption. Used by default.

    - `HIDE`

      Keyword. When specified, the key combination will not be displayed in the action caption.

- `CHANGEMOUSE key [SHOW | HIDE]`

  Specifies the mouse key combination that triggers this action. Sets the default value for the design, which can be overridden in the `DESIGN` instruction.

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

        Keyword indicating that the mouse key combination should be displayed in the action header. This is the default behavior.

    - `HIDE`

        Keyword indicating that the mouse key combination should not be displayed in the action header.

- `STICKY` | `NOSTICKY`

    Keywords. `STICKY` indicates that the action in the table will be pinned to the left and remain visible when scrolling to the right. `NOSTICKY` removes this pinning. By default, `STICKY` or `NOSTICKY` is determined heuristically.

- `syncType`

    Defines whether the action is executed synchronously or asynchronously:

    - `WAIT` — synchronously.

    - `NOWAIT` — asynchronously. This is the default behaviour.

- `CONFIRM`

    Keyword. If specified, the user will be asked to confirm the action when it is executed. Sets the value for the default design and can be overridden in the `DESIGN` statement.
