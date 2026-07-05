# Migration notes

## 7.0

### Deterministic order-dependent aggregations

Order-dependent aggregations and row picks (`GROUP LAST` / `CONCAT` / ordered `CUSTOM`,
`TOP` picks, the order-sensitive `PARTITION` types, `GROUP AGGR` / `NAGGR` / `EQUAL` on
duplicate `BY` keys, assignments with extra `WHERE` parameters, the `FOR ... NEW`
numbering) now implicitly append the object order as the `ORDER` tiebreak
(see [issue #1700](https://github.com/lsfusion/platform/issues/1700)). Results that
previously depended on the SQL scan order become stable; queries with a total explicit
`ORDER` are unaffected. The compiled plans change slightly (longer aggregate / window
`ORDER BY`), and where the order was not total the picked row / concatenation order may
differ from what a particular database happened to return before - if a specific pick is
required, specify a total `ORDER` explicitly. There is no fallback setting: the previous
behavior was nondeterministic.


### Predicate push down planning changes

7.0 reworks two aspects of predicate push down planning:

- interval comparisons on keys (`a <= key`, `key <= b`) participate in the join statistics
  (key compare joins), and interval keys are sourced by an iteration instead of being
  protected by hanging-key heuristics;
- when a predicate is pushed into an aggregation, the joins that depend on the push target
  are no longer cut from the pushed condition wholesale: their depending arguments are
  virtualized (replaced with a fresh key), so the pushed condition keeps its key sources
  (see [issue #1699](https://github.com/lsfusion/platform/issues/1699)).

Both changes affect query compilation globally: complex queries (filtered aggregations over
intervals, nested aggregations, report totals and column footers) compile into different -
usually better - plans. However, on large production volumes or in rare query shapes this
may surface as:

- queries failing with the incorrect set operation error
  (`Операция над множеством некорректна`) that worked on 6.x;
- performance degradation of specific complex queries.

#### Quick fallback

The pre-7.0 planner behavior can be restored without downgrading, via `settings.properties`
(or `-D` JVM parameters):

```
settings.removeJoinCutBackwardCompatibility = true
settings.keyExprCompareJoinBackwardCompatibility = true
```

The first setting reverts the push down recursion guard to cutting the dependent joins
wholesale, the second reverts the interval comparison handling. Setting both restores the
pre-7.0 push down planning entirely. The settings can also be applied selectively to
localize which of the two changes causes a particular problem. See the
[Working parameters](https://docs.lsfusion.org/Working_parameters/) article.
