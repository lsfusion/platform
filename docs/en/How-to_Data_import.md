---
title: 'How-to: Data import'
---

## Example 1

### Task

We have the books for which names and prices are defined. The order logic is also defined.

```lsf
REQUIRE Utils;

CLASS Book 'Book';
name 'Name' = DATA ISTRING[100] (Book) IN id;

id 'Code' = DATA STRING[20] (Book) IN id;
book 'Book' (STRING[20] id) = GROUP AGGR Book b BY id(b);

CLASS Order 'Order';
date 'Date' = DATA DATE (Order);
number 'Number' = DATA STRING[10] (Order);

CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE;

book 'Book' = DATA Book (OrderDetail) NONULL;
nameBook 'Book' (OrderDetail d) = name(book(d));

quantity 'Quantity' = DATA INTEGER (OrderDetail);
price 'Price' = DATA NUMERIC[14,2] (OrderDetail);

FORM order 'Order'
    OBJECTS o = Order PANEL
    PROPERTIES(o) date, number

    OBJECTS d = OrderDetail
    PROPERTIES(d) nameBook, quantity, price, NEW, DELETE
    FILTERS order(d) == o

    EDIT Order OBJECT o
;

FORM orders 'Orders'
    OBJECTS o = Order
    PROPERTIES(o) READONLY date, number
    PROPERTIES(o) NEWSESSION NEW, EDIT, DELETE
;

NAVIGATOR {
    NEW orders;
}
```

We need to create a button that loads the contents of the order from the Excel file selected by the user on their computer.

### Solution

```lsf
importXlsx 'Import from XLS' (Order o)  {
    INPUT f = EXCELFILE DO {

        LOCAL bookId = STRING[20] (INTEGER);
        LOCAL quantity = INTEGER (INTEGER);
        LOCAL price = NUMERIC[14,2] (INTEGER);

        IMPORT XLS FROM f TO bookId = A, quantity = B, price = C;

        FOR imported(INTEGER i) NEW d = OrderDetail DO {
            order(d) <- o;

            book(d) <- book(bookId(i));
            quantity(d) <- quantity(i);
            price(d) <- price(i);
        }
    }
}

EXTEND FORM order
    PROPERTIES(o) importXlsx
;
```

The [`INPUT` operator](INPUT_operator.md) which requests a file will display a dialog where the user will be able to choose an .xls or .xlsx file. Once the file is selected successfully, the system will call the [action](Actions.md) specified after `DO`.

It is assumed that a file consists of three columns. The first one `A` contains book codes, the second one `B` contains quantities, and the third one `C` contains prices. 

The  [`IMPORT` operator](IMPORT_operator.md) reads the selected file and then writes its contents to local properties which take only one argument — line number. The numbering starts from 0. The `imported` property will be set to `TRUE` if the file contains a line with the corresponding number. Then, a corresponding line is created in the order for each of these lines.

## Example 2

### Task

