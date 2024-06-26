---
title: 'CLASS statement'
---

`CLASS` statement - creating a new [custom class](User_classes.md).

### Syntax

```
CLASS ABSTRACT name [caption] [: parent1, ..., parentN];
 
CLASS [NATIVE] name [caption] 
[{
    objectName1 [objectCaption1],
    ...
    objectNameM [objectCaptionM]
}] 
[: parent1, ..., parentN];
```

### Description

The `CLASS` statement declares a new class and adds it to the current [module](Modules.md). 

The statement has two forms: `CLASS ABSTRACT` for declaring an [abstract class](User_classes.md#abstract) and just `CLASS` for declaring a concrete class. In the latter case, during declaration, you can declare the [static objects](Static_objects.md) of this class and specify their names and captions in a curly brackets block.   

The `NATIVE` keyword is used in some system modules. It is used to declare individual system classes that are created before the modules are initialized.

### Parameters

- `name`

    Class name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](Naming.md#namespace).

- `caption`

    Class caption. [String literal](Literals.md#strliteral). If the caption is not defined, the name of the class will be its caption.  

- `objectName1, ..., objectNameM`

    The names of static objects of this class. Each name is defined by a simple ID. Name values are stored in the `System.staticName` system property.

- `objectCaption1, ..., objectCaptionM`

    Captions of static objects of this class. Each caption is a string literal. If the caption is not defined, the name of the static object will be its caption. Caption values are stored in the `System.staticCaption` system property.

- `parent1, ..., parentN`

    List of parent class names. Each name is defined by a [composite ID](IDs.md#cid). If the list of parent classes is not specified, the class is inherited from the `System.Object` class.  

### Examples

```lsf
CLASS ABSTRACT Document; // The caption of this class will be 'Document'
CLASS IncomeDocument 'Income' : Document;
CLASS OutcomeDocument 'Expenses' : Document;

CLASS Person;
CLASS Worker;
CLASS Musician : Person, Worker; // multiple inheritance

CLASS Barcode 'Barcode'; // The parent class of Barcode will be the System.Object class

CLASS PrintOrientation {
    portrait 'Portrait',
    landscape 'Landscape'
}
```

