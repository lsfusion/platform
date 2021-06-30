---
title: 'Data import (IMPORT)'
---

The *data import* operator creates an [action](Actions.md) which reads a file from the value of some [property](Properties.md), then, depending on its [format](Structured_view.md), defines the columns (fields) of data in this file, after which it [writes](Property_change_CHANGE.md) the value of each column (field) to the corresponding property (parameter) - import *assignment*. The mapping of columns to properties can go in either column or name order.

Rows, in turn, are mapped during import to objects of specified classes (let's call these objects *imported*). In the current platform implementation, there can be at most one object and the specified class must be either [numeric](Built-in_classes.md) or a [specific user-defined class](User_classes.md#abstract). Rows are mapped to the imported object as follows:

-   for numeric classes: all imported rows are numbered in the order in which they appear in the file (starting from 0).
-   for specific user-defined classes: [a new object](New_object_NEW.md) of the specified class is created for each row.

You can also define an import *condition*: this is a property in which the [default value](Built-in_classes.md) of the property value class is written for each row (as opposed to import destination in which column values are written).

### General case

It should be noted that data import is a special case of (syntactic sugar for) [form import](In_a_structured_view_EXPORT_IMPORT.md#importForm), in which the imported form is created automatically and consists of:

-   one [group of objects](Form_structure.md#objects) named `value` whose objects correspond to imported objects (not created if there are no imported objects)
-   imported properties. The [property group](Form_structure.md#propertygroup) for the properties that are created on the form is the [builtin](Groups_of_properties_and_actions.md#builtin) group `System.private`.
-   a filter equal to the defined condition.

Accordingly, the behavior of the data import operator (for example, determining the names of the resulting columns / keys, [processing of `value`](Structured_view.md#value), etc.) is completely determined by the behavior of the form import operator (as if the above form were passed to it as a parameter).

### Language

To declare an action that imports data, use the [`IMPORT` operator](IMPORT_operator.md).

### Examples


```lsf
import()  {

    LOCAL xlsFile = EXCELFILE ();

    LOCAL field1 = BPSTRING[50] (INTEGER);
    LOCAL field2 = BPSTRING[50] (INTEGER);
    LOCAL field3 = BPSTRING[50] (INTEGER);
    LOCAL field4 = BPSTRING[50] (INTEGER);

    LOCAL headField1 = BPSTRING[50] ();
    LOCAL headField2 = BPSTRING[50] ();

    INPUT f = EXCELFILE DO {
        IMPORT XLS SHEET 2 FROM f TO field1 = C, field2, field3 = F, field4 = A;
        IMPORT XLS SHEET ALL FROM f TO field1 = C, field2, field3 = F, field4 = A;

        FOR imported(INTEGER i) DO { // imported property - a system property for iterating data
            MESSAGE 'field1 value = ' + field1(i);
            MESSAGE 'field2 value = ' + field2(i);
            MESSAGE 'field3 value = ' + field3(i);
            MESSAGE 'field4 value = ' + field4(i);
       }
    }

    LOCAL t = FILE ();
    EXTERNAL SQL 'jdbc:postgresql://localhost/test?user=postgres&password=12345' EXEC 'SELECT x.a,x.b,x.c,x.d FROM orders x WHERE x.id = $1;' PARAMS '4553' TO t;
    IMPORT FROM t() FIELDS INTEGER a, DATE b, BPSTRING[50] c, BPSTRING[50] d DO        // import with FIELDS option
        NEW o = Order {
            number(o) <- a;
            date(o) <- b;
            customer(o) <- c;
            currency(o) <- GROUP MAX Currency currency IF name(currency) = d; // finding currency with this name
        }


    INPUT f = FILE DO
        IMPORT CSV '*' HEADER CHARSET 'utf-8' FROM f TO field1 = C, field2, field3 = F, field4 = A;
    INPUT f = FILE DO
        IMPORT XML ATTR FROM f TO field1, field2;
    INPUT f = FILE DO
        IMPORT XML ROOT 'element' ATTR FROM f TO field1, field2;
    INPUT f = FILE DO
        IMPORT XML ATTR FROM f TO() headField1, headField2;

    INPUT f = FILE DO
        INPUT memo = FILE DO
            IMPORT DBF MEMO memo FROM f TO field1 = 'DBFField1', field2 = 'DBFField2';
}
```
