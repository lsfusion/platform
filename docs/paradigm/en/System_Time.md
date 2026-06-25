---
slug: "/System_Time"
title: 'Time'
---

`Time` is a [system module](System_modules.md) that collects properties, actions, and classes for working with time: reading the current date and time, converting between time classes, extracting date parts, date and time arithmetic, intervals, and calendar classes (months, days of the week). It is pulled in via `REQUIRE Time` (`System` is pulled in automatically).

The signatures below use the built-in time classes `DATE`, `TIME`, `DATETIME` (without time zone) and `ZDATETIME` (with time zone), as well as `INTEGER` / `LONG` for counts of days, seconds, milliseconds, and so on. Most properties are thin wrappers over the matching PostgreSQL date/time functions; the underlying PostgreSQL expression is given in each row where the property maps directly to one (`$1`, `$2`, … are the arguments in the listed order). The rest are compositions of other `Time` properties, and the composition is named instead.

### Current value

| Property                                                  | What it returns                                                                                                                                            |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `currentDateTime[]`                                       | current `DATETIME` of the server clock (no time zone), truncated to whole seconds; PG `date_trunc('second', LOCALTIMESTAMP)`                                |
| `currentDateTime[INTEGER]`                                | same, keeping the given number of fractional-second digits (the argument is that precision); PG `LOCALTIMESTAMP($1)`                                        |
| `currentDateTimeMillis[]`                                 | current `DATETIME` truncated to milliseconds; PG `date_trunc('milliseconds', LOCALTIMESTAMP)`                                                               |
| `currentZDateTime[]`                                      | current `ZDATETIME` (with time zone), truncated to whole seconds; PG `date_trunc('second', CURRENT_TIMESTAMP)`                                              |
| `currentZDateTime[INTEGER]`                               | same, keeping the given number of fractional-second digits; PG `CURRENT_TIMESTAMP($1)`                                                                      |
| `currentDate[]`                                           | current `DATE`; a stored snapshot the platform refreshes no more than once a day, not a live clock read                                                     |
| `currentTime[]`                                           | current `TIME` of the server clock — the time part of `currentDateTime[]`                                                                                   |
| `currentTime[INTEGER]`                                    | current `TIME` keeping the given number of fractional-second digits; PG `LOCALTIME($1)`                                                                     |
| `currentDay[]` / `currentMonth[]` / `currentYear[]`       | day of month / month number / year of `currentDate[]` as `INTEGER` (`extractDay[DATE]` / `extractMonthNumber[DATE]` / `extractYear[DATE]` of `currentDate[]`) |
| `currentHour[]` / `currentMinute[]` / `currentSecond[]`   | hour / minute / second of `currentTime[]` as `INTEGER` (`extractHour[TIME]` / `extractMinute[TIME]` / `extractSecond[TIME]` of `currentTime[]`)             |
| `currentDateTimeSnapshot[]` / `currentZDateTimeSnapshot[]`| stored `DATETIME` / `ZDATETIME` snapshot of the current moment, written by the platform and read like an ordinary stored value                              |
| `currentTimeText[]`                                       | current date and time as `TEXT` in `YYYYMMDDHH24MISSMS` format; PG `to_char(now(), 'YYYYMMDDHH24MISSMS')`                                                    |
| `dateDiffersCurrent[DATE]`                                | takes a `DATE`; returns `TRUE` when it is a non-null date other than `currentDate[]`, otherwise `NULL`                                                      |

### Conversion

