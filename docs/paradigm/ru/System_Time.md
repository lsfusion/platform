---
slug: "/System_Time"
title: 'Time'
---

`Time` — [системный модуль](System_modules.md), собирающий свойства, действия и классы для работы со временем: получение текущей даты-времени, преобразования между классами времени, извлечение компонентов даты, арифметика над датами и временем, интервалы, календарные классы (месяцы, дни недели). Подключается через `REQUIRE Time` (`System` тянется автоматически).

В сигнатурах ниже используются встроенные классы времени `DATE`, `TIME`, `DATETIME` (без часового пояса) и `ZDATETIME` (с часовым поясом), а также `INTEGER` / `LONG` для счёта дней, секунд, миллисекунд и т. п.

### Текущее значение

| Свойство                              | Что возвращает                                                          |
|---------------------------------------|--------------------------------------------------------------------------|
| `currentDateTime[]`                   | текущие `DATETIME` (без часового пояса), округлённые до секунды          |
| `currentDateTime[INTEGER]`            | то же с точностью до указанного числа долей секунды                      |
| `currentDateTimeMillis[]`             | текущие `DATETIME`, округлённые до миллисекунды                          |
| `currentZDateTime[]` / `[INTEGER]`    | текущие `ZDATETIME` (с часовым поясом), аналогично                       |
| `currentDate[]`                       | текущая `DATE`; снимок, обновляется не чаще раза в сутки                 |
| `currentTime[]` / `[INTEGER]`         | текущее `TIME` локального сервера                                        |
| `currentDay[]` / `currentMonth[]` / `currentYear[]` | отдельные компоненты `currentDate`                           |
| `currentHour[]` / `currentMinute[]` / `currentSecond[]` | отдельные компоненты `currentTime`                       |
| `currentDateTimeSnapshot[]` / `currentZDateTimeSnapshot[]` | хранимый снимок текущей даты-времени                  |
| `currentTimeText[]`                   | текущая дата-время как `TEXT` в формате `YYYYMMDDHH24MISSMS`             |
| `dateDiffersCurrent[DATE]`            | признак, что дата отлична от текущей                                     |

### Преобразование

| Свойство                                          | Что делает                                                          |
|---------------------------------------------------|----------------------------------------------------------------------|
| `toDate[DATETIME]` / `toTime[DATETIME]`           | разбор `DATETIME` на дату и время                                    |
| `toDateTime[DATE]` / `toDateTime[DATE, TIME]`     | сборка `DATETIME` из даты (и опционально времени)                    |
| `toZDateTime[DATE]` / `toDateTime[LONG]` / `toZDateTime[LONG]` | приведение между `DATE`, `LONG` Unix-секунд (несмотря на имя параметра `millis` в исходнике) и `DATETIME` / `ZDATETIME` |
| `toSeconds[TIME]` / `toTime[INTEGER]`             | взаимный перевод `TIME` и числа секунд                               |
| `toDateFormat[STRING, STRING]`                    | разбор `DATE` из строки по указанному формату                        |
| `toDateTimeFormat[STRING, STRING]`                | разбор `DATETIME` из строки по указанному формату                    |
| `toDateISO[DATE]`                                 | форматирование в `'YYYY-MM-DD'`                                      |
| `toDateDDMMYY[DATE]` / `toDateDDMMYYYY[DATE]`     | форматирование в `'DD.MM.YY'` / `'DD.MM.YYYY'`                       |
| `toMilliseconds[DATETIME]` → `resultMilliseconds[]`| перевод `DATETIME` в миллисекунды (запись в локальное свойство)     |
| `getMilliSeconds[ZDATETIME]` / `getSeconds[ZDATETIME]` | UNIX-время в миллисекундах / секундах                          |

### Извлечение компонентов

