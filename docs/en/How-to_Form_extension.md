---
title: 'How-to: Form extension'
---

Let's assume there is a module that describes the `Sku` form that is used to edit SKU values:

```lsf
MODULE Sku;

CLASS Sku; // declaring class Sku

name 'Name' = DATA BPSTRING[100] (Sku); // creating a name property for it

FORM sku 'Sku' // creating the Item form
    OBJECTS s = Sku PANEL // adding a product object and making it display exactly one copy
    PROPERTIES(s) name // adding the product name property to the form

    EDIT Sku OBJECT s;
;

DESIGN sku {
    NEW skuDetails AFTER BOX(s) { // creating a new container in a standard container right after i.box
                                  // this container will be the tab panel, where tabs with product properties can be added
        type = TABBED;
        fill = 1; // let it expand to the whole form
    }
}
```

We need to implement additional functionality for adding multiple barcodes to an SKU. This can be done by creating a new module that will introduce a new `Barcode` class and extend the functionality of the Sku edit form by adding the possibility to enter barcodes:

```lsf
MODULE Barcode;

REQUIRE Sku;

CLASS Barcode; // declaring a barcode class

id = DATA BPSTRING[13] (Barcode); // creating a property with a barcode number
sku = DATA Sku (Barcode); // creating a barcode link to sku

EXTEND FORM sku // creating the Item form
    OBJECTS b = Barcode // adding the barcode object
    PROPERTIES(b) id // adding the barcode number to the barcode table
    PROPERTIES(b) NEW, DELETE // adding actions to create and delete barcodes
    FILTERS sku(b) == s // making that only barcodes of this sku are displayed
;

DESIGN sku { // expanding the design of the Item form
    skuDetails {
        MOVE BOX(b); // making a container, which contains everything related to barcodes, by a tab in the previously created tab panel
    }
}
```

Note that the `Barcode` module assumes that there is a `Sku` form with an `s` object and a container called `skuDetails`. If the form changes for some reason, the `Barcode` module will become inoperable.
