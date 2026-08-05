### Title
Take an interval on any index field into account when planning, not only on the first

### Description

An interval condition on a property (`prop >= a AND prop < b`) is taken into account when estimating a query only if the property is the **first** field of some index. A property standing later in a composite index is estimated as if it were not indexed at all.

```lsf
CLASS Measurement;
device = DATA Device (Measurement);
dateTime = DATA DATETIME (Measurement);

INDEX device(Measurement m), dateTime(m);

// estimated as a full scan of Measurement, although the index covers the condition exactly
selected (Measurement m) = device(m) = currentDevice()
    AND dateTime(m) >= fromTime() AND dateTime(m) < toTime();
```

Such an interval is now taken into account for any field of an ordinary `INDEX` (`LIKE` and `MATCH` indexes are unaffected beyond their first field).

Note what is and is not decided here. The interval says **how many rows survive the condition**, and that is true regardless of the fields preceding it in the index - a narrow window over a datetime leaves few rows whether or not the device is fixed. **How expensive it is to reach those rows** is a separate question, and it keeps being answered as before: the access cost is computed by walking the real index field by field and stopping at the first field the query does not constrain. So an interval on a later field is no longer invisible, but neither does it pretend an index scan is available when the preceding fields are not bound.

Conditioning the estimate on the preceding fields being bound was considered and rejected: it suppresses a true row estimate in order to police a cost claim that is made elsewhere, and any workable definition of "bound" turned out to be either too narrow - an equality with a value taken from the enclosing query is not represented the same way as an equality with a constant and would have been missed, although the database serves exactly that case from the index - or wide enough to be meaningless.

This is an estimation change: results do not change, plans do. The previous behavior can be restored with the `prefixIndexIntervalBackwardCompatibility` setting, which is read while the indexes are registered at startup.

### Reason

Filtering a large table by "one object over a time window" is a common shape - measurements of a sensor, documents of a supplier, actions of a user. The index for it naturally starts with the object, so the interval always lands on a later field and stayed invisible to the planner. On large volumes the estimate is then off by orders of magnitude: on a 292M row table a one hour window was estimated as the whole table instead of the few hundred rows it actually returns. Inside a correlated aggregation that estimate drives the plan into a scan per outer row, which shows up as multi-minute queries and gigabytes of temporary files.