| Property                                          | What it does                                                                                                                                              |
|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `toDate[DATETIME]` / `toTime[DATETIME]`           | take a `DATETIME`, return its date part as `DATE` / its time part as `TIME`; built-in `DATE(...)` / `TIME(...)` class casts                                |
| `toDateTime[DATE]` / `toZDateTime[DATE]`          | take a `DATE`, return a `DATETIME` / `ZDATETIME` at midnight of that date; built-in `DATETIME(...)` / `ZDATETIME(...)` class casts                          |
| `toDateTime[DATE, TIME]`                          | combine a `DATE` and a `TIME` into a `DATETIME`; PG `$1 + $2` (date plus time)                                                                             |
| `toSeconds[TIME]`                                 | takes a `TIME`, returns seconds since midnight as `INTEGER`; PG `extract(epoch from $1)`                                                                   |
| `toTime[INTEGER]`                                 | takes a second count, returns the `TIME` that many seconds after midnight; PG `TIME '00:00' + INTERVAL '1 second' * $1` (counts of 86400 or more, or negative, wrap over the 24-hour clock) |
| `toDateTime[LONG]` / `toZDateTime[LONG]`          | take a Unix time in seconds (despite the parameter name `millis` in the source), return `DATETIME` / `ZDATETIME`; PG `to_timestamp(CAST($1 AS NUMERIC), 'UTC')::timestamp` / `to_timestamp(CAST($1 AS NUMERIC))` |
| `toDateFormat[STRING, STRING]`                    | parse a `DATE` from the first string by the format pattern in the second; PG `to_date($1, $2)`                                                             |
| `toDateTimeFormat[STRING, STRING]`                | parse a `DATETIME` from the first string by the format pattern in the second; PG `to_timestamp($1, $2)`                                                    |
| `toDateISO[DATE]`                                 | formats a `DATE` as a `'YYYY-MM-DD'` string; PG `to_char($1, 'YYYY-MM-DD')`                                                                                |
| `toDateDDMMYY[DATE]` / `toDateDDMMYYYY[DATE]`     | format a `DATE` as `'DD.MM.YY'` / `'DD.MM.YYYY'`; PG `to_char($1, 'DD.MM.YY')` / `to_char($1, 'DD.MM.YYYY')`                                                |
| `toMilliseconds[DATETIME]` → `resultMilliseconds[]` | a built-in Java action: takes a `DATETIME` and writes its Unix time in milliseconds (`LONG`) to the local property `resultMilliseconds[]`, which you read afterwards |
| `getMilliSeconds[ZDATETIME]` / `getSeconds[ZDATETIME]` | take a `ZDATETIME`, return Unix time in milliseconds / seconds as `LONG`; PG `extract(epoch from $1) * 1000`, with `getSeconds` dividing that by 1000   |

### Extracting parts

| Property                                                            | What it returns                                                                                                                                |
|---------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `extractYear[DATE]`                                                 | year of a `DATE` as `INTEGER`; PG `extract(year from $1)`                                                                                       |
| `extractMonthNumber[DATE]`                                          | month number 1–12 of a `DATE` as `INTEGER`; PG `extract(month from $1)`                                                                         |
| `extractDay[DATE]`                                                  | day of month 1–31 of a `DATE` as `INTEGER`; PG `extract(day from $1)`                                                                           |
| `extractDOY[DATE]`                                                  | day of year 1–366 of a `DATE` as `INTEGER`; PG `extract(doy from $1)`                                                                           |
| `extractHour[TIME]` / `extractMinute[TIME]` / `extractSecond[TIME]` | hour / minute / second of a `TIME` as `INTEGER`; PG `extract(hour from $1)` / `extract(minute from $1)` / `extract(second from $1)`             |
| `extractWeek[DATE]`                                                 | ISO-8601 week number of a `DATE` (week 1 is the one containing 4 January); PG `extract(week from $1)`                                           |
| `extractWeekZeroBased[DATE]`                                        | same as `extractWeek[DATE]`, but returns 0 for the first days of January that ISO-8601 still counts in the last week of the previous year (when `extractWeek[DATE] > 50` and the month is January) |
| `extractDOWNumber[DATE]`                                            | day-of-week number of a `DATE`, PostgreSQL style 0–6 with 0 = Sunday; PG `extract(dow from $1)`                                                 |
| `extractDOW[DATE]` / `extractDOWName[DATE]`                         | the day of the week of a `DATE` as a `DOW` object (`DOW[INTEGER]` of `extractDOWNumber[DATE]`) / its name as text (`name[DOW]` of that object)  |
| `extractMonth[DATE]` / `extractMonthName[DATE]`                     | the month of a `DATE` as a `Month` object (`month[INTEGER]` of `extractMonthNumber[DATE]`) / its name as text (`name[Month]` of that object)    |

### Arithmetic

