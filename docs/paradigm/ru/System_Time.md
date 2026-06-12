---
slug: "/System_Time"
title: 'Time'
---

`Time` — [системный модуль](System_modules.md), собирающий свойства, действия и классы для работы со временем: получение текущей даты-времени, преобразования между классами времени, извлечение компонентов даты, арифметика над датами и временем, интервалы, календарные классы (месяцы, дни недели). Подключается через `REQUIRE Time` (`System` тянется автоматически).

В сигнатурах ниже используются встроенные классы времени `DATE`, `TIME`, `DATETIME` (без часового пояса) и `ZDATETIME` (с часовым поясом), а также `INTEGER` / `LONG` для счёта дней, секунд, миллисекунд и т. п. Большинство свойств — тонкие обёртки над соответствующими функциями даты-времени PostgreSQL; в строках, где свойство напрямую соответствует одной такой функции, приведено исходное выражение PostgreSQL (`$1`, `$2`, … — аргументы в перечисленном порядке). Остальные свойства — композиции других свойств `Time`, и тогда вместо выражения названа сама композиция.

### Текущее значение

| Свойство                                                  | Что возвращает                                                                                                                                              |
|-----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `currentDateTime[]`                                       | текущие `DATETIME` серверных часов (без часового пояса), округлённые до целых секунд; PG `date_trunc('second', LOCALTIMESTAMP)`                              |
| `currentDateTime[INTEGER]`                                | то же с сохранением указанного числа долей секунды (аргумент — эта точность); PG `LOCALTIMESTAMP($1)`                                                        |
| `currentDateTimeMillis[]`                                 | текущие `DATETIME`, округлённые до миллисекунды; PG `date_trunc('milliseconds', LOCALTIMESTAMP)`                                                             |
| `currentZDateTime[]`                                      | текущие `ZDATETIME` (с часовым поясом), округлённые до целых секунд; PG `date_trunc('second', CURRENT_TIMESTAMP)`                                            |
| `currentZDateTime[INTEGER]`                               | то же с сохранением указанного числа долей секунды; PG `CURRENT_TIMESTAMP($1)`                                                                               |
| `currentDate[]`                                           | текущая `DATE`; хранимый снимок, который платформа обновляет не чаще раза в сутки, а не прямое чтение часов                                                  |
| `currentTime[]`                                           | текущее `TIME` серверных часов — временная часть `currentDateTime[]`                                                                                         |
| `currentTime[INTEGER]`                                    | текущее `TIME` с сохранением указанного числа долей секунды; PG `LOCALTIME($1)`                                                                              |
| `currentDay[]` / `currentMonth[]` / `currentYear[]`       | день месяца / номер месяца / год от `currentDate[]` как `INTEGER` (`extractDay[DATE]` / `extractMonthNumber[DATE]` / `extractYear[DATE]` от `currentDate[]`)  |
| `currentHour[]` / `currentMinute[]` / `currentSecond[]`   | час / минута / секунда от `currentTime[]` как `INTEGER` (`extractHour[TIME]` / `extractMinute[TIME]` / `extractSecond[TIME]` от `currentTime[]`)             |
| `currentDateTimeSnapshot[]` / `currentZDateTimeSnapshot[]`| хранимый снимок текущего момента `DATETIME` / `ZDATETIME`, записываемый платформой и читаемый как обычное хранимое значение                                   |
| `currentTimeText[]`                                       | текущая дата-время как `TEXT` в формате `YYYYMMDDHH24MISSMS`; PG `to_char(now(), 'YYYYMMDDHH24MISSMS')`                                                       |
| `dateDiffersCurrent[DATE]`                                | принимает `DATE`; возвращает `TRUE`, когда это непустая дата, отличная от `currentDate[]`, иначе `NULL`                                                       |

### Преобразование

