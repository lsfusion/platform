---
title: 'Distribution (UNGROUP)'
---

The *distribution* operator creates a property, the [grouping](Grouping_GROUP.md) value of which using sum function will be equal to the value of the specified property (*distributable*). Accordingly, as for a group operator, for a distribution operator multiple properties (*groups*) must be set by which the grouping will take place.

There are many different ways to build this kind of distribution. At present the platform supports the two most commonly used:

1.  Limiting - the distribution result must not exceed the value of the specified property.
2.  Proportional - the distribution result should be directly proportional to the value of a given property (in other words, the ratio of the distribution results for two object collections within the same group should be equal to the ratio of the values of this property for the same object collections). 

The operator can work in *non-strict* mode (used by default). Here the platform tries to calculate the value as close as possible to the value of the distributable property but does not guarantee that they will be equal.

As for other operations with sets, an *order* can (and usually must) be defined for the distribution operator.

The result class of the distribution follows the class of the per-element shape expression — the limit for the limiting form, or the proportion for the proportional form — rather than the class of the distributable property itself. So when the limit / proportion has a wider or different numeric class than the distributable value, the result takes the class of the limit / proportion.

The general algorithm of the distribution operator, depending on the type of distribution, is as follows:

1.  Limiting - distribution is done in the specified order, not exceeding the restriction, until the overall result equals the value of the distributable property. If the operator is working in strict mode and the overall result has not reached the value of the distributable property, the total difference is added to the resulting value of the first object collection.
2.  Proportional - the total of the proportions for each group is calculated, after which a distribution coefficient is determined for each object collection, equal to the ratio of the proportion value for this set of objects to the total amount of the group to which it belongs. Finally, the distribution result is calculated as the product of this coefficient and the value of the distributed property. Since the final (and intermediate) results are rounded (and hence accuracy is lost), the sum of the result of this distribution may differ from the value of the distributed property. Therefore, if the operator is working in strict mode, the difference between these values is added to the resulting value of the first object collection.

### Extended form

The mechanism described above allows distribution only in "one-to-many" mode. However, in some cases this is not enough, and distribution in "many-to-many" mode is necessary. For this, the platform has the so-called *extended* distribution operator form (consequently, the basic form will be called *simple*). 

In the extended form of this operator, the conditions for the distribution result are changed as follows:

1.  Limiting - it is not the distribution result itself that must not exceed the value of the specified property, but rather the grouping of the distribution result by certain additional groups must not exceed this value.
2.  Proportional - similar; that is, it is not the result of the distribution itself that must be directly proportional to the value of a certain property, but its grouping by additional groups.

The algorithm of the operator’s work likewise changes accordingly.

### Language

Since the simple form of the operator is semantically very similar to the operator [partition/sort](Partitioning_sorting_PARTITION_..._ORDER.md), to declare a property that implements a simple distribution the [`PARTITION` operator](PARTITION_operator.md) is also used.

For the extended form, use the [`UNGROUP` operator](UNGROUP_operator.md).

### Examples

```lsf
CLASS Invoice;
totalCost 'Total cost' = DATA NUMERIC[10,2] (Invoice);

CLASS InvoiceLine;
invoice = DATA Invoice (InvoiceLine) NONULL DELETE;
weight = DATA NUMERIC[14,3] (InvoiceLine);

// proportional distribution: split totalCost across the lines of each invoice proportionally to weight,
// rounding to two decimals; the rounding remainder goes to the first line in the chosen order
lineCost 'Cost by line' (InvoiceLine l) = PARTITION UNGROUP totalCost
                                                    PROPORTION STRICT ROUND(2) weight(l)
                                                    ORDER l
                                                    BY invoice(l);

// limiting distribution: allocate totalCost across the lines of each invoice in priority order,
// bounded by each line's quota, until the source value is exhausted
quota = DATA NUMERIC[14,2] (InvoiceLine);
priority = DATA INTEGER (InvoiceLine);
allocated 'Allocated cost' (InvoiceLine l) = PARTITION UNGROUP totalCost
                                                       LIMIT quota(l)
                                                       ORDER priority(l), l
                                                       BY invoice(l);
```
