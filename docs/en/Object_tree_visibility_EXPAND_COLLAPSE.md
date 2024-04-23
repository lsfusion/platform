---
title: 'Object tree visibility (EXPAND, COLLAPSE)'
---

[Object tree](Interactive_view.md#tree) *expansion* and *collapse* operators provide control over the visibility of elements within a tree displayed on a [form](Forms.md). As input to these operators, the [object group](Form_structure.md#objects) to which the operation is applied is passed. It is also specified over which tree elements (each corresponding to a specific object collection in the mentioned group) the operation needs to be performed:

- [current](Form_structure.md#currentObject) element
- specified set of elements
- all top-level elements of the specified object group

Possible operations include:

- expand or collapse an element
- expand or collapse an element and all its descendants
- expand all ancestors of an element

### Language

To declare actions that implement the expansion and collapse operations of object tree elements, use the [`EXPAND`](EXPAND_operator.md) and [`COLLAPSE`](COLLAPSE_operator.md) operators.

### Examples

```lsf
FORM expandCollapseTest
    TREE elements e = NavigatorElement PARENT parent(e)
    PROPERTIES(e) READONLY BACKGROUND NOT e IS NavigatorFolder VALUE, canonicalName, caption
;

expandDown {
    EXPAND DOWN expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

expandAllTop {
    EXPAND ALL TOP expandCollapseTest.e;
}

collapseDown {
    COLLAPSE DOWN expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

EXTEND FORM expandCollapseTest
    PROPERTIES() expandDown, expandAllTop, collapseDown
;
```