| Свойство                                          | Что возвращает                                                       |
|---------------------------------------------------|-----------------------------------------------------------------------|
| `extractYear[DATE]` / `extractMonthNumber[DATE]` / `extractDay[DATE]` / `extractDOY[DATE]` | компоненты даты в виде `INTEGER` |
| `extractHour[TIME]` / `extractMinute[TIME]` / `extractSecond[TIME]` | компоненты времени                                |
| `extractWeek[DATE]` / `extractWeekZeroBased[DATE]` | номер недели по ISO 8601 (последний с обработкой первых дней года)   |
| `extractDOWNumber[DATE]`                          | номер дня недели в формате PG (0 = воскресенье)                       |
| `extractDOW[DATE]` / `extractDOWName[DATE]`       | день недели как объект `DOW` / его название                           |
| `extractMonth[DATE]` / `extractMonthName[DATE]`   | месяц как объект `Month` / его название                               |

### Арифметика

| Свойство                                                  | Что делает                                                |
|-----------------------------------------------------------|------------------------------------------------------------|
| `sum[DATE, INTEGER]`                                      | прибавить N дней к дате                                    |
| `sumMonth[DATE, INTEGER]` / `sumYear[DATE, INTEGER]`      | прибавить N месяцев / лет                                  |
| `subtract[DATE, LONG]`                                    | вычесть N дней из даты                                     |
| `sumDay[DATETIME, LONG]` / `subtractDay[DATETIME, LONG]`  | прибавить / вычесть дни от `DATETIME`                      |
| `sumMinutes[…]` / `sumSeconds[…]`                         | прибавить минуты / секунды; перегружены для `TIME`, `DATETIME`, `ZDATETIME` |
| `subtractSeconds[…]`                                      | вычесть секунды или вернуть разницу в секундах между двумя `TIME`, `DATETIME`, `ZDATETIME` |
| `firstDayOfMonth[DATE]` / `lastDayOfMonth[DATE]`          | первый / последний день месяца                             |
| `sumWeekFrom[DATE, INTEGER]` / `sumWeekTo[DATE, INTEGER]` | смещение на N полных недель от начала / до конца           |
| `daysBetweenDates[DATE, DATE]` / `daysInclBetweenDates[DATE, DATE]` | число дней между датами (с включением и без)      |
| `secondsBetweenDates[DATETIME, DATETIME]`                 | число секунд между двумя моментами                         |
| `dateTimeToDateTime[DATE, TIME]`                          | сборка `DATETIME` через приведение текста                  |
| `dateFromYearWeekDay[INTEGER, INTEGER, INTEGER]`          | сборка даты по году, номеру ISO-недели и дню недели        |
| `distanceDOWDOW[DOW, DOW]`                                | расстояние от одного дня недели до другого вперёд          |

### Итерация и интервалы

| Свойство                                          | Что делает                                                                                                |
|---------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| `iterate[DATE, DATE, DATE]`                       | рекурсивная развёртка диапазона дат: даёт все даты от `from` до `to` включительно                          |
| `iterate[DATE, INTERVAL[DATE]]`                   | то же по интервалу дат                                                                                     |
| `interval[DATE, DATE]` / `from[INTERVAL[DATE]]` / `to[INTERVAL[DATE]]` | сборка интервала из пары дат и обратный разбор                                  |
| `interval[…]` / `from[…]` / `to[…]`               | то же для `TIME`, `DATETIME`, `ZDATETIME` через соответствующие классы `INTERVAL[…]`                       |

### Календарные классы и формы

`Month` и `DOW` — встроенные [классы](User_classes.md#static), описывающие месяцы (12 объектов) и дни недели (7 объектов). Содержат свойства `number[Month]`, `name[Month]`, `number[DOW]`, `numberM[DOW]` (нумерация с понедельника), `name[DOW]`. Поиск по номеру: `month[INTEGER]`, `DOW[INTEGER]`. Формы `months` и `DOWs` показывают эти классы как справочники.

`DateTimePickerRanges` и `DateTimeIntervalPickerRanges` — статические [классы](User_classes.md#static) с готовыми наборами шорткатов (`rangeToday`, `rangeYesterday`, `rangeLast7Days` и т. п.) для выбора в UI-пикерах одиночной даты и интервала.

### Связано

- [`System modules`](System_modules.md) — общий список модулей платформы.
- [`Built-in classes`](Built-in_classes.md) — описание классов `DATE`, `TIME`, `DATETIME`, `ZDATETIME`, `INTERVAL[…]`.
- [`Scheduler`](Scheduler.md) — запуск действий по расписанию.
