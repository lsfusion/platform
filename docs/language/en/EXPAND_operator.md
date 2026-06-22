---
slug: "/EXPAND_operator"
title: 'EXPAND operator'
---

The `EXPAND` operator creates an [action](../paradigm/Actions.md) that expands either [elements](../paradigm/Object_tree_visibility_EXPAND_COLLAPSE.md) of an [object tree](../paradigm/Interactive_view.md#tree) on a form, or a [collapsible container](../paradigm/Container_visibility_EXPAND_COLLAPSE.md) of a form.

### Syntax
```
EXPAND [expandType] formObjectGroupId [OBJECTS objName1 = expr1, ..., objNameN = exprN]
```

To expand a form container:

```
EXPAND CONTAINER formName.componentSelector
```

### Description

The first form creates an action that is used to expand specific elements of the object tree on a form. These elements can be determined using the `OBJECTS` block. If this block is not specified, the expansion operation will be applied either to the current element of the tree or to the top-level elements of the specified [object group](../paradigm/Form_structure.md#objects), depending on the type of operation.

The form with the `CONTAINER` keyword creates an action that expands a container of the form in whose context the action is executing, revealing its contents.

### Parameters

- `expandType`

    Expansion type. Specified in one of the following ways:
 
    - `DOWN`

        Keyword that, when specified, will cause the elements of the tree to expand. If the `OBJECTS` block is not specified, the operation is applied to the current element.  

    - `UP`

        Keyword that, when specified, will cause the expansion of tree elements, as well as all elements that are their ancestors. If the `OBJECTS` block is not specified, the operation is applied to the current element.

    - `ALL`

        Keyword that, when specified, will cause the recursive expansion of tree elements and all their descendants. If the `OBJECTS` block is not specified, the operation is applied to the current element.

    - `ALL TOP`

        Two keywords that, when specified, will cause the recursive expansion of all top-level elements of the specified object group. The `OBJECTS` block is ignored.

    If not specified, the default value is `DOWN`.

- `formObjectGroupId`

    [Object group ID](IDs.md#groupobjectid) that specifies the object group to which the expansion operation is applied.

- `objName1 ... objNameN`

    Names of objects on the form. The objects must belong to the specified object group. The object name is specified by a [simple ID](IDs.md#id).
 
- `expr1 ... exprN`

    [Expressions](Expression.md) whose values are the target values of the corresponding objects in the specified object group.

- `formName`

    Form name. [Composite ID](IDs.md#cid).

- `componentSelector`

    Design component [selector](DESIGN_statement.md#selector). The component must be a collapsible container.

### Examples

```lsf
FORM expandCollapseTest
    TREE elements e = NavigatorElement PARENT parent(e)
    PROPERTIES(e) READONLY BACKGROUND NOT e IS NavigatorFolder VALUE, canonicalName, caption
;

expandDown {
    EXPAND DOWN expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

expandUp {
    EXPAND UP expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

expandAllTop {
    EXPAND ALL TOP expandCollapseTest.e;
}

EXTEND FORM expandCollapseTest
    PROPERTIES() expandDown, expandUp, expandAllTop
;
```

```lsf
CLASS Store;
name = DATA ISTRING[100] (Store);

FORM dashboard
    OBJECTS s = Store
    PROPERTIES(s) name
;

DESIGN dashboard {
    NEW detailsBox {
        collapsible = TRUE;
        caption = 'Details';
        MOVE BOX(s);
    }
}

expandDetails {
    EXPAND CONTAINER dashboard.detailsBox;
}

EXTEND FORM dashboard
    PROPERTIES() expandDetails
;
```
