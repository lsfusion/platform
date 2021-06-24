---
title: 'EXTEND CLASS statement'
---

The `EXTEND CLASS` statement [extends](Class_extension.md) an existing class.

### Syntax

    EXTEND CLASS name 
    [{
        objectName1 [objectCaption1],
        ...
        objectNameM [objectCaptionM]
    }] 
    [: parent1, ..., parentN];

### Description

The `EXTEND CLASS` statement extends an existing [custom class](User_classes.md) with additional parent classes and new [static objects](Static_objects.md). You can also extend [abstract classes](User_classes.md#abstract) by adding parent classes to them.

### Parameters

- `name`

    Class name. A [composite ID](IDs.md#cid). 

- `objectName1, ..., objectNameM`

    Names of new static objects of the specified class. Each name is defined [by a simple ID](IDs.md#id). Name values are stored in the `System.staticName` system property.

- `objectCaption1, ..., objectCaptionM`

    Captions of new static objects of the specified class. Each caption is a [string literal](IDs.md#strliteral). If the caption is not defined, the name of the static object will be its caption. Caption values are stored in the `System.staticCaption` system property.

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
