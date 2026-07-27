# Migration notes

## 7.0

### Nested transactions and user interaction in a transaction now fail

Two situations that used to be tolerated now raise an error
(see [issue #1726](https://github.com/lsfusion/platform/issues/1726)):

- **another session working inside a transaction** - a session doing anything on an SQL
  session that another one (typically opened earlier in the same thread) already holds a
  transaction on. Its statements run in that transaction : they see its uncommitted data
  and disappear with its rollback, while the session itself believes it is outside any
  transaction. The extreme case is an apply, which never had a transaction of its own -
  its commit was physically nothing, so the data was silently lost, and the temp tables
  it created turned into the "relation t_N does not exist" errors of
  [issue #1716](https://github.com/lsfusion/platform/issues/1716). The service operations
  that do this knowingly (database synchronization, recalculations, checks) take it on
  with the setting below, the same way an application can;
- **a user interaction inside a transaction** - a dialog, a message or an input request
  raised from a form or navigator action executed under `APPLY` (an apply event, for
  instance). It used to be an assert, that is a log line in production, while the
  transaction and its locks were held for the user think time. Interactions from the
  contexts that can process them without a user (background tasks, the scheduler, the
  external API) are unaffected.

Symptoms after the upgrade : an action that previously "worked" now fails with
`OTHER DATASESSION IN THE MIDDLE OF TRANSACTION IN THIS THREAD` or
`USER INTERACTION IN TRANSACTION`. In both cases the flow
was already broken - the changes were being lost, or the transaction was hanging on a
dialog - so the fix is to move the apply or the interaction out of the transaction.

When either is intended, it can be allowed for a single stack, taking the consequences
on knowingly :

```lsf
pushSetting('allowUserInteractionInTransaction', 'true');
// ... the action that interacts inside the transaction ...
popSetting('allowUserInteractionInTransaction');
```

`allowNestedTransaction` works the same way for the nesting. Both settings also work
globally, to buy time for the migration.


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


### Ordered aggregations over nullable columns use the index

Order-dependent group operators (`GROUP LAST` / `MAX` / `CONCAT`, ordered `PARTITION`)
whose `ORDER` expression is a nullable property now emit an `ORDER BY` whose null placement
matches the index built for that column on PostgreSQL, so the aggregation is served by an
index scan instead of a full scan of the grouped partition plus a sort
(see [issue #1727](https://github.com/lsfusion/platform/issues/1727)). Results are unchanged
(the aggregation already excludes null-ordered rows); only the plan changes, and existing
databases need no reindex. On large volumes this removes full-scan/spill behavior for such
aggregations. The change is result-safe and there is no fallback setting.
