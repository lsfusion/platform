---
slug: "/How-to_Heavy_computations"
title: 'How-to: Heavy computations'
---

A [calculated property](../paradigm/Properties.md#calculated) created by a [grouping](../paradigm/Grouping_GROUP.md) is, unless it is materialized, computed inside every query that reads it, as a subquery. When an action needs such a property for a part of the objects only and the condition that selects these objects does not restrict the subquery, the grouping can be computed for all objects, with the selection applied to its result, so the time of the action can grow with the size of the tables rather than with the number of the objects it needs. Which conditions the platform moves inside a subquery is described in [materializations](../paradigm/Materializations.md). Whether a condition has been moved is seen in the text of the query in the [`explain` log](../paradigm/Journals_and_logs.md#logs): what has been moved appears inside the subquery of the grouping as well, and a condition that has not been moved stands only outside it.

The examples use orders and a history of prices of SKUs:

```lsf
CLASS Sku 'SKU';
CLASS Stock 'Warehouse';

CLASS Order 'Order';
date 'Date' = DATA DATE (Order) INDEXED;
shipped 'Shipped' = DATA BOOLEAN (Order);
invoiced 'Invoiced' = DATA BOOLEAN (Order);

CLASS OrderDetail 'Order line';
order 'Order' = DATA Order (OrderDetail) NONULL DELETE INDEXED;
sku 'SKU' = DATA Sku (OrderDetail);
quantity 'Quantity' = DATA NUMERIC[16,3] (OrderDetail);

CLASS PriceChange 'Price change';
sku 'SKU' = DATA Sku (PriceChange);
date 'Valid from' = DATA DATE (PriceChange);
price 'Price' = DATA NUMERIC[16,4] (PriceChange);
INDEX sku(PriceChange c), date(c), c;

// the price of an SKU on a date
price 'Price' (Sku s, DATE d) = GROUP LAST price(PriceChange c) ORDER date(c), c WHERE date(c) <= d BY sku(c);
// the sum of an order at the prices of its date
sum 'Sum' (Order o) = GROUP SUM quantity(OrderDetail d) * price(sku(d), date(order(d))) BY order(d);
```

## Example 1 {#example-1}

### Task

We need an action that marks the orders to be invoiced: the shipped orders dated not later than a given date that are not invoiced yet and have a positive sum. Almost all earlier orders are invoiced already, so few orders are marked.

The selection can be written as one assignment:

```lsf
toInvoice 'To be invoiced' = DATA LOCAL BOOLEAN (Order);

selectToInvoice 'Select orders to be invoiced' (DATE to)  {
    toInvoice(Order o) <- NULL;
    toInvoice(Order o) <- TRUE WHERE date(o) <= to AND shipped(o) AND NOT invoiced(o) AND sum(o) > 0;
}
```

Neither a date bounded on one side only nor the absence of the `invoiced[Order]` flag is counted by the platform as restricting the subquery of `sum[Order]`. The sums can then be computed for all orders, with a price found for every line of the whole history, and the conditions on the order applied to the result.

### Solution

```lsf
selectToInvoiceInSteps 'Select orders to be invoiced' (DATE to)  {
    toInvoice(Order o) <- NULL;

    // the orders selected by their own stored properties
    LOCAL candidate = BOOLEAN (Order);
    candidate(Order o) <- TRUE WHERE date(o) <= to AND shipped(o) AND NOT invoiced(o);

    // the sum is needed for them only
    toInvoice(Order o) <- TRUE WHERE candidate(o) AND sum(o) > 0;
}
```

The assignment to `candidate[Order]` reads only the stored properties of an order, so the DBMS selects the candidates by all its conditions at once. The last assignment reads `sum[Order]` for the orders written into this [local property](../paradigm/Data_properties_DATA.md#local). The platform knows how many objects such a property holds and restricts the subquery by them when it estimates that as cheaper, as in this example, where the candidates are few among many orders.

A condition is worth taking out into such an assignment when the platform does not count it as restricting, as it does not count the date with one bound and the absence of the flag here. The equality of a stored property to a given value and a range between two given values of an indexed property of a date or time class are counted, so they can restrict the subquery by themselves: in an action that takes both bounds of a period, `(DATE from, DATE to)`, the single assignment with `date(o) >= from AND date(o) <= to` in place of `date(o) <= to` can compute the sums of the orders of that period only.

## Example 2

### Task

Similar to [**Example 1**](#example-1), but for each marked order the action also needs its sum at the current prices.

```lsf
currentSum 'Sum at current prices' (Order o) = GROUP SUM quantity(OrderDetail d) * price(sku(d), currentDate()) BY order(d);

invoiceCurrentSum 'Sum at current prices' = DATA LOCAL NUMERIC[18,4] (Order);
```

### Solution

```lsf
fillCurrentSums 'Fill sums at current prices' (DATE to)  {
    selectToInvoiceInSteps(to);

    invoiceCurrentSum(Order o) <- NULL;
    invoiceCurrentSum(Order o) <- NUMERIC[18,4](currentSum(o)) WHERE toInvoice(o);
}
```

The second assignment has the marked orders as its condition, so the second grouping can be restricted by them in the same way. A local property filled once serves every expensive property that is needed for the same objects, so the conditions that select them are not repeated in each assignment.

## Example 3

### Task

An action that prepares the price tags of a warehouse has read the balance of each SKU in the warehouse into a local property. We need the prices, on a given date, of the SKUs that are in stock.

```lsf
tagBalance 'Balance' = DATA LOCAL NUMERIC[16,3] (Sku);
tagPrice 'Price' = DATA LOCAL NUMERIC[16,4] (Sku);
```

### Solution

```lsf
fillTagPrices 'Fill prices of price tags' (DATE d)  {
    tagPrice(Sku s) <- NULL;

    // the SKUs in stock are written into a local property of their own
    LOCAL inStock = BOOLEAN (Sku);
    inStock(Sku s) <- TRUE WHERE tagBalance(s) > 0;

    tagPrice(Sku s) <- price(s, d) WHERE inStock(s);
}
```

The comparison of a balance with zero is a range with one bound, so in `WHERE tagBalance(s) > 0` only the presence of a value in `tagBalance[Sku]` is counted as restricting, not the comparison itself. Since `tagBalance[Sku]` holds the SKUs with a zero balance as well, the query that reads the prices by this condition can group the whole history of prices before it checks the balance. Placed in the assignment that fills one more local property, the comparison leaves in that property only the SKUs in stock.

The number of the objects matters as well: the platform restricts a subquery by a local property when it estimates the restricted subquery as cheaper, and the estimate uses the number of the objects written into the local property. A local property that restricts an expensive grouping should therefore hold only the objects the grouping is needed for.

## Example 4

### Task

We need the value of the stock of each warehouse on each of the selected dates. The balance on a date is calculated from the movements of the SKUs.

```lsf
CLASS Ledger 'Movement';
sku 'SKU' = DATA Sku (Ledger);
stock 'Warehouse' = DATA Stock (Ledger);
date 'Date' = DATA DATE (Ledger);
quantity 'Quantity' = DATA NUMERIC[16,3] (Ledger);

balance 'Balance on date' (Sku s, Stock st, DATE d) = GROUP SUM quantity(Ledger l) IF date(l) <= d BY sku(l), stock(l);
value 'Value on date' (Stock st, DATE d) = GROUP SUM balance(Sku s, st, d) * price(s, d);

selected 'Selected' = DATA LOCAL BOOLEAN (DATE);
stockValue 'Value' = DATA LOCAL NUMERIC[20,4] (Stock, DATE);
```

### Solution

```lsf
fillStockValues 'Fill values' ()  {
    stockValue(Stock st, DATE d) <- NULL;

    FOR selected(DATE d) NOINLINE DO
        stockValue(Stock st, d) <- NUMERIC[20,4](value(st, d)) WHERE st IS Stock;
}
```

In one assignment — `stockValue(Stock st, DATE d) <- NUMERIC[20,4](value(st, d)) WHERE selected(d) AND st IS Stock;` — the selected dates are read from `selected[DATE]` inside the query. Every movement is joined with all the selected dates not earlier than its own, and the data of all the dates is grouped by that query, so the volume of its intermediate data grows with the number of the dates, and the DBMS can write this data to temporary files. The loop executes a query for each date, with the date passed as a value, and each query processes the data of one date. The total time of the two variants can be close: what the loop limits is the volume of data one query processes, so it is the variant to choose when that volume, not the time, is what the single assignment runs into.

The `NOINLINE` option is needed here: without it the platform can execute a loop whose body is an assignment as that same single assignment (see [How-to: FOR](How-to_FOR.md), where the assignment for all objects at once is otherwise the preferred form).

## Example 5

### Task

We need an action that marks the SKUs that have a price change dated after a given date.

The condition can be written as a comparison of two groupings, the last price change of the SKU and its last price change not later than the date:

```lsf
lastChange 'Last price change' (Sku s) = GROUP LAST PriceChange c ORDER date(c), c WHERE date(c) BY sku(c);
lastChange 'Last price change as of date' (Sku s, DATE d) = GROUP LAST PriceChange c ORDER date(c), c WHERE date(c) <= d BY sku(c);

repriced 'Repriced' = DATA LOCAL BOOLEAN (Sku);

selectRepriced 'Select repriced SKUs' (DATE d)  {
    repriced(Sku s) <- NULL;
    repriced(Sku s) <- TRUE WHERE d AND lastChange(s) AND NOT lastChange(s, d) = lastChange(s);
}
```

Both groupings are then computed for all SKUs before they can be compared.

### Solution

```lsf
lastChangeDate 'Date of the last price change' (Sku s) = GROUP MAX date(PriceChange c) BY sku(c) MATERIALIZED INDEXED;

selectRepricedByDate 'Select repriced SKUs' (DATE d)  {
    repriced(Sku s) <- NULL;
    repriced(Sku s) <- TRUE WHERE lastChangeDate(s) > d;
}
```

The condition that selects the objects is written over a materialized property. In a session that has not changed the history of prices its values are read as stored, and with an [index](../paradigm/Indexes.md) on it the DBMS can find the SKUs by the date without reading the history of prices. The same holds when another grouping aggregates over the objects selected by such a condition inside it, summing the order lines of those SKUs for example: a materialized property lets the DBMS find them first, while a comparison of groupings that are not materialized is checked only after they are computed. As a condition that selects the objects for a grouping from outside, it remains a range with one bound, the kind of condition that [**Example 1**](#example-1) writes into a local property first.
