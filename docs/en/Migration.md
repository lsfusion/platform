---
title: 'Migration'
---

An **lsFusion**-based information system uses a relational database management system for permanent data storage. It should be remembered that after certain changes to the system logic, the platform is unable to determine how the data should be migrated. In these cases, the developer must explicitly define the migration method by creating the special *migration file* `migration.script`, which should be in the CLASSPATH of the application server.

The migration file consists of blocks describing the changes made in the specified version of the database structure. At server startup, all changes from the migration file that have a version higher than the version stored in the database are applied. Changes are applied according to the version, from the lowest version to the highest. If the database structure is changed successfully, the maximum version of all applied blocks is written to the database as the current one. The syntax for the description of each block is as follows:

    V<version number> {
        change1
        ...
        changeN 
    }

The *version number* is a set of one or more numbers separated by a dot. When comparing the numbers of two versions, the first numbers of the versions are compared first, then if equal the second are compared, and so on. If one version contains fewer numbers than another, during comparison zeros are added to the a version with fewer numbers. For example, version number `1.3` is equivalent to number `1.3.0.0`, and version `1.2` is higher than version `1.1.3`. In the migration file, the version number is indicated with a capital letter `V`: `V1.0`, `V2.0.11`.

The migration file allows you to handle changes to [canonical names](Naming.md#canonicalname) of system elements, which occur when renaming and/or transferring to another namespace. Changes are of the following types: 

    PROPERTY oldNS.oldName[class1,...,classN] -> newNS.newName[class1,...,classN]
    STORED PROPERTY oldNS.oldName[class1,...,classN] -> newNS.newName[class1,...,classN]
    FORM PROPERTY oldNS.oldFormName.oldName(object1,...,objectN) -> newNS.newFormName.newName(object1,...,objectN)  
    CLASS oldNS.oldName -> newNS.newName
    OBJECT oldNS.oldClassName.oldName -> newNS.newClassName.newName
    TABLE oldNS.oldName -> newNS.newName
    NAVIGATOR oldNS.oldName -> newNS.newName

### Changing the name of a property or action

When renaming a [property](Properties.md)/[action](Actions.md) and/or when moving it to another [namespace](Naming.md#namespace), the canonical name of the property/action changes. Adding a `PROPERTY` change to the migration file specifying the old and new canonical names will allow you to preserve the security policy settings, as well as settings from the `Reflection.properties` table. If the property is [primary](Data_properties_DATA.md), to preserve data when changing the canonical name of this property **it is necessary** to add a `STORED PROPERTY` change. Then, when the server starts, the field corresponding to this property in the database table will be renamed. Otherwise, the old field will be renamed to the field with the name `<old ID>_deleted` (for example, when deleting a property), and a new field will be created with empty values. Apart from that the `STORED PROPERTY` type is equivalent to the `PROPERTY` type.


:::info
On the right side of `STORED PROPERTY` and `PROPERTY` changes it is not necessary to specify a signature, as here the signature is automatically taken from the left side.
:::

### Changing the name of a property/action on a form

When changing [the name of the property on a form](Properties_and_actions_block.md#name) using the migration file, you can preserve information from the table settings for this property/action on the form. For this, the `FORM PROPERTY` change type is used. The old and new names are the name of the form namespace, the name of the form, and the name of the property on the form, separated by dots. Also, using this type of change you can preserve information from the table settings when changing the canonical name of the form. To do this, add `FORM PROPERTY` changes to the migration file for all properties/actions on the form with the changed canonical name of the form.

### Changing the name of a custom class

When renaming a [custom class](User_classes.md) and/or when moving it to another namespace, the canonical name of this class changes. In this case, it is **essential** to reflect these changes in the migration file in order to preserve objects of this class and all data associated with these objects. To do this, add a `CLASS` change to the migration file, specifying the old and new class names. This will also automatically rename [static objects](Static_objects.md) of this class, if they exist. 


:::info
It is worth noting that changing the canonical name of a class can lead to changes in the canonical names of data properties. At present these changes are not automatically tracked, and they must also be added to the migration file.
:::

### Changing the name of a static object

When renaming a [static object](Static_objects.md), an `OBJECT` change is used, which allows you to preserve data associated with the object. The old and new names are the name of the class namespace, the name of the class, and the name of the object, separated by dots. 

### Changing the name of a table

When renaming a [table](Tables.md) and/or when moving it to another namespace, the canonical name of this table changes. In this case, after creating a table with a new name, the system automatically moves all the records from the table with the old name in a separate request. However, if you add a `TABLE` change to the migration file, specifying the old and new canonical table names, a query will be executed to rename the old table, which will be significantly faster.

### Changing the name of a navigator element

When renaming a [navigator element](Navigator.md) and/or when moving it to another namespace, the canonical name of this element changes. In order to preserve the security policy settings associated with this element, add a `NAVIGATOR` change to the migration file, specifying the old and new canonical names of the navigator element. 

### Changing the name of a namespace

Since the name of a namespace is used in the canonical names of system elements, changing it causes the canonical names of the system elements included in it to change. Therefore, if a namespace name is changed, information on all the above elements must be placed in the migration file. The same must be done when moving system elements to different namespaces.

### Example

**migration.script**

    V0.3.1 {
        STORED PROPERTY Item.gender[Item.Article] -> Item.dataGender[Item.Article] // change of DATA property 
        PROPERTY System.SIDProperty[Reflection.Property] -> Reflection.dbNameProperty[Reflection.Property] // parallel transferring to another namespace and changing of the property name
        FORM PROPERTY Item.itemForm.name(i) -> Item.itemForm.itemName(i)
    }
     
    V0.4 {
        FORM PROPERTY Document.documentForm.name(i) -> Document.itemForm.itemName(i)
        FORM PROPERTY Item.itemForm.itemName(i) -> Item.itemForm.iname // adding of an explicit name for a property on a formе: iname = itemName(i)
        CLASS Date.DateInterval -> Date.Interval
        OBJECT Geo.Direction.North -> Geo.Direction.north
        TABLE User.oldTable -> User.newTable
    }