Similar to [**Example 1**](#example-1). In addition, we have specified a directory to which an external system puts orders. For each order, a separate CSV file is generated for storing the order date and number (in the denormalized form) along with the book code, quantity, and price.

```lsf
serverDirectory 'Directory on the server from which orders should be imported' = DATA STRING[100] ();

EXTEND FORM orders PROPERTIES() serverDirectory;
```

We need to implement an action that will import orders from this folder into the system.

### Solution

```lsf
importOrders 'Import orders from directory' ()  {

    listFiles('file://' + serverDirectory());

    FOR ISTRING[255] f = fileName(INTEGER j) AND NOT fileIsDirectory(j) DO NEWSESSION {

        LOCAL file = FILE ();
        READ 'file://' + serverDirectory() + '/' + f TO file;

        LOCAL date = DATE (INTEGER);
        LOCAL number = STRING[10] (INTEGER);

        LOCAL bookId = STRING[20] (INTEGER);
        LOCAL quantity = INTEGER (INTEGER);
        LOCAL price = NUMERIC[14,2] (INTEGER);

        IMPORT CSV '|' NOHEADER CHARSET 'CP1251' FROM file() TO date, number, bookId, quantity, price;

        NEW o = Order {
            date(o) <- date(0);
            number(o) <- number(0);

            FOR imported(INTEGER i) NEW d = OrderDetail DO {
                order(d) <- o;

                book(d) <- book(bookId(i));
                quantity(d) <- quantity(i);
                price(d) <- price(i);
            }
        }

        APPLY;
        move('file://' + serverDirectory() + '/' + f, 'file://' + serverDirectory() + '/' + (IF canceled() THEN 'error/' ELSE 'success/') + f);
    }
}

EXTEND FORM orders PROPERTIES() importOrders;
```

The `listFiles` action is declared in the `Utils` system [module](Modules.md). The action scans the folder specified in the argument and reads all the files from it and writes their contents to the `fileName` and `fileIsDirectory` properties.

The [`READ` operator](READ_operator.md) reads the specified file and writes its contents to a local property of the `FILE` type which is then processed by the `IMPORT` operator. Its arguments specify that the file format is CSV without a title in the first line, with a vertical bar as separator, and with the CP1251 encoding.

It is assumed that dates and numbers in each line will have the same values. This is why their values are read from the first line with number 0.

Each file is processed in a separate new [change session](Change_sessions.md) and then is saved using the [`APPLY` operator](APPLY_operator.md). This operator writes `TRUE` to the `canceled` property when a certain [constraint](Constraints.md) has been violated. Then, the `MOVE` statement of the `READ` operator moves the file either to `success` folder or `error` folder. This allows us to call the action again without processing the same orders several times.

Since the result action has no arguments, we can add it to the scheduler for automatic launch at certain intervals.

## Example 3

### Task

Similar to [**Example 1**](#example-1).

An external database stores a book directory with their codes and names.

We need to create an action that will synchronize the book directory with this external database.

### Solution

```lsf
importBooks 'Import books' ()  {
    LOCAL file = FILE ();
    READ 'jdbc:sqlserver://localhost;databaseName=books;User=import;Password=password@SELECT id, name FROM books' TO file;

    LOCAL id = STRING[20] (INTEGER);
    LOCAL name = ISTRING[100] (INTEGER);
    IMPORT TABLE FROM file() TO id, name;

    //creating new books
    FOR id(INTEGER i) AND NOT book(id(i)) NEW b = Book DO {
        id(b) <- id(i);
    }

    // changing values
    FOR id(Book b) == id(INTEGER i) DO {
        name(b) <- name(i);
    }

    // deleting books
    DELETE Book b WHERE b IS Book AND NOT [ GROUP SUM 1 BY id(INTEGER i)](id(b));
}
```

Synchronization consists of the three main actions. First, we create books whose codes can be found in the external database, but not in our database. Then, we update the values for all books that can be found in our database. And finally, books that cannot be found in the external database are removed from our database.

This guarantees that when the action is started, the book directory will be absolutely identical to that in the external system. This scheme is useful when some master data is maintained in another system. The result action can be added to the scheduler and triggered at certain relatively small time intervals, thereby ensuring near real-time updates for the directory.

## Example 4

### Task

Similar to [**Example 1**](#example-1).

For each order line, we have added the decoding of this line by color and size.

```lsf
CLASS OrderDetailInfo 'Order line (transcript)';

detail = DATA OrderDetail (OrderDetailInfo) NONULL DELETE;
size = DATA STRING[100] (OrderDetailInfo);
color = DATA STRING[100] (OrderDetailInfo);
quantity = DATA INTEGER (OrderDetailInfo);

EXTEND FORM order
    OBJECTS i = OrderDetailInfo
    PROPERTIES(i) size, color, quantity, NEW, DELETE
    FILTERS detail(i) = d
;
```

We need to implement the import of orders from the JSON file of the specified structure. A JSON file may look like this:
```json
{
   "version":"v1",
   "order":[
      {
         "date":"03.01.2018",
         "number":"430",
         "detail":[
            {
               "item":{
                  "id":"132",
                  "info":[
                     {
                        "size":"40",
                        "color":"black",
                        "quantity":2
                     },
                     {
                        "size":"41",
                        "color":"white",
                        "quantity":3
                     }
                  ]
               },
               "price":1.99
            },
            {
               "item":{
                  "id":"136",
                  "info":[
                     {
                        "size":"39",
                        "color":"white",
                        "quantity":4
                     },
                     {
                        "size":"43",
                        "color":"red",
                        "quantity":1
                     }
                  ]
               },
               "price":2.99
            }
         ]
      },
      {
         "date":"04.01.2018",
         "number":"435",
         "detail":[
            {
               "item":{
                  "id":"122",
                  "info":[
                     {
                        "size":"L",
                        "color":"black",
                        "quantity":1
                     },
                     {
                        "size":"XL",
                        "color":"white",
                        "quantity":1
                     }
                  ]
               },
               "price":11.99
            },
            {
               "item":{
                  "id":"126",
                  "info":[
                     {
                        "size":"S",
                        "color":"white",
                        "quantity":1
                     },
                     {
                        "size":"M",
                        "color":"red",
                        "quantity":1
                     }
                  ]
               },
               "price":12.99
            }
         ]
      },
   ]
}
```

### Solution

```lsf
version = DATA LOCAL STRING[100]();

GROUP item;
idItem = DATA LOCAL STRING[100] (OrderDetail);

FORM importOrder
    PROPERTIES() version

    OBJECTS order = Order
    PROPERTIES(order) date, number

    OBJECTS detail = OrderDetail
    PROPERTIES(detail) IN item idItem EXTID 'id'
    PROPERTIES(detail) price
    FILTERS order(detail) = order

    OBJECTS detailInfo = OrderDetailInfo IN item EXTID 'info'
    PROPERTIES(detailInfo) size, color, quantity
    FILTERS detail(detailInfo) = detail
;

importOrderFromJSON 'Import from JSON' () {
    INPUT f = FILE DO {
        IMPORT importOrder JSON FROM f;
        book(OrderDetail d) <- book(idItem(d)) WHERE idItem(d);
        APPLY;
    }
}

EXTEND FORM orders
    PROPERTIES() importOrderFromJSON DRAW o TOOLBAR
;
```

To implement the import process, we need to declare the form of the [structure](Structured_view.md) matching the structure of the JSON file.

We declare the `version` tag at the upmost level without inputs and then add it to the form.

Since the `order` tag is an array, we declare an object with the same name on the form. The platform will create a new object for each array element in the JSON. The `date` and `number` properties for the order will be automatically imported from the corresponding tags in the JSON.

Similarly, for the `detail` tag, we create an object with the same name and then link this object to the `order` object using `FILTERS`. During the import process, the system will fill the link in the order line based on this filter and the nesting of tags.

To import values from tags nested in the `item` tag, we create a new [group](Groups_of_properties_and_actions.md) called `item` and then place the properties and objects into it. In particular, the local property `idItem` is created and then added to the form in this group. Since the property name does not match the tag name, we specify the corresponding name for the property on the form using the `EXTID` keyword.