| Property                                                  | What it does                                                                                                                                                |
|-----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `sum[DATE, INTEGER]`                                      | adds the given number of days to a `DATE`, returns a `DATE`; PG `$1 + $2`                                                                                    |
| `subtract[DATE, LONG]`                                    | subtracts the given number of days from a `DATE`, returns a `DATE`; PG `$1 - $2`                                                                             |
| `sumMonth[DATE, INTEGER]` / `sumYear[DATE, INTEGER]`      | add the given number of months / years to a `DATE`, return a `DATE`; PG `($1 + $2 * interval '1 month')::date` / `($1 + $2 * interval '1 year')::date`       |
| `firstDayOfMonth[DATE]` / `lastDayOfMonth[DATE]`          | first / last day of the month of a `DATE`, returns a `DATE`; PG `date_trunc('month', $1)::date` / `(date_trunc('month', $1) + interval '1 month - 1 day')::date` |
| `sumWeekFrom[DATE, INTEGER]` / `sumWeekTo[DATE, INTEGER]` | shift a `DATE` by `n` whole weeks, **without snapping to the start or end of a week**: `sumWeekFrom` adds `n*7` days (`sum[DATE, INTEGER]` of `n*7`), `sumWeekTo` adds `n*7 + 6` days. Both keep the original weekday — `sumWeekFrom(d, 0) = d` and `sumWeekTo(d, 0) = d + 6` — and always differ by exactly 6 days, so neither returns the Monday or Sunday of the week. For the actual start or end of a week, see the recipes below |
| `sumDay[DATETIME, LONG]` / `subtractDay[DATETIME, LONG]`  | add / subtract whole days on a `DATETIME` or `ZDATETIME` (overloaded for both), return the same class; PG `$1 + $2 * interval '1 day'` / `$1 - $2 * interval '1 day'` |
| `sumMinutes[…]`                                           | adds the given number of minutes; overloaded for `TIME`, `DATETIME`, `ZDATETIME`, returns the same class; PG `$1 + $2 * interval '1 minute'`. On `TIME` the result runs over a 24-hour clock and wraps past midnight (e.g. `23:30` + 60 → `00:30`, a negative count moves back past `00:00`); on `DATETIME` / `ZDATETIME` it rolls into the neighbouring day, moving the date part |
| `sumSeconds[…]`                                           | adds the given number of seconds; same overloads, and the same 24-hour wrap on `TIME` as `sumMinutes[…]`; PG `$1 + $2 * interval '1 second'`                 |
| `subtractSeconds[DATETIME, LONG]` / `subtractSeconds[ZDATETIME, LONG]` | subtract the given number of seconds from a `DATETIME` / `ZDATETIME`, return the same class; PG `$1 - $2 * interval '1 second'`                  |
| `subtractSeconds[TIME, TIME]` / `subtractSeconds[DATETIME, DATETIME]` / `subtractSeconds[ZDATETIME, ZDATETIME]` | difference of two same-class values in seconds as `INTEGER`, computed as the **second** argument minus the **first** (`subtractSeconds(a, b) = b - a`, positive when `b` is the later value); PG `extract(epoch from (b - a))` |
| `daysBetweenDates[DATE, DATE]`                            | difference of two dates in days as `INTEGER`, computed as the **first** argument minus the **second** (`daysBetweenDates(a, b) = a - b`, positive when `a` is the later date); PG `$1 - $2`. Note the operand order is the opposite of `subtractSeconds` |
| `daysInclBetweenDates[DATE, DATE]`                        | inclusive day count between two dates, counting both ends: the **second** argument minus the **first**, plus 1 (`daysInclBetweenDates(a, b) = b - a + 1`) — the opposite operand order from `daysBetweenDates[DATE, DATE]`, since it calls it with the arguments swapped |
| `secondsBetweenDates[DATETIME, DATETIME]`                 | difference of two moments in seconds as `INTEGER`, computed as the **first** argument minus the **second** (`secondsBetweenDates(a, b) = a - b`, positive when `a` is the later moment); PG `extract(epoch from $1) - extract(epoch from $2)` |
| `dateTimeToDateTime[DATE, TIME]`                          | combines a `DATE` and a `TIME` into a `DATETIME` through a text round-trip; PG `to_timestamp(<date> \|\| <time>, 'YYYY-MM-DDHH24:MI:SS.MS')::timestamp`        |
| `dateFromYearWeekDay[INTEGER, INTEGER, INTEGER]`          | builds a `DATE` from year, ISO-8601 week number, and ISO day of week (1 = Monday … 7 = Sunday); PG `to_date($1 \|\| ' ' \|\| $2 \|\| ' ' \|\| $3, 'IYYY IW ID')`     |
| `distanceDOWDOW[DOW, DOW]`                                | forward distance in days from the first day of week to the second, wrapping over the 0–6 week: `number[DOW]` of the second minus that of the first, plus 7 when that would be negative |

