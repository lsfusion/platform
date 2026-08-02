# Migration notes

## 7.0

### A window's `CUSTOM` accepts more, a `DESIGN` container's is checked, and the client must match

A React component can now draw the window the forms open in and the window the messages
appear in, and a `DESIGN` container's `custom` is read by the same rules a window's has
always been read by. Three things an upgrading application can notice:

- **the client and the server must be upgraded together**. The API version went to 392
  because every window now carries what draws it, so a client of the previous version
  refuses to connect. There is no setting for this : deploy the pair;
- **a form the server used to build can now be refused**, with the message naming the
  container. Three combinations are checked when the form is built : a `custom` literal
  that is neither a React component name nor markup (a misspelled component name, which
  the browser used to draw as its own text), `tabbed` together with `custom` (the client
  drew one of the two and ignored the other, and with a `custom` property it failed in
  the browser), and a React component paired with a `custom` property. The fix is to
  write what was meant : a component name, markup, or one of `tabbed` / `custom`;
- **`WINDOW ... CUSTOM 'literal' CUSTOM property()` written in ONE statement keeps both
  halves**. It used to drop the literal, so a window drawn from a template started empty
  until the property computed its first value ; it now shows the literal from the start,
  as two statements always did. A window written this way with a COMPONENT literal beside
  a property is refused at load, because a component draws the window itself.

A template a property computes now reports a place that names nothing, in the place and
in the browser console. That changes what such a window or container shows - a message
where there used to be a blank spot - but not whether the application starts.

### A window's `CUSTOM` accepts more, a `DESIGN` container's is checked, and the client must match

A React component can now draw the window the forms open in and the window the messages
appear in, and a `DESIGN` container's `custom` is read by the same rules a window's has
always been read by. Three things an upgrading application can notice:

- **the client and the server must be upgraded together**. The API version went to 392
  because every window now carries what draws it, so a client of the previous version
  refuses to connect. There is no setting for this : deploy the pair;
- **a form the server used to build can now be refused**, with the message naming the
  container. Three combinations are checked when the form is built : a `custom` literal
  that is neither a React component name nor markup (a misspelled component name, which
  the browser used to draw as its own text), `tabbed` together with `custom` (the client
  drew one of the two and ignored the other, and with a `custom` property it failed in
  the browser), and a React component paired with a `custom` property. The fix is to
  write what was meant : a component name, markup, or one of `tabbed` / `custom`;
- **`WINDOW ... CUSTOM 'literal' CUSTOM property()` written in ONE statement keeps both
  halves**. It used to drop the literal, so a window drawn from a template started empty
  until the property computed its first value ; it now shows the literal from the start,
  as two statements always did. A window written this way with a COMPONENT literal beside
  a property is refused at load, because a component draws the window itself.

A template a property computes now reports a place that names nothing, in the place and
in the browser console. That changes what such a window or container shows - a message
where there used to be a blank spot - but not whether the application starts.

### Another session and a user interaction inside a transaction now fail

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
`USER INTERACTION IN TRANSACTION`. In both cases the flow was already broken - the
changes were being lost, or the transaction was hanging on a dialog - so the fix is to
move the work or the interaction out of the transaction.

Expect more of these than the flows you know about. Both situations used to be asserts,
that is log lines in production, so they went unnoticed for as long as they existed; and
the first one is now reported for ANY operation of the other session - a read, a write,
a temp table - not only for its apply, which is simply the point where the damage used
to become visible.

When either is intended, it can be allowed for a single stack, taking the consequences
on knowingly :

```lsf
pushSetting('allowUserInteractionInTransaction', 'true');
// ... the action that interacts inside the transaction ...
popSetting('allowUserInteractionInTransaction');
```

`allowNestedTransaction` works the same way for the other session's work. Both settings
also work globally : setting them restores the previous behavior (a log line instead of
the error), which is the quickest way back if the upgrade breaks a production flow -
the places to fix are then in the assert log, under the same messages.


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
