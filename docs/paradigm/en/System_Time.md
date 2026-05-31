---
slug: "/System_Time"
title: 'Time'
---

`Time` is a [system module](System_modules.md) that collects properties, actions, and classes for working with time: reading the current date and time, converting between time classes, extracting date parts, date and time arithmetic, intervals, and calendar classes (months, days of the week). It is pulled in via `REQUIRE Time` (`System` is pulled in automatically).

The signatures below use the built-in time classes `DATE`, `TIME`, `DATETIME` (without time zone) and `ZDATETIME` (with time zone), as well as `INTEGER` / `LONG` for counts of days, seconds, milliseconds, and so on.

### Current value

| Property                              | What it returns                                                          |
|---------------------------------------|---------------------------------------------------------------------------|
| `currentDateTime[]`                   | current `DATETIME` (no time zone), truncated to seconds                   |
| `currentDateTime[INTEGER]`            | same with the given number of fractional-second digits                    |
| `currentDateTimeMillis[]`             | current `DATETIME` truncated to milliseconds                              |
| `currentZDateTime[]` / `[INTEGER]`    | current `ZDATETIME` (with time zone), similarly                           |
| `currentDate[]`                       | current `DATE`; a snapshot refreshed no more than once a day              |
| `currentTime[]` / `[INTEGER]`         | current `TIME` of the server's local clock                                |
| `currentDay[]` / `currentMonth[]` / `currentYear[]` | individual components of `currentDate`                       |
| `currentHour[]` / `currentMinute[]` / `currentSecond[]` | individual components of `currentTime`                   |
| `currentDateTimeSnapshot[]` / `currentZDateTimeSnapshot[]` | stored snapshot of the current date and time          |
| `currentTimeText[]`                   | the current date and time as `TEXT` in `YYYYMMDDHH24MISSMS` format        |
| `dateDiffersCurrent[DATE]`            | a flag set when the date is not the current one                           |

### Conversion

| Property                                          | What it does                                                         |
|---------------------------------------------------|----------------------------------------------------------------------|
| `toDate[DATETIME]` / `toTime[DATETIME]`           | splitting `DATETIME` into date and time                              |
| `toDateTime[DATE]` / `toDateTime[DATE, TIME]`     | assembling `DATETIME` from a date (and an optional time)             |
| `toZDateTime[DATE]` / `toDateTime[LONG]` / `toZDateTime[LONG]` | conversion between `DATE`, `LONG` Unix seconds (despite the parameter name `millis` in the source), and `DATETIME` / `ZDATETIME` |
| `toSeconds[TIME]` / `toTime[INTEGER]`             | converting `TIME` to/from a second count                             |
| `toDateFormat[STRING, STRING]`                    | parsing `DATE` from a string by the given format                     |
| `toDateTimeFormat[STRING, STRING]`                | parsing `DATETIME` from a string by the given format                 |
| `toDateISO[DATE]`                                 | formatting as `'YYYY-MM-DD'`                                         |
| `toDateDDMMYY[DATE]` / `toDateDDMMYYYY[DATE]`     | formatting as `'DD.MM.YY'` / `'DD.MM.YYYY'`                          |
| `toMilliseconds[DATETIME]` → `resultMilliseconds[]` | converts `DATETIME` to milliseconds, written to a local property   |
| `getMilliSeconds[ZDATETIME]` / `getSeconds[ZDATETIME]` | UNIX time in milliseconds / seconds                             |

### Extracting parts

| Property                                          | What it returns                                                      |
|---------------------------------------------------|-----------------------------------------------------------------------|
| `extractYear[DATE]` / `extractMonthNumber[DATE]` / `extractDay[DATE]` / `extractDOY[DATE]` | date components as `INTEGER` |
| `extractHour[TIME]` / `extractMinute[TIME]` / `extractSecond[TIME]` | time components                                   |
| `extractWeek[DATE]` / `extractWeekZeroBased[DATE]` | ISO-8601 week number (the latter handles the first days of the year) |
| `extractDOWNumber[DATE]`                          | day-of-week number in PG style (0 = Sunday)                          |
| `extractDOW[DATE]` / `extractDOWName[DATE]`       | the day of the week as a `DOW` object / its name                     |
| `extractMonth[DATE]` / `extractMonthName[DATE]`   | the month as a `Month` object / its name                             |

