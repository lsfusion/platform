---
slug: "/Structured_view"
title: 'Structured view'
---

All structured views (*formats*) can be divided into two types:

-   *Hierarchical* (XML, JSON) - single text file; the information for [object groups](Form_structure.md#objects) is put as a list inside the information for [parent](Static_view.md#hierarchy) groups.
-   *Flat* (DBF, CSV, XLS, XLSX, TABLE) - one table file for each object group, and each object group with more than one level of nesting should have a column in its table named `parent` which should contain the "upper" row number in the parent group table.


:::info
Working with flat formats with a hierarchy depth greater than one is not very convenient (because in this case, the developer has to maintain an additional column), therefore, usually flat formats are used only for working with simple forms (with a hierarchy depth less than one). In other cases, hierarchical formats are usually used.
:::

Formats are also divided into:

-   human-readable (text) and binary. All hierarchical formats are human-readable, flat can be either binary (DBF, TABLE, XLS, XLSX), or human-readable (CSV). You can and should specify the encoding for human-readable formats (UTF-8 is used by default).
-   standardized and internal. At the moment, only one internal format is supported: TABLE (a table of values). All the other formats are standardized. Internal format files are processed in a special way in some [integration](Integration.md) operations (e.g., in [SQL calls](Access_to_an_external_system_EXTERNAL.md#table)). In addition, internal formats can be used to communicate lsFusion systems with each other.

In the current platform implementation, [group-in-columns](Form_structure.md#groupcolumns) are ignored in a structured view.

<a className="lsdoc-anchor" id="objects"/>

When building an object group hierarchy in a structured view, the object groups that have all their objects [passed](Open_form.md#params) on the form opening are ignored (as if these object groups did not exist).

<a className="lsdoc-anchor" id="drawgroup"/>

If the property [display group](Form_structure.md#drawgroup) is specified explicitly in a structured view, then this group should not be earlier than the default one (if the specified group appears earlier, then the default display group will be used anyway).

### Export/import name {#extid}

The name of the property on the form that will be used during export/import can be specified explicitly using the corresponding option (`EXTID`). Unless this is done, the name of the property on the form will be used as the name of the export/import. If it is also not specified, then the name of the property itself will be the name of the export/import (without adding parameter objects to its end, as it is done in the other mechanisms - [reports](Print_view.md), customizing interactive view [design](Form_design.md), etc.). The export/import names of object groups and property groups are determined similarly.


:::info
Because of the grammar nature, the option for specifying import/export name of the form (specifically, its [empty group](Static_view.md#empty) of objects) is called `FORMEXTID` (not `EXTID`).
:::


:::info
Unlike property names on the form, property export/import names (`EXTID`) of different properties can be equal if these properties are located in different nodes of the hierarchy (i.e., different groups of objects/properties). The same applies to export/import names of object groups and property groups.
:::

`EXTID` can have specialized postfixes:
-   ':escapeInnerJSON'
    This postfix disables the [embedding of JSON values](#innerjson): the property value is exported as a string even if it is itself JSON.
    Example:
    ```lsf
    regexpPattern 'Mask' () = '[0-9]';
    FORM exportPattern
    PROPERTIES() c1 = regexpPattern EXTID 'c1', c2 = regexpPattern EXTID 'c2:escapeInnerJSON';
    ```
    ```json
    {"c1":["0-9"],"c2":"[0-9]"}
    ```

### Hierarchical view {#hierarchy}

Before directly proceeding with the form export/import, the platform builds a hierarchy of properties, groups of objects/properties as follows:

-   The hierarchy of objects/properties groups is built in accordance with the [hierarchy](Static_view.md) of object groups and property display groups: a property display group is considered the parent of this property, the hierarchy of object groups is preserved.
-   Then for each `X` object group:
    -   [property groups](Groups_of_properties_and_actions.md) that all `X` descendants belong to are determined, then these property groups and their ancestors are automatically included in the hierarchy. Also:
        -   property groups become the parents of `X` descendants that belong to those groups
        -   the hierarchy of property groups is preserved
        -   the `X` object group becomes the parent of the uppermost (i.e., those without parents) of the used property groups.


:::info
In a hierarchical view, object groups, like properties, can be included in property groups.
:::


:::info
The described algorithm is very similar to the algorithm for building property containers in the [default design](Form_design.md#defaultDesign) (with the only difference being that it does not use the hierarchy of object groups). As in the container building mechanism, the same property group can be included in the hierarchy several times for different groups of objects.
:::

After the hierarchy is built, the form is exported/imported recursively according to the following rules: 

*JSON*:
```json
JSON result ::= 
    { JSON with properties, groups of objects/properties without parents }

JSON with properties, groups of objects/properties ::= 
    JSON of the property 1 | JSON of the property group 1 | JSON of the object group 1
    JSON of the property 2 | JSON of the property group 2 | JSON of the object group 2
    ...
    JSON of the property M | JSON of the property group M | JSON of the object group M

JSON of the property ::=
    "property name on the form" : property value

JSON of the property group ::=
    "property group name" : { JSON with child properties, groups of properties/objects }

JSON of the object group ::=
    "object group name" : [
        { JSON with child properties, groups of properties/objects 1 }, 
        { JSON with child properties, groups of properties/objects 2 },
        ... 
        { JSON with child properties, groups of properties/objects N },
    ]
```
*XML*:
```xml
XML result ::= 
    <form name> XML with properties, groups of properties/objects without parents </form name>

XML with properties, groups of properties/objects ::= 
    XML of the property 1 | XML of the property group 1 | XML of the object group 1
    XML of the property 2 | XML of the property group 2 | XML of the object group 2
    ...
    XML of the property M | XML of the property group M | XML of the object group M

XML of the property ::= 
    <property name on the form> property value </property name on the form>

XML of the property group ::=
    <property group name> XML with child properties, groups of properties/objects </property group name>

XML of the object group ::=
    <object group name> XML with child properties, groups of properties/objects 1 </object group name>
    <object group name> XML with child properties, groups of properties/objects 2 </object group name>
    ...
    <object group name> XML with child properties, groups of properties/objects N </object group name>
```

<a className="lsdoc-anchor" id="innerjson"/>

When exporting to JSON, a string property value that is itself JSON (starts and ends with a square or curly bracket), for example a value of the `JSON` or `JSONTEXT` class, is embedded into the result as a nested object or array rather than as a string. If such a value cannot be parsed as JSON, it is exported as a plain string. The parsing is lenient: unquoted tokens are accepted, and text after the first parsed value is discarded, so a string value that merely resembles JSON (for example, `'[AA] [BB]'`) is embedded in altered form (`["AA"]`). The embedding can be disabled for an individual property with the ':escapeInnerJSON' postfix in the [export name](#extid). For string values that may begin with a bracket it should be disabled explicitly. When importing from JSON, conversely, if a nested object or array corresponds to a property in the imported document, its text representation is written to the property.

When exporting/importing to XML, the special `ATTR` option can be specified for a property on the form. Thus, when exporting/importing that property, its value will be stored not in a separate tag, but in the attribute of the parent tag:

```
<parent tag ... property name on the form = "property value" ...>
```

When importing from XML, the name of the uppermost tag (in the rule) is ignored (according to the XML specification, there should be only one such tag).

On import, the file is matched against this hierarchy by names: the value of each property, property group, or object group is read from the element under its [export/import name](#extid). Elements of the file that do not correspond to any name in the hierarchy are ignored. The reverse situation raises no error either: an object group or property group with no matching element imports nothing (its entire subtree is skipped), while a property whose own element is missing from an imported record — with its parent element present — is imported as `NULL`. In particular, when importing from JSON, the records of an object group are read only from an array under a key equal to that group's export/import name. A root-level array is converted to `{ "value" : [ ... ] }` (see [Predefined value](#value)) and is therefore read only by an object group with the export/import name `value`.

An empty string in the file is imported into a string property as follows: JSON keeps it as an empty non-`NULL` string — thus a value distinct from an absent field and from an explicit `null`, both of which are imported as `NULL` — while CSV and XML import it as `NULL`.

Properties with `NULL` values, as well as property groups that do not have any tags inside as a result of export, are not exported (ignored).

### Object group key {#extkey}

By default, the object collections of an object group are determined in the file by their position: in the hierarchical view, by the order of the records, in the [flat](#flat) one, by the order of the rows in the table. The values of the objects themselves are not written to the file, and on import the platform determines them itself: for an object of a [concrete custom class](User_classes.md), a new object is created, for an object of a numerical class, the sequence number of the object collection is used, starting from zero, and for a nested object group the numbering is continuous - it does not restart for each record of the parent group. Accordingly, an object group imported this way must consist of exactly one object of a concrete custom or numerical class.

If the corresponding option (`EXTKEY`) is specified for an object group, its object collections are determined in the file not by position, but by the values of their objects: on export these values are written to the file, on import they are read from it. In the hierarchical view, such an object group is represented not as a list, but as key-value pairs, where the key is the value of the object:

*JSON*:
```json
JSON of the object group ::=
    "object group name" : {
        "object value 1" : { JSON with child properties, groups of properties/objects 1 },
        "object value 2" : { JSON with child properties, groups of properties/objects 2 },
        ...
        "object value N" : { JSON with child properties, groups of properties/objects N },
    }
```

*XML*:
```xml
XML of the object group ::=
    <object group name>
        <object value 1> XML with child properties, groups of properties/objects 1 </object value 1>
        <object value 2> XML with child properties, groups of properties/objects 2 </object value 2>
        ...
        <object value N> XML with child properties, groups of properties/objects N </object value N>
    </object group name>
```

In the hierarchical view, an object group with a key must consist of exactly one object of a [built-in class](Built-in_classes.md), whose value is written as the key (in XML, as a tag name, so it must be a valid tag name). In the [flat view](#flat) there are no restrictions on the composition of the object group, and the values of the objects are written to separate columns of the table.

On import, object collections with the same key are considered one object collection, and the property values for it are taken from the last such record in the file.

For example, for the form:

```lsf
price = DATA NUMERIC[14,2] (STRING[13]);

FORM importPrices
    OBJECTS barcode = STRING[13] EXTKEY
    PROPERTIES(barcode) price
;
```

the imported file will look as follows:

```json
{
    "barcode" : {
        "4600000000017" : { "price" : 12.5 },
        "4600000000024" : { "price" : 9.9 }
    }
}
```

That is, the price values will be written for the barcodes read from the keys of the file.

### Predefined value {#value}

When importing JSON, if for an object group an array ( `[ ]` ) of values contains not an object ( `{ }` ), but a specific value (for example, a number or a string), then this value is automatically converted to an object `{ "value" : value }`. A similar conversion is performed when exporting an object group to JSON: if the object contains exactly one `value` key (i.e., it has the form `{ "value" : value}`), then instead of it, the value for this `value` key is substituted to the resulting JSON. In addition to "ordinary" object groups, the same conversions are also performed for the empty root object group, i.e., for example, JSON `["ab","vv"]` is processed as JSON `{ "value" : ["ab","vv"] }`.

When importing/exporting XML, if the property is named `value`, then the value of this property will be stored not in a separate tag, but inside (in the text) the parent tag (i.e., as if the parent tag itself was a property view). This behavior is usually used if the parent tag has other tags/attributes in it (XML specification allows this).

When importing XML, if the object group is named `value`, then all tags are read (with any name).

### XML namespaces

Unlike other formats, XML supports the concept of namespaces for tags and attributes.

For example, in **lsFusion** to export a property to a tag with a specified namespace, you must specify the name of this property using a special syntax:

```
[namespace[=uri]:]name
```

For example, `h:table` or `h=http://www.w3.org/TR/html4:table`. The namespace name may be empty if necessary. If no URI is specified for the namespace, it is inherited from the namespace with the same name of the parent tags. If there are no namespaces with that name in the parent tags, URI is automatically considered equal to `http://www.w3.org/<name of the namespace>`.


:::info
It is not possible to specify the property name described above (for example, `h:table`) in the lsFusion syntax (since the name cannot contain a colon), therefore, to specify such an export name, you should use the [described above](#extid) `EXTID` option.
:::

If a namespace must be declared in a tag, but the tag itself should not belong to it, you must add a property marked `ATTR` and named `xmlns:namespace` to the export. It is assumed that the value of this property will contain the URI of the declared namespace.

Working with namespaces is similar when importing properties, as well as when working with object groups/property groups.

### Flat view {#flat}

Each file for an object group in flat view is a table in which:

-   Rows are object collections of this object group.
-   Columns are properties whose display groups are equal to this object group.

If a [key](#extkey) is specified for the object group, columns with the values of the objects of this object group are added to these columns - one for each object, before the property columns (that is, after the `parent` column, if there is one). They are named, like the other elements of the file, by the [export/import name](#extid) of the object.

In CSV format (when there is no first header line), the columns are named similarly to XLS (i.e., `A` is the first, `B` is the second, etc.).

If a column with the form property name is not found when importing the form, then the column next to the column of the previous property in the list of form properties is selected for import (in this case, the `parent` column is considered the first).

### Language

All of the above options, as well as defining the form structure, can be done using the [`FORM` statement](../language/FORM_statement.md).

### Open form

To display the form in structured view, the corresponding [open form](Open_form.md) in a [structured view](In_a_structured_view_EXPORT_IMPORT.md) operator is used.

### Examples

```lsf
FORM exportSku
    OBJECTS st = Store

    OBJECTS s = Sku
    PROPERTIES(s) id, name, weight
    FILTERS in(st, s)
;

exportSku (Store store) {
    // uploading to DBF all Sku for which in (Store, Sku) is specified for the desired warehouse
    // flat format: the file is specified for the object group s
    EXPORT exportSku OBJECTS st = store DBF CHARSET 'CP866' TO s = exportFile;
    EXPORT exportSku XML;
    EXPORT exportSku OBJECTS st = store CSV ',' TO s = exportFile;
}
```

```lsf

date = DATA DATE (INTEGER);
sku = DATA BPSTRING[50] (INTEGER);
price = DATA NUMERIC[14,2] (INTEGER);
order = DATA INTEGER (INTEGER);
FORM import
    OBJECTS o = INTEGER // orders
    OBJECTS od = INTEGER // order lines
    PROPERTIES (o) dateOrder = date // importing the date from the dateOrder field
    PROPERTIES (od) sku = sku, price = price // importing product quantity from sku and price fields
    FILTERS order(od) = o // writing the top order to order

;

importForm() {
    INPUT f = FILE DO {
        IMPORT import JSON FROM f;
        SHOW import; // showing what was imported

        // creating objects in the database
        FOR DATE date = date(INTEGER io) NEW o = Order DO {
            date(o) <- date;
            FOR order(INTEGER iod) = io NEW od = OrderDetail DO {
                price(od) <- price(iod);
                sku(od) <- GROUP MAX Sku sku IF name(sku) = sku(iod); // finding sku with this name
            }
        }
    }
}
```

  
