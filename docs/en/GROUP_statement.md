---
title: 'GROUP statement'
---

The `GROUP` statement creates a new [property group](Groups_of_properties_and_actions.md).

### Syntax

```
GROUP [NATIVE] name [caption] [EXTID extID] [: parentName];
```

### Description

The `GROUP` statement declares a new property group and adds it to the current [module](Modules.md).  

The `NATIVE` keyword is used in some system modules. It is used to declare individual property groups that are created before the modules are initialized.

### Parameters

- `name`

    Group name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](Naming.md#namespace).

- `caption`

    Group caption. [String literal](Literals.md#strliteral). If the caption is not defined, the name of the group will be its caption.  

- `EXTID extID`

    Specifying the name to be used to [export/import](Structured_view.md#extid) this property group. Used only in the [structured](Structured_view.md) view.

    - `extId`

        String literal.

- `parentName`

    Name of the parent group. [Сomposite ID](IDs.md#cid). If the name of the parent group is not defined, the `System.private` group becomes the parent.  

### Examples

```lsf
GROUP base : root; // The caption of this group will be 'base'
GROUP local 'Local properties'; // The parent group of local will be System.private
```

