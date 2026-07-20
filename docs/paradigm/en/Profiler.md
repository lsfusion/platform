---
slug: "/Profiler"
title: 'Profiler'
---

The *profiler* is a built-in platform mechanism that collects execution statistics for properties and actions over a given time interval and presents them as a call graph with aggregated metrics. Unlike the [process monitor](Process_monitor.md), which shows the system state at the current moment, the profiler describes system behavior over a past interval.

### Start and stop

Data collection is controlled by a pair of actions, *Start profiling* and *Stop profiling*, available on the `Administration > System > Profiler` form. From start to stop the platform records property and action invocations; the data is persisted on the application server and becomes available for analysis once profiling is stopped.

### Call graph

Each profiler record corresponds to an *arc* — a *(caller, callee)* pair in the context of a user and a form. The arcs form a directed graph: the same callee may be the endpoint of several arcs with different callers, and the same caller may be the source of several arcs with different callees. The graph can be walked in both directions — *down* from a given property to the ones it calls, and *up* to the ones that call it.

### Metrics

For each arc the profiler stores raw metrics:

|Metric|Meaning|
|---|---|
|Call count|How many times the arc was traversed during the interval|
|Total time|Sum of callee execution time, including everything it calls|
|Min and max time|Bounds over individual calls|
|Sum of squared times|Used to compute the root-mean-square runtime|
|SQL time|Part of the total time spent waiting for the database server|
|User-interaction time|Part of the total time spent waiting for user input or for a response from the client side|

From these the profiler computes derived metrics:

|Metric|Rule|
|---|---|
|Average time|Total time divided by call count|
|Root mean square of runtime|`sqrt(sum of squared times / call count)`|
|Total time without wait|Total time minus user-interaction time|
|Total Java time|Total time without wait minus SQL time|
|Inherent time without wait|Total time without wait minus the same metric summed over the callees|
|Inherent SQL time|SQL time minus the SQL time summed over the callees|
|Inherent Java time|Total Java time minus the total Java time summed over the callees|

The *inherent* family attributes the time to the property or action itself rather than to its nested calls, which makes a hot spot stand out even when most of the time is spent in shared helpers it invokes.

### Data presentation

The collected data is shown on several tabs:

|Tab|What it shows|
|---|---|
|Settings|Profiler start/stop, choice of active metrics, user and form filters|
|Hot spots|A flat list of properties and actions sorted by the chosen metric|
|Call tree|A hierarchical view of arcs — *down* (what the selected node calls) and *up* (who calls it)|
|Raw data|Full list of arcs for arbitrary analysis|

User and form filters narrow the selection to a specific context; metrics are recomputed from the same underlying data set, no rerun of the profiler is required.

### Query-plan analysis settings

The same form has a separate `SQL` tab that configures parameters affecting how SQL query plans are written to the logs (see [Journals and logs](Journals_and_logs.md#logs)):

|Parameter|Meaning|
|---|---|
|No analyze|Ask the database server for the query plan in a lightweight mode, without actually executing the query|
|Java stack|Include in the log the Java stack from which the query was issued|
|Compile plan|Log the compile plan as well, not only the execution plan|
|Time threshold|Minimum query execution time starting from which a plan reaches the log|

These parameters are a separate mechanism, not included in the graph the profiler collects, but typically used alongside it for in-depth analysis of specific «hot spots».
