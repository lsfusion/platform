---
slug: "/EXTEND_CLASS_statement"
title: 'EXTEND CLASS statement'
---

The `EXTEND CLASS` statement [extends](../paradigm/Class_extension.md) an existing class.

### Syntax

```
EXTEND CLASS name 
[{
    objectName1 [objectCaption1] [imageSetting],
    ...
    objectNameM [objectCaptionM] [imageSetting]
}] 
[: parent1, ..., parentN];
```

Where `imageSetting` is one of:

```
IMAGE [imageLiteral]
NOIMAGE
```

### Description

The `EXTEND CLASS` statement extends an existing [custom class](../paradigm/User_classes.md) with additional parent classes and new [static objects](../paradigm/Static_objects.md). You can also extend [abstract classes](../paradigm/User_classes.md#abstract) by adding parent classes to them.

### Parameters

- `name`

    Class name. A [composite ID](IDs.md#cid). 

- `objectName1, ..., objectNameM`

    Names of new static objects of the specified class. Each name is defined [by a simple ID](IDs.md#id). The `name[StaticObject]` property returns this name qualified with the namespace and class — the object's [canonical name](../paradigm/Static_objects.md).

- `objectCaption1, ..., objectCaptionM`

    Captions of new static objects of the specified class. Each caption is a [string literal](Literals.md#strliteral). If the caption is not defined, the name of the static object will be its caption. The caption of each static object is available through the `caption[StaticObject]` property.
 
- `imageSetting`

    Icon setting for a static object. One of:

    - `IMAGE`

        [Manual icon specification](../paradigm/Icons.md#manual), optionally followed by `imageLiteral` — a [string literal](Literals.md#strliteral) whose value defines the icon. If `imageLiteral` is omitted, the [automatic assignment](../paradigm/Icons.md#auto) mode is enabled.

    - `NOIMAGE`

        The static object has no icon.

- `parent1, ..., parentN`

    A list of names of new parent classes. Each name is defined by a composite ID. 

### Examples

```lsf
CLASS ABSTRACT Shape;
CLASS Box : Shape;

CLASS Quadrilateral;
EXTEND CLASS Box : Quadrilateral; // Adding inheritance

CLASS ShapeType {
	point 'Dot',
	segment 'Line segment'
}

EXTEND CLASS ShapeType { // Adding a static object
	circle 'Circle'
}
```
