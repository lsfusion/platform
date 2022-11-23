---
title: 'Naming'
---

Each [system element](Element_identification.md) may have a *name* which can be used to [access](Search_.md) the element.

### Namespaces {#namespace}

It is often necessary to use the same name in different contexts. In order not to include this context in the name itself (producing long and bulky names), the platform has the concept of *namespaces*. Each element is created in a namespace, and if other elements are accessed during the creation process then elements created in the same namespace take precedence.  However, if you do need an element from another namespace, you can always specify the namespace of the element you are looking for explicitly. Also, you can specify additional namespaces that will take precedence when searching for items.


:::info
You can [find](Search_.md) more details on how namespaces are used when [finding](Search_.md) elements in the relevant section.
:::

The namespace in which elements are created is determined by the [module](Modules.md), and cannot be changed in the future. The same limitation applies to additional priority namespaces.

Each namespace has its own name, which is its unique ID. Accordingly, the string obtained by concatenating (via a dot) the name of its namespace with the name of each element itself will be called the element's *full name*. For example, if the namespace is called `System`, and inside it there is a class `Element`, then the full name of this class will be `System.Element`.

### Uniqueness

Elements of the system must be named so that the system does not contain any two elements that cannot be distinguished from one another. In most cases, it is necessary and sufficient for the full name of the element to be unique. Exceptions to this rule are metacodes and properties / actions. So, for example, several metacodes may have the same full name if they differ in the number of parameters they take (properties / actions must have a different signature).

### Canonical names {#canonicalname}

For some elements of the system, string *canonical names* are determined and are unique among all elements of the given type within the system. For most system elements (user-defined classes, property groups, navigator elements, windows, tables) the canonical name is equivalent to the *full name* of that element of the system, which looks as follows:

    <namespace name>.<System element name>

    Item.name
    Sale.Document

Since properties and actions can have the same names within the same namespace, the full name of a property may not be unique. Therefore, the canonical name of the properties / actions also includes a signature, that is, a list of the canonical names of the classes of the property / action's parameters, separated by commas. If a parameter's class is not determined, then the question mark character `?` is used instead of the canonical class name:

    <namespace name>.<Property/action name>[<class1>,...,<classN>]

    Item.gender[Item.Article]
    Date.between[DATE,DATE,DATE]
    Document.addHeader[Document.Document,STRING]
    Math.sum[?,?]

Since the signature of properties/actions do not have to contain only custom classes, canonical names are also determined for [built-in](Built-in_classes.md) classes: 

| Class name              | Canonical name    |
| ----------------------- | ----------------- |
| `INTEGER`               | `INTEGER`         |
| `LONG`                  | `LONG`            |
| `DOUBLE`                | `DOUBLE`          |
| `NUMERIC[ , ]`          | `NUMERIC`         |
| `BOOLEAN`               | `BOOLEAN`         |
| `DATE`                  | `DATE`            |
| `DATETIME`              | `DATETIME`        |
| `TIME`                  | `TIME`            |
| `YEAR`                  | `YEAR`            |
| `STRING`, `STRING[ ]`   | `STRING`          |
| `ISTRING`, `ISTRING[ ]` | `STRING`          |
| `BPSTRING[ ]`           | `STRING`          |
| `BPISTRING[ ]`          | `STRING`          |
| `TEXT`                  | `STRING`          |
| `RICHTEXT`              | `STRING`          |
| `COLOR`                 | `COLOR`           |
| `FILE,RAWFILE...`       | `FILE,RAWFILE...` |


:::info
In some cases, an element of the property signature may be not a single class but a set of classes. In this case, the canonical name will be more complex.
:::

### Name policy

To avoid name collision, as well as for better readability, it is recommended that you use the following name policy:

#### System elements

-   The name must begin with a lowercase letter (excluding classes).

-   If the name consists of several words, then each subsequent word should begin with a capital letter. For example, `myFirstName`.

#### Classes

-   The name of each class should begin with a capital letter. For example, `MySuperClass`.

### Language

Elements are named using [simple IDs](IDs.md#id).