| Свойство                                          | Что делает                                                                                                                                                  |
|---------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `toDate[DATETIME]` / `toTime[DATETIME]`           | принимают `DATETIME`, возвращают его дату как `DATE` / его время как `TIME`; встроенные приведения класса `DATE(...)` / `TIME(...)`                          |
| `toDateTime[DATE]` / `toZDateTime[DATE]`          | принимают `DATE`, возвращают `DATETIME` / `ZDATETIME` на полночь этой даты; встроенные приведения класса `DATETIME(...)` / `ZDATETIME(...)`                  |
| `toDateTime[DATE, TIME]`                          | собирает `DATETIME` из `DATE` и `TIME`; PG `$1 + $2` (дата плюс время)                                                                                       |
| `toSeconds[TIME]`                                 | принимает `TIME`, возвращает число секунд от полуночи как `INTEGER`; PG `extract(epoch from $1)`                                                             |
| `toTime[INTEGER]`                                 | принимает счёт секунд, возвращает `TIME` через столько секунд после полуночи; PG `TIME '00:00' + INTERVAL '1 second' * $1` (счёт от 86400 и больше, а также отрицательный, заворачивается по 24-часовому циферблату) |
| `toDateTime[LONG]` / `toZDateTime[LONG]`          | принимают Unix-время в секундах (несмотря на имя параметра `millis` в исходнике), возвращают `DATETIME` / `ZDATETIME`; PG `to_timestamp(CAST($1 AS NUMERIC), 'UTC')::timestamp` / `to_timestamp(CAST($1 AS NUMERIC))` |
| `toDateFormat[STRING, STRING]`                    | разбирает `DATE` из первой строки по шаблону формата во второй; PG `to_date($1, $2)`                                                                         |
| `toDateTimeFormat[STRING, STRING]`                | разбирает `DATETIME` из первой строки по шаблону формата во второй; PG `to_timestamp($1, $2)`                                                                |
| `toDateISO[DATE]`                                 | форматирует `DATE` как строку `'YYYY-MM-DD'`; PG `to_char($1, 'YYYY-MM-DD')`                                                                                 |
| `toDateDDMMYY[DATE]` / `toDateDDMMYYYY[DATE]`     | форматируют `DATE` как `'DD.MM.YY'` / `'DD.MM.YYYY'`; PG `to_char($1, 'DD.MM.YY')` / `to_char($1, 'DD.MM.YYYY')`                                              |
| `toMilliseconds[DATETIME]` → `resultMilliseconds[]`| встроенное java-действие: принимает `DATETIME` и записывает его Unix-время в миллисекундах (`LONG`) в локальное свойство `resultMilliseconds[]`, которое читается затем |
| `getMilliSeconds[ZDATETIME]` / `getSeconds[ZDATETIME]` | принимают `ZDATETIME`, возвращают Unix-время в миллисекундах / секундах как `LONG`; PG `extract(epoch from $1) * 1000`, причём `getSeconds` делит это на 1000 |

### Извлечение компонентов

| Свойство                                                            | Что возвращает                                                                                                                                  |
|---------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `extractYear[DATE]`                                                 | год от `DATE` как `INTEGER`; PG `extract(year from $1)`                                                                                         |
| `extractMonthNumber[DATE]`                                          | номер месяца 1–12 от `DATE` как `INTEGER`; PG `extract(month from $1)`                                                                          |
| `extractDay[DATE]`                                                  | день месяца 1–31 от `DATE` как `INTEGER`; PG `extract(day from $1)`                                                                             |
| `extractDOY[DATE]`                                                  | день года 1–366 от `DATE` как `INTEGER`; PG `extract(doy from $1)`                                                                              |
| `extractHour[TIME]` / `extractMinute[TIME]` / `extractSecond[TIME]` | час / минута / секунда от `TIME` как `INTEGER`; PG `extract(hour from $1)` / `extract(minute from $1)` / `extract(second from $1)`              |
| `extractWeek[DATE]`                                                 | номер недели по ISO 8601 от `DATE` (неделя 1 — та, что содержит 4 января); PG `extract(week from $1)`                                           |
| `extractWeekZeroBased[DATE]`                                        | то же, что `extractWeek[DATE]`, но возвращает 0 для первых дней января, которые ISO 8601 относит ещё к последней неделе предыдущего года (когда `extractWeek[DATE] > 50` и месяц — январь) |
| `extractDOWNumber[DATE]`                                            | номер дня недели от `DATE` в стиле PostgreSQL 0–6, где 0 = воскресенье; PG `extract(dow from $1)`                                               |
| `extractDOW[DATE]` / `extractDOWName[DATE]`                         | день недели от `DATE` как объект `DOW` (`DOW[INTEGER]` от `extractDOWNumber[DATE]`) / его название как текст (`name[DOW]` этого объекта)         |
| `extractMonth[DATE]` / `extractMonthName[DATE]`                     | месяц от `DATE` как объект `Month` (`month[INTEGER]` от `extractMonthNumber[DATE]`) / его название как текст (`name[Month]` этого объекта)       |

