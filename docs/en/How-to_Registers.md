---
title: 'How-to: Registers'
---

### Accumulation ledger

Let's assume we need to implement the logic for calculating the SKU balances.

```lsf
REQUIRE Utils;

CLASS SKU 'SKU';
CLASS Stock 'Warehouse';
```

Theoretically we could just create a [property](Properties.md) that would sum up all incomings and subtract all outgoings, with all operations set explicitly. The weakness of this approach is that whenever a new operation is added, it needs to be added to the formula for calculating the balance. In addition, it will be difficult to build a form with a list of all the operations that can affect the balance for a specific SKU and warehouse. [Modularity](Modularity.md) will also be violated, because the module in which the balance property is declared will depend on all modules containing operations that affect it.

To give the system efficient [extensibility](Extensions.md), it is best to implement this kind of functionality using *ledgers*. To do this, we introduce an abstract class `SKULedger`. One instance of the class will reflect a single change in the balance by a given amount (positive or negative) for one SKU in one warehouse. Abstract properties are set for it, which need to be defined when the class is implemented.


:::info
All ledgers can have an arbitrary number and type of measurements. In this example they are the SKU and the Warehouse.
:::

```lsf
CLASS ABSTRACT SKULedger 'Register of changes in the product balance';

posted 'Completed' = ABSTRACT BOOLEAN (SKULedger);
dateTime 'Date/time' = ABSTRACT DATETIME (SKULedger);

sku 'SKU' = ABSTRACT SKU (SKULedger);
stock 'Warehouse' = ABSTRACT Stock (SKULedger);

quantity 'Qty' = ABSTRACT NUMERIC[14,2] (SKULedger);

balance 'Balance' = GROUP SUM quantity(SKULedger l) IF posted(l) BY stock(l), sku(l);

balance 'Balance as of date/time' = GROUP SUM quantity(SKULedger l) IF posted(l) AND dateTime(l) <= DATETIME dt BY stock(l), sku(l), dateTime(l);
```

The current balance and the balance for a certain time period are calculated only from the properties of the `SKULedger` class without reference to specific operations. This code can and must be declared in a separate module. Modules containing specific operations will use and extend this class.

For example, let's look at one operation: *Stock receipt*.

```lsf
CLASS Receipt 'Warehouse arrival';
posted 'Completed' = DATA BOOLEAN (Receipt);
dateTime 'Date/time' = DATA DATETIME (Receipt);

stock 'Warehouse' = DATA Stock (Receipt);

CLASS ReceiptDetail 'Warehouse arrival line';
receipt 'Arrival' = DATA Receipt (ReceiptDetail) NONULL DELETE;

sku 'SKU' = DATA SKU (ReceiptDetail);

quantity 'Qty' = DATA NUMERIC[14,2] (ReceiptDetail);
price 'Price' = DATA NUMERIC[14,2] (ReceiptDetail);
```

To "post" it into the ledger, we need to [extend the class](Class_extension.md) `SKULedger` with a `ReceiptDetail` class for stock receipt. We also need to [extend the properties](Property_extension.md) of the ledger.

```lsf
EXTEND CLASS ReceiptDetail : SKULedger;

// [SKULedger] must be specified, since ReceiptDetail also inherits PriceLedger in the same example and the platform needs to know which property needs to be implemented
posted[SKULedger](ReceiptDetail d) += posted(receipt(d));
dateTime[SKULedger](ReceiptDetail d) += dateTime(receipt(d));

stock[SKULedger](ReceiptDetail d) += stock(receipt(d));

sku[SKULedger](ReceiptDetail d) += sku(d);
quantity[SKULedger](ReceiptDetail d) += quantity(d);
```

Let's look at a more complex case, when we have a document recording transfer from one warehouse to another.

```lsf
CLASS Transfer 'Moving from warehouse to warehouse';
posted 'Completed' = DATA BOOLEAN (Transfer);
dateTime 'Date/time' = DATA DATETIME (Transfer);

fromStock 'Warehouse (from)' = DATA Stock (Transfer);
toStock 'Warehouse (to)' = DATA Stock (Transfer);

CLASS TransferDetail 'Warehouse shipment line';
transfer 'Arrival' = DATA Transfer (TransferDetail) NONULL DELETE;

sku 'SKU' = DATA SKU (TransferDetail);

quantity 'Qty' = DATA NUMERIC[14,2] (TransferDetail);
price 'Price' = DATA NUMERIC[14,2] (TransferDetail);
```