### Arithmetic

| Property                                                  | What it does                                                |
|-----------------------------------------------------------|--------------------------------------------------------------|
| `sum[DATE, INTEGER]`                                      | adds N days to a date                                        |
| `sumMonth[DATE, INTEGER]` / `sumYear[DATE, INTEGER]`      | adds N months / years                                        |
| `subtract[DATE, LONG]`                                    | subtracts N days from a date                                 |
| `sumDay[DATETIME, LONG]` / `subtractDay[DATETIME, LONG]`  | adds / subtracts days on `DATETIME`                          |
| `sumMinutes[…]` / `sumSeconds[…]`                         | adds minutes / seconds; overloaded for `TIME`, `DATETIME`, `ZDATETIME` |
| `subtractSeconds[…]`                                      | subtracts seconds, or returns the difference in seconds between two `TIME`, `DATETIME`, or `ZDATETIME` values |
| `firstDayOfMonth[DATE]` / `lastDayOfMonth[DATE]`          | first / last day of the month                                |
| `sumWeekFrom[DATE, INTEGER]` / `sumWeekTo[DATE, INTEGER]` | offset by N full weeks to the start / to the end             |
| `daysBetweenDates[DATE, DATE]` / `daysInclBetweenDates[DATE, DATE]` | number of days between dates (with and without inclusion) |
| `secondsBetweenDates[DATETIME, DATETIME]`                 | number of seconds between two points in time                 |
| `dateTimeToDateTime[DATE, TIME]`                          | assembles `DATETIME` via a text round-trip                   |
| `dateFromYearWeekDay[INTEGER, INTEGER, INTEGER]`          | builds a date from year, ISO week number, and day of week    |
| `distanceDOWDOW[DOW, DOW]`                                | forward distance from one day of week to another             |

### Iteration and intervals

| Property                                          | What it does                                                                                              |
|---------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| `iterate[DATE, DATE, DATE]`                       | recursive enumeration of a date range — gives every date from `from` to `to` inclusive                     |
| `iterate[DATE, INTERVAL[DATE]]`                   | same, by a date interval                                                                                   |
| `interval[DATE, DATE]` / `from[INTERVAL[DATE]]` / `to[INTERVAL[DATE]]` | building an interval from a pair of dates and the reverse                          |
| `interval[…]` / `from[…]` / `to[…]`               | same for `TIME`, `DATETIME`, `ZDATETIME` via the corresponding `INTERVAL[…]` classes                       |

### Calendar classes and forms

`Month` and `DOW` are built-in static [classes](User_classes.md#static) describing months (12 objects) and days of the week (7 objects). They carry the properties `number[Month]`, `name[Month]`, `number[DOW]`, `numberM[DOW]` (numbering from Monday), `name[DOW]`. Lookup by number: `month[INTEGER]`, `DOW[INTEGER]`. The forms `months` and `DOWs` show these classes as directories.

`DateTimePickerRanges` and `DateTimeIntervalPickerRanges` are static [classes](User_classes.md#static) with ready-made shortcut sets (`rangeToday`, `rangeYesterday`, `rangeLast7Days`, and so on) for picking a single date or a date interval in the UI.

### See also

- [`System modules`](System_modules.md) — the general list of platform modules.
- [`Built-in classes`](Built-in_classes.md) — description of the `DATE`, `TIME`, `DATETIME`, `ZDATETIME`, `INTERVAL[…]` classes.
- [`Scheduler`](Scheduler.md) — running actions on a schedule.