### Арифметика

| Свойство                                                  | Что делает                                                                                                                                                  |
|-----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `sum[DATE, INTEGER]`                                      | прибавляет к `DATE` указанное число дней, возвращает `DATE`; PG `$1 + $2`                                                                                    |
| `subtract[DATE, LONG]`                                    | вычитает из `DATE` указанное число дней, возвращает `DATE`; PG `$1 - $2`                                                                                     |
| `sumMonth[DATE, INTEGER]` / `sumYear[DATE, INTEGER]`      | прибавляют к `DATE` указанное число месяцев / лет, возвращают `DATE`; PG `($1 + $2 * interval '1 month')::date` / `($1 + $2 * interval '1 year')::date`      |
| `firstDayOfMonth[DATE]` / `lastDayOfMonth[DATE]`          | первый / последний день месяца от `DATE`, возвращают `DATE`; PG `date_trunc('month', $1)::date` / `(date_trunc('month', $1) + interval '1 month - 1 day')::date` |
| `sumWeekFrom[DATE, INTEGER]` / `sumWeekTo[DATE, INTEGER]` | сдвигают `DATE` на указанное число полных недель, попадая на начало (`sum[DATE, INTEGER]` на `n*7` дней) / конец (`n*7 + 6` дней) этой недели                 |
| `sumDay[DATETIME, LONG]` / `subtractDay[DATETIME, LONG]`  | прибавляют / вычитают целые дни к `DATETIME` или `ZDATETIME` (перегружены для обоих), возвращают тот же класс; PG `$1 + $2 * interval '1 day'` / `$1 - $2 * interval '1 day'` |
| `sumMinutes[…]`                                           | прибавляет указанное число минут; перегружено для `TIME`, `DATETIME`, `ZDATETIME`, возвращает тот же класс; PG `$1 + $2 * interval '1 minute'`. Для `TIME` результат ведётся по 24-часовому циферблату и заворачивается через полночь (например, `23:30` + 60 → `00:30`, а отрицательный счёт уводит назад за `00:00`); для `DATETIME` / `ZDATETIME` переходит в соседние сутки, сдвигая дату |
| `sumSeconds[…]`                                           | прибавляет указанное число секунд; те же перегрузки и тот же заворот по 24 часам для `TIME`, что и у `sumMinutes[…]`; PG `$1 + $2 * interval '1 second'`      |
| `subtractSeconds[DATETIME, LONG]` / `subtractSeconds[ZDATETIME, LONG]` | вычитают указанное число секунд из `DATETIME` / `ZDATETIME`, возвращают тот же класс; PG `$1 - $2 * interval '1 second'`                         |
| `subtractSeconds[TIME, TIME]` / `subtractSeconds[DATETIME, DATETIME]` / `subtractSeconds[ZDATETIME, ZDATETIME]` | разность двух значений одного класса в секундах как `INTEGER`, считается как **второй** аргумент минус **первый** (`subtractSeconds(a, b) = b - a`, положительно, когда `b` — более поздняя величина); PG `extract(epoch from (b - a))` |
| `daysBetweenDates[DATE, DATE]`                            | разность двух дат в днях как `INTEGER`, считается как **первый** аргумент минус **второй** (`daysBetweenDates(a, b) = a - b`, положительно, когда `a` — более поздняя дата); PG `$1 - $2`. Порядок операндов обратен `subtractSeconds` |
| `daysInclBetweenDates[DATE, DATE]`                        | число дней между двумя датами с учётом обоих концов: **второй** аргумент минус **первый**, плюс 1 (`daysInclBetweenDates(a, b) = b - a + 1`) — порядок операндов обратен `daysBetweenDates[DATE, DATE]`, так как оно вызывается с переставленными аргументами |
| `secondsBetweenDates[DATETIME, DATETIME]`                 | разность двух моментов в секундах как `INTEGER`, считается как **первый** аргумент минус **второй** (`secondsBetweenDates(a, b) = a - b`, положительно, когда `a` — более поздний момент); PG `extract(epoch from $1) - extract(epoch from $2)` |
| `dateTimeToDateTime[DATE, TIME]`                          | собирает `DATETIME` из `DATE` и `TIME` через приведение текста; PG `to_timestamp(<дата> || <время>, 'YYYY-MM-DDHH24:MI:SS.MS')::timestamp`                    |
| `dateFromYearWeekDay[INTEGER, INTEGER, INTEGER]`          | собирает `DATE` по году, номеру недели ISO 8601 и дню недели ISO (1 = понедельник … 7 = воскресенье); PG `to_date($1 || ' ' || $2 || ' ' || $3, 'IYYY IW ID')` |
| `distanceDOWDOW[DOW, DOW]`                                | расстояние вперёд в днях от первого дня недели до второго, с заворотом по неделе 0–6: `number[DOW]` второго минус `number[DOW]` первого, плюс 7, когда результат был бы отрицательным |