In this case, the data from the document must be "posted" into the ledger twice. By analogy with stock receipt, we will post the line into the ledger as an outgoing operation with a negative value.

```lsf
EXTEND CLASS TransferDetail : SKULedger;

posted(TransferDetail d) += posted(transfer(d));
dateTime(TransferDetail d) += dateTime(transfer(d));

stock(TransferDetail d) += fromStock(transfer(d));

sku(TransferDetail d) += sku(d);
quantity(TransferDetail d) += -quantity(d);
```

To post it into the ledger for the warehouse where the SKUs are being transferred to, we use object [aggregation](Aggregations.md). The line in the transfer document will generate an object, which in turn will be "posted" into the ledger.

```lsf
CLASS TransferSKULedger 'Moving to warehouse (register)' : SKULedger;
transferSKULedger = AGGR TransferSKULedger WHERE posted(TransferDetail transferDetail);

posted(TransferSKULedger d) += d IS TransferSKULedger;
dateTime(TransferSKULedger d) += dateTime(transfer(transferDetail(d)));

stock(TransferSKULedger d) += toStock(transfer(transferDetail(d)));

sku(TransferSKULedger d) += sku(transferDetail(d));
quantity(TransferSKULedger d) += quantity(transferDetail(d));
```

The ledger object will only be created when the transfer document has been posted. Therefore, in this case the `posted` property will always equal `TRUE`.

It should be noted that documents with one warehouse can also be posted into the ledger using aggregation. The aggregation scheme is more flexible but requires the creation of additional objects in the system, which may be worse from a performance perspective.

### Information ledger

The *information ledger* technique makes it possible to implement the logic of changing a certain indicator over time in a flexible way. Unlike the inventory ledger, it calculates not the sum of an indicator but its latest value over a certain period of time.

To implement this technique we introduce an abstract class `PriceLedger`. Its instance reflects a single price change for one SKU and one warehouse at a certain time.

```lsf
CLASS ABSTRACT PriceLedger 'Receipt price change register';

posted 'Completed' = ABSTRACT BOOLEAN (PriceLedger);
dateTime 'Date/time' = ABSTRACT DATETIME (PriceLedger);

sku 'SKU' = ABSTRACT SKU (PriceLedger);
stock 'Warehouse' = ABSTRACT Stock (PriceLedger);

price 'Price' = ABSTRACT NUMERIC[14,2] (PriceLedger);

price 'Price' (Stock stock, SKU sku, DATETIME dateTime) =
    GROUP LAST price(PriceLedger l)
          ORDER dateTime(l), l
          WHERE posted(l) AND dateTime(l) <= dateTime
          BY stock(l), sku(l);

price 'Price' (Stock stock, SKU sku) =
    GROUP LAST price(PriceLedger l)
          ORDER dateTime(l), l
          WHERE posted(l)
          BY stock(l), sku(l);

price 'Price' (SKU sku, DATETIME dateTime) =
    GROUP LAST price(PriceLedger l)
          ORDER dateTime(l), l
          WHERE posted(l) AND dateTime(l) <= dateTime
          BY sku(l);
```

As a result, we get properties giving the price by SKU and warehouse for the date/time, the latest price, and also the latest price for that SKU for all warehouses.

Documents are posted into the information ledger the same way they are posted into the inventory ledger.

```lsf
EXTEND CLASS ReceiptDetail : PriceLedger;

// [PriceLedger] must be specified, since ReceiptDetail also inherits SKULedger in the same example and the platform needs to know which property to implement
posted[PriceLedger](ReceiptDetail d) += posted(receipt(d));
dateTime[PriceLedger](ReceiptDetail d) += dateTime(receipt(d));

stock[PriceLedger](ReceiptDetail d) += stock(receipt(d));

sku[PriceLedger](ReceiptDetail d) += sku(d);
price[PriceLedger](ReceiptDetail d) += price(d);
```

In this case the signature of the abstract property needs to be specified explicitly, because there can be several of them with the same name and namespace (properties are named in just the same way for class `SKULedger`).
