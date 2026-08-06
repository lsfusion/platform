### Title
Do not lose the index plan for an ordered aggregation when the session has unsaved changes

### Description
A `GROUP LAST` (or `PARTITION`) over an indexed ordering column degrades from an index lookup per key
to a full scan of the whole table as soon as the session has an **unsaved change of the property the
aggregation is ordered by**.

```lsf
CLASS Device;
CLASS Measurement;

device = DATA Device (Measurement);
dateTime = DATA DATETIME (Measurement);
value = DATA NUMERIC[10,2] (Measurement);

INDEX device(Measurement m), dateTime(m);

lastMeasurement = GROUP LAST Measurement m ORDER dateTime(m), m BY device(m);
lastValue (Device d) = value(lastMeasurement(d));
```

```lsf
// fast - one index lookup per device
showLast () {
    FOR Device d IS Device DO
        MESSAGE lastValue(d);
}

// the same, but the session holds an UNSAVED change of a measurement's date
showLastAfterEdit () {
    NEWSESSION {
        dateTime(someMeasurement()) <- sumSeconds(dateTime(someMeasurement()), -60);

        FOR Device d IS Device DO
            MESSAGE lastValue(d);       // reads the whole measurement table and sorts it

        CANCEL;
    }
}
```

On a table of 111M measurements read for 25 devices the first form takes 0.25 s. The second reads and
sorts the whole table: it runs for 63 s, spills more than 8 GB of temporary files, and on a server with
`temp_file_limit` set it does not finish at all - it fails with a postgres error.

The condition is narrow and worth stating exactly: only an unsaved change of the property used in
`ORDER` triggers it. Changes of other properties of the same object, changes of the grouped object,
and creating new objects are all harmless. The size of the change does not matter either - one edited
row behaves the same as eighty.

Practical consequence: a form on which the ordering property can be edited becomes "infectious" -
after such an edit every later read in the same session, including unrelated reports, goes over the
full table until the session is saved or cancelled.

### Reason
An unsaved change is applied to the property by substitution, so the ordering stops being the plain
indexed column and becomes a computed expression. The planner then sees no index for the ordering,
concludes that restricting the aggregation to the requested keys buys nothing, and therefore computes
the aggregation for all keys over the whole history. The cost stops depending on how many keys were
asked for and starts depending on the size of the table.

### Fix
The ordering of such an aggregation is compiled per branch (the changed rows and the rest), and in
each branch the ordering is the plain column again, so the index applies. The planner now takes that
into account when estimating, and the per-key index plan is chosen as it is for a clean session.
