---
title: 'EXPAND operator'
---

The `EXPAND` operator is the creation of an [action](Actions.md), that implements the [expansion of elements](Object_tree_visibility_EXPAND_COLLAPSE.md) in the [object tree](Interactive_view.md#tree).

### Syntax
```
EXPAND [expandType] formObjectGroupId [OBJECTS objName1 = expr1, ..., objNameN = exprN]
```

### Description

The `EXPAND` operator creates an action that is used to expand specific elements of the object tree on a form. These elements can be determined using the `OBJECTS` block. If this block is not specified, the expansion operation will be applied either to the current element of the tree or to the top-level elements of the specified [object group](Form_structure.md#objects), depending on the type of operation. 

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
