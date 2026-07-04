# Migration notes

## 7.0

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
