---
title: 'Partitioning / sorting (PARTITION ... ORDER)'
---

The *partition/order* operator creates a [property](Properties.md) that partitions all objects collections in the system into *groups*, and using the specified *order* calculates an [aggregate function](Set_operations.md#func) for each objects collection. Accordingly, the set on which this aggregating function is calculated is determined as following: all object collections of the group of this object collection, and the order of which is less than or equal to the order of this object collection. 

Groups in this operator are defined as a set of properties (*groupings*), and the order is defined as a list of properties and a marker of increasing or decreasing. If the aggregation function is not [commutative](Set_operations.md#commutative), the order must be uniquely determined. 

Note that the partition/order operator is very similar to the [grouping operator](Grouping_GROUP.md), but unlike the latter, it computes a result not for grouping values, but for the object collections for which calculation is taking place.

### Language

To declare a property that implements partition/order, use the [`PARTITION` operator](PARTITION_operator.md). 

### Examples

```lsf
// determines the place of the team in the conference
CLASS Conference;
conference = DATA Conference (Team);
points = DATA INTEGER (Team);
gamesWon = DATA INTEGER (Team);
place 'Place' (Team team) = PARTITION SUM 1 ORDER DESC points(team), gamesWon(team) BY conference(team);

// building ordinal indexes of objects in the database in ascending order of their internal IDs (i.e., in the order of creation)
index 'Number' (Object o) = PARTITION SUM 1 IF o IS Object ORDER o;

// finds the team next in the conference standings
prevTeam (Team team) = PARTITION PREV team ORDER place(team), team BY conference(team);

// proportional distribution example
CLASS Order;
transportSum 'Freight costs' = DATA NUMERIC[10,2] (Order);

CLASS OrderDetail;
order = DATA Order (OrderDetail) NONULL DELETE;
sum = DATA NUMERIC[14,2] (OrderDetail);

transportSum 'Freight costs by line' (OrderDetail d) = PARTITION UNGROUP transportSum
                                    PROPORTION STRICT ROUND(2) sum(d)
                                    ORDER d
                                    BY order(d);

// example of distribution with limits
discountSum 'Discount' = DATA NUMERIC[10,2] (Order);
discountSum 'Discount by line' (OrderDetail d) =
    PARTITION UNGROUP discountSum
                LIMIT STRICT sum(d)
                ORDER sum(d), d
                BY order(d);
;
```
