---
title: 'Execution'
sidebar_label: Overview
---

If the system is small and there is not much data in it, it usually works quite efficiently without any additional optimizations. But if the logic becomes more complex, and the amount of data increases significantly, it often makes sense to tell the platform how best to store and process all this data.

The platform provides two main mechanisms for working with data: [properties](Properties.md) and [actions](Actions.md). The first is responsible for storing and calculating data, and the second for changing the system state from one to another. Actions can be optimized to quite a limited extent (among other things, because of aftereffects), but for properties, there is a whole set of features that allows to reduce the response time of specific operations and increase overall system performance:

### Materializations

If a property is read a large number of times (significantly more often than it is changed), the performance of operations using this property can be significantly improved by [materializing](Materializations.md) it.

### Indexes

If a property is often involved in the calculation of other properties, it can make sense to build an [index](Indexes.md) with that property.

### Tables

If the same properties are often read/changed for the same object collection at the same time, storing each such property separately can be quite inefficient. So the platform allows to "group" properties into so called [tables](Tables.md).

  