### Итерация и интервалы

| Свойство                                          | Что делает                                                                                                                                            |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `iterate[DATE, DATE, DATE]`                       | рекурсивная развёртка диапазона дат: первый параметр пробегает все `DATE` от второго (`from`) до третьего (`to`) включительно, шагая по одному дню через `sum[DATE, INTEGER]` |
| `iterate[DATE, INTERVAL[DATE]]`                   | та же развёртка по интервалу дат, по его границам `from[INTERVAL[DATE]]` и `to[INTERVAL[DATE]]`                                                          |
| `interval[DATE, DATE]`                            | собирает `INTERVAL[DATE]` из начальной и конечной даты, упаковывая обе границы как Unix-эпохи в миллисекундах в одно числовое значение                   |
| `from[INTERVAL[DATE]]` / `to[INTERVAL[DATE]]`     | начальная / конечная `DATE`, распакованная из `INTERVAL[DATE]`; PG `to_timestamp(...)` над хранимой эпохой                                               |
| `interval[…]` / `from[…]` / `to[…]` для `TIME`, `DATETIME`, `ZDATETIME` | та же упаковка и распаковка для соответствующих классов `INTERVAL[TIME]`, `INTERVAL[DATETIME]`, `INTERVAL[ZDATETIME]`               |

### Календарные классы и формы

`Month` и `DOW` — встроенные [классы](User_classes.md#static), описывающие месяцы (12 объектов) и дни недели (7 объектов). Содержат свойства `number[Month]`, `name[Month]`, `number[DOW]`, `numberM[DOW]` (нумерация с понедельника), `name[DOW]`. Поиск по номеру: `month[INTEGER]`, `DOW[INTEGER]`. Формы `months` и `DOWs` показывают эти классы как справочники.

`DateTimePickerRanges` и `DateTimeIntervalPickerRanges` — статические [классы](User_classes.md#static) с готовыми наборами шорткатов (`rangeToday`, `rangeYesterday`, `rangeLast7Days` и т. п.) для выбора в UI-пикерах одиночной даты и интервала.

### Связано

- [`System modules`](System_modules.md) — общий список модулей платформы.
- [`Built-in classes`](Built-in_classes.md) — описание классов `DATE`, `TIME`, `DATETIME`, `ZDATETIME`, `INTERVAL[…]`.
- [`Scheduler`](Scheduler.md) — запуск действий по расписанию.
