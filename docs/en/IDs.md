---
title: 'IDs'
---

Identifiers or *IDs* in the **lsFusion** language are used for naming or referring to [system elements](Element_identification.md), parameters of [properties](Properties.md) and [actions](Actions.md), [static objects](Static_objects.md), properties and actions on a form, and other entities.

### Simple ID {#id}

*Simple ID* is a basic building block for creating other identifiers. It consists of a sequence of uppercase and/or lowercase Latin letters `a-zA-Z`, digits `0-9`, and the underscore character `_`. The first character of a simple ID must be a letter. Names beginning with an underscore are reserved for internal system names.

Simple IDs are used mainly as names for system elements and as names for parameters of properties and actions.

#### Examples

```lsf 
name
value_id13
bankAccount
```

### Composite ID {#cid}

```
[namespace.]name
```

*Composite ID* is used to reference a system element by name, with the option  of specifying а [namespace](Naming.md#namespace). It can consist of either a single simple ID or two simple IDs separated by a dot. If you need to specify a namespace, you first specify the namespace name, then a dot, followed by the name of the system element.

#### Examples

```lsf
System.name
Sale.Document
name
```

### Class ID {#classid}

*Class ID* is used to reference either a [user](User_classes.md) or [built-in](Built-in_classes.md) class. For a user class, the identifier is represented as a composite ID, while for a built-in class, special [keywords](Built-in_classes.md) are used to specify the built-in class.

#### Examples

```lsf
System.Object
Barcode
INTEGER
STRING
```

### Static object ID {#staticobjectid}

```
[namespace.]className.objectName
```

*Static object ID* is used to reference a [static object](Static_objects.md) of a class. It consists of a user class ID and a simple ID (name of the static object) separated by a dot.

#### Examples

```lsf
Direction.north
System.FormResult.ok
```

### Property or action ID {#propertyid}

*Property ID* or *action ID* is used to reference a property (action). In the simple case it is a composite ID that specifies the name of the property (action) and possibly its namespace, similar to any other named system element. In the general case, this composite ID is supplemented by a description of the property (action) signature, which describes the classes of the property (action) parameters. The signature is specified as a list of class IDs enclosed in square brackets. If the parameter class is unknown or not important for uniquely identifying the property (action), its ID in the list can be replaced with a question mark `?`.

#### Examples

```lsf
userRole
Security.userRole
userRole[System.User]
cross[Circle, Line]
quantity[Document, ?, Store]
```

### Property or action on a form ID {#formpropertyid}

```
[namespace.]formName.formPropertyName
```

*Property on a form ID* or *action on a form ID* is used to reference a property or action added to a form. It consists of a composite ID that specifies the form and the [name of the property (action) on a form](Properties_and_actions_block.md#name), separated by a dot.

#### Examples

```lsf
barcodeSku.amount(b)
Item.items.name(i)
Consignment.dashboard.date
```

### Group object ID {#groupobjectid}

*Object group ID* is used to reference an [object group](Form_structure.md) (or object) on a form. It consists of a composite ID that specifies the form and the name of the object group (or the name of the object), separated by a dot.

#### Examples

```lsf
storeArticle.s
Item.form.object
```

### Typed parameter {#paramid}

```
[classID] name
```

*Typed parameter* is used to reference a [property (or action) parameter](Properties.md) (action). It consists of an optional parameter class ID and a simple ID representing the parameter's name.

#### Examples

```lsf
user
User user
System.User user
INTEGER count
```
