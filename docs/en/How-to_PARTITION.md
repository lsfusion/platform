---
title: 'How-to: PARTITION'
---

## Example 1

### Task

We have an order with the lines.

```lsf
CLASS Order 'Order';
CLASS OrderDetail 'Order line';

order 'Order' = DATA Order (OrderDetail) NONULL DELETE;
```

We need to number the lines starting from 1 as they are added to the order.

### Solution

```lsf
index 'Line number' (OrderDetail d) = PARTITION SUM 1 ORDER d BY order(d) CHARWIDTH 4;
```

In this case, we sort by internal ID of lines in the order, since we know for sure that this ID increases when the new lines are created.

## Example 2

### Task

We have a list of customer orders with specified dates.

```lsf
date 'Date' = DATA DATE (Order);

CLASS Customer 'Customer';
customer 'Customer' = DATA Customer (Order);
```

For each order we need to find the date of the previous order placed by the same customer.

### Solution

```lsf
prevOrderDate 'Previous order' (Order o) = PARTITION PREV date(o) ORDER date(o), o BY customer(o);
```

Similar to [How-to: `GROUP CONCAT`](How-to_GROUP_CONCAT.md), the order should be uniquely determined. Therefore, we add the order itself (i. e. its internal ID) as the last argument for `ORDER`.

## Example 3

### Task

We have the current balance for books by batches.

```lsf
CLASS Book 'Book';

CLASS Batch 'Batch';
book 'Book' = DATA Book (Batch);
date 'Arrival date' = DATA DATE (Batch);

CLASS Stock 'Warehouse';
currentBalance 'Balance' = DATA INTEGER (Batch, Stock); // The balance is made data for the example. This is usually a calculated property.
```

We need to distribute the specified quantity for a specified book by batches according to the FIFO principle.

### Solution

```lsf
quantity = DATA LOCAL INTEGER (Book);

quantityFIFO 'Quantity by FIFO batch' (Batch b, Stock s) = PARTITION UNGROUP quantity
                                                                    LIMIT STRICT currentBalance(b, s)
                                                                    ORDER date(b), b
                                                                    BY book(b);
```

The `STRICT` parameter means that if the quantity is greater than the total balance for all batches, then the remaining difference will be added to the last batch.  
  