### Iteration and intervals

| Property                                          | What it does                                                                                                                                          |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `iterate[DATE, DATE, DATE]`                       | recursive enumeration of a date range: the first parameter ranges over every `DATE` from the second (`from`) up to the third (`to`) inclusive, stepping one day at a time via `sum[DATE, INTEGER]` |
| `iterate[DATE, INTERVAL[DATE]]`                   | the same enumeration driven by a date interval, taking its `from[INTERVAL[DATE]]` and `to[INTERVAL[DATE]]` bounds                                       |
| `interval[DATE, DATE]`                            | builds an `INTERVAL[DATE]` from a start and an end date, packing the two endpoints as Unix-millisecond epochs into one numeric value                    |
| `from[INTERVAL[DATE]]` / `to[INTERVAL[DATE]]`     | the start / end `DATE` unpacked from an `INTERVAL[DATE]`; PG `to_timestamp(...)` on the stored epoch                                                    |
| `interval[…]` / `from[…]` / `to[…]` for `TIME`, `DATETIME`, `ZDATETIME` | the same build-and-unpack for the matching `INTERVAL[TIME]`, `INTERVAL[DATETIME]`, `INTERVAL[ZDATETIME]` classes                   |

### Calendar classes and forms

`Month` and `DOW` are built-in static [classes](User_classes.md#static) describing months (12 objects) and days of the week (7 objects). They carry these properties:

| Property        | What it returns                                                                                              |
|-----------------|-------------------------------------------------------------------------------------------------------------|
| `number[Month]` | month number 1–12 (January = 1 … December = 12)                                                             |
| `name[Month]`   | the month's name as text                                                                                    |
| `number[DOW]`   | day-of-week number 0–6, PostgreSQL style with Sunday = 0 (Sunday = 0, Monday = 1, … Saturday = 6) — the same numbering as `extractDOWNumber[DATE]` |
| `numberM[DOW]`  | day-of-week number 0–6 counting from Monday (Monday = 0, Tuesday = 1, … Sunday = 6)                          |
| `name[DOW]`     | the day's name as text                                                                                      |

Lookup by number runs the other way: `month[INTEGER]`, `DOW[INTEGER]`. The forms `months` and `DOWs` show these classes as directories.

`DateTimePickerRanges` and `DateTimeIntervalPickerRanges` are static [classes](User_classes.md#static) with ready-made shortcut sets (`rangeToday`, `rangeYesterday`, `rangeLast7Days`, and so on) for picking a single date or a date interval in the UI.

### Recipes

The most common date calculations in schedules are compositions of the properties above. Day counts are whole days; `d` is any `DATE`.

```lsf
// today plus N days (N may be negative)
sum(currentDate(), N)

// Monday — the start of the week containing d
sum(d, -numberM(extractDOW(d)))

// Sunday — the end of that week
sum(d, 6 - numberM(extractDOW(d)))

// weekday of d: as a DOW object, as text, as a Monday-based number 0–6
extractDOW(d)
extractDOWName(d)
numberM(extractDOW(d))

// first and last day of d's month
firstDayOfMonth(d)
lastDayOfMonth(d)
```

### See also

- [`System modules`](System_modules.md) — the general list of platform modules.
- [`Built-in classes`](Built-in_classes.md) — description of the `DATE`, `TIME`, `DATETIME`, `ZDATETIME`, `INTERVAL[…]` classes.
- [`Scheduler`](Scheduler.md) — running actions on a schedule.
