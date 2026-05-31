---
slug: "/System_Utils"
title: 'Utils'
---

`Utils` — [системный модуль](System_modules.md), собирающий часто нужные вспомогательные свойства и действия: работа с файловой системой, преобразование и кодирование данных, строковые операции, арифметика, доступ к JSON, полнотекстовый поиск, диагностика сервера и т. п. Подключается через `REQUIRE Utils` (`System` и `Time` тянутся автоматически).

### Файловая система

Действия `(ISTRING path, ...)` работают на сервере по умолчанию; флаг `isClient = TRUE` (или отдельная клиентская перегрузка) выполняет ту же операцию на стороне клиента.

| Действие / свойство                                       | Что делает                                                                |
|-----------------------------------------------------------|----------------------------------------------------------------------------|
| `listFiles[ISTRING, BOOLEAN, BOOLEAN]` и перегрузки       | перечисляет файлы в директории; пишет имя, признак директории, дату, размер в локальные свойства `fileName`, `fileIsDirectory`, `fileModifiedDateTime`, `fileSize` от `INTEGER` |
| `listFilesClient[…]`                                      | то же на клиенте                                                           |
| `fileExists[ISTRING]` / `fileExistsClient[ISTRING]`       | проверка существования; результат в `fileExists[]`                         |
| `mkdir[ISTRING]` / `mkdirClient[ISTRING]`                 | создать директорию                                                         |
| `delete[ISTRING]` / `deleteClient[ISTRING]`               | удалить файл или директорию                                                |
| `copy[ISTRING, ISTRING]` / `copyClient[…]`                | скопировать                                                                |
| `move[ISTRING, ISTRING]` / `moveClient[…]`                | переместить                                                                |
| `getFileSize[FILE]`                                       | размер файла в байтах в `fileSize[]`                                       |
| `appendToFile[STRING, TEXT, STRING]`                      | дописать строку в файл с указанной кодировкой                              |

### Содержимое файлов и кодировки

| Свойство / действие                                       | Что делает                                                                 |
|-----------------------------------------------------------|-----------------------------------------------------------------------------|
| `stringToFile[TEXT, STRING, STRING]` → `resultFile[]`     | сериализовать строку в `FILE` с указанной кодировкой и расширением         |
| `fileToString[FILE, STRING]` → `resultString[]`           | прочитать `FILE` как строку                                                |
| `linkToString[LINK]` / `richTextToString[RICHTEXT]`       | приведение ссылочного / форматированного текста к `STRING`                 |
| `readResource[STRING, BOOLEAN]` → `resource[]`            | прочитать ресурс из classpath                                              |
| `readResourcePaths[STRING, BOOLEAN]` → `resourcePaths`    | список ресурсов, попадающих под шаблон                                     |
| `readProperties[RAWFILE]` → `properties[STRING]`          | разобрать `.properties`-файл в свойство-словарь                            |
| `encode[…, STRING]` / `decode[STRING, STRING]`            | кодирование / декодирование произвольной кодировкой                        |
| `encodeBase64[…]` / `encodeBase64Unchunked[…]` / `decodeBase64[STRING]` | base64 для `RAWFILE`, `STRING`, `FILE`, `NAMEDFILE`             |
| `urlEncode[TEXT, TEXT]` → `urlEncoded[]` / `urlDecode[…]` → `urlDecoded[]` | URL-кодирование с указанной кодировкой                            |
| `urlParse[]` / `urlFormat[]` (через `urlFormatted` и `urlParsed`) | разбор URL на составляющие и обратная сборка                         |
| `escapeJSONValue[TEXT]` / `escapeXMLValue[TEXT]`          | экранирование строки для встраивания в JSON / XML                          |

### Архивы

| Действие                          | Что делает                                                                  |
|-----------------------------------|------------------------------------------------------------------------------|
| `zipping[STRING] <- file`         | накапливание файлов в архиве (`STRING`-путь внутри архива → содержимое)     |
| `makeZipFile[BOOLEAN]` → `zipped[]`| собрать архив; флаг — обнулять ли время файлов                              |
| `unzipping[] <- file`             | задать архив для распаковки                                                  |
| `makeUnzipFile[]` → `unzipped[STRING]` | распаковать; результат: `STRING`-путь внутри архива → файл              |

### Консоль и сеть

| Действие                                              | Что делает                                                              |
|-------------------------------------------------------|--------------------------------------------------------------------------|
| `cmd[TEXT, TEXT, BOOLEAN, BOOLEAN]` и перегрузки      | запуск команды ОС; результат в `cmdOut[]` и `cmdErr[]`                   |
| `cmdClient[TEXT, BOOLEAN]` / `cmdClient[TEXT]`        | то же на клиенте                                                         |
| `ping[TEXT, BOOLEAN]` / `pingClient[TEXT]`            | проверка доступности хоста; ошибка в `pingError[]`                       |

### Excel

| Свойство / действие                                       | Что делает                                                              |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `protectExcel[EXCELFILE, STRING]` → `protectedExcel[]`    | защита файла паролем                                                    |
| `mergeExcel[EXCELFILE, EXCELFILE]` → `mergedExcel[]`      | объединение двух файлов                                                 |
| `columnsCount[EXCELFILE, INTEGER]` → `columnsCount[]`     | число колонок на листе                                                  |
| `sheetNames[EXCELFILE]` → `sheetNames[INTEGER]`           | имена листов                                                            |

### PDF и Word

| Свойство / действие                                       | Что делает                                                              |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `pagesCountPdf[PDFFILE]` → `pagesCountPdf[]`              | число страниц                                                           |
| `pdfToString[PDFFILE, BOOLEAN]`                           | распознать текст из PDF                                                 |
| `wordToPdf[WORDFILE]`                                     | сконвертировать Word в PDF                                              |

### Строки

| Свойство                                                  | Что делает                                                              |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `length[TEXT]`                                            | длина строки                                                            |
| `left[TEXT, INTEGER]` / `right[TEXT, INTEGER]`            | первые / последние N символов                                           |
| `substr[TEXT, INTEGER, INTEGER]` / `substrFrom[TEXT, INTEGER]` | подстрока с позиции / от позиции до конца                          |
| `substring[STRING, STRING]`                               | подстрока, найденная регулярным выражением                              |
| `strpos[TEXT, TEXT]`                                      | позиция подстроки                                                        |
| `replace[TEXT, TEXT, TEXT]`                               | замена всех вхождений                                                   |
| `ltrim[…]` / `rtrim[…]` / `trim[TEXT]`                    | срезание пробелов слева / справа / с обеих сторон                       |
| `lpad[TEXT, INTEGER, TEXT]` / `rpad[…]`                   | дополнение до заданной длины                                            |
| `repeat[TEXT, INTEGER]`                                   | повторение N раз                                                        |
| `startsWith[…]` / `istartsWith[…]` / `endsWith[…]`        | проверка начала / конца строки (с учётом регистра и без)                |
| `isSubstring[…]` / `isISubstring[…]`                      | вхождение подстроки (с учётом регистра и без)                           |
| `isWordInCSV[…]`                                          | вхождение значения в строку-CSV                                          |
| `getWord[TEXT, TEXT, INTEGER]` / `wordCount[…]`           | разбор строки по разделителю                                            |
| `splitPart[STRING, STRING, INTEGER]`                      | N-й фрагмент по разделителю                                             |
| `regexpReplace[STRING, STRING, STRING, STRING]`           | замена по регулярному выражению                                          |
| `regexPatternMatch[TEXT, STRING]`                         | проверка соответствия регулярному выражению                              |
| `onlyDigits[TEXT]`                                        | признак, что строка состоит только из цифр                              |
| `array[STRING, STRING, INTEGER]`                          | разворачивает строку с разделителем в построчное множество значений     |

Для генерации идентификаторов и буфера обмена: `generateUUID[]` → `generatedUUID[]`, `generatePassword[INTEGER, BOOLEAN, BOOLEAN]` → `generatedPassword[]`, `copyToClipboard[TEXT]`, а также cookie: `getCookie[STRING]` → `cookie[]`, `setCookie[STRING, STRING, JSON]`.

### Числа

| Свойство                                                  | Что делает                                                              |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `abs[…]` / `delta[…, …]`                                  | модуль / модуль разности                                                |
| `min[…, …]` / `max[…, …]`                                 | минимум / максимум двух значений                                         |
| `floor[…]` / `floor[…, …]`                                | округление вниз; вторая форма — до кратного заданного шага              |
| `ceil[…]` / `ceil[…, …]`                                  | округление вверх; вторая форма — до кратного шага                       |
| `round[…, …]` и `roundM1` / `round0` … `round6`           | округление до N знаков (включая отрицательные)                          |
| `trunc[…, …]`                                             | усечение                                                                |
| `mod[…, …]` / `divideInteger[…, …]` / `divideIntegerNeg` / `divideIntegerRnd` | остаток и варианты целочисленного деления                |
| `sqr[…]` / `sqrt[…]` / `power[…, …]`                      | квадрат, корень, степень                                                |
| `ln[…]` / `exp[…]`                                        | натуральный логарифм / экспонента                                       |
| `percent[…, …]` / `share[…, …]`                           | процент от суммы / доля от целого в процентах                           |
| `bitwiseAnd[…, …]` / `bitwiseOr[…, …]` / `bitwiseNot[…]`  | побитовые операции                                                      |
| `toInteger[…]` / `toNumeric[…]` / `toNumericNull[…]`      | строгое и nullable приведение к числам                                  |
| `toChar[…, …]`                                            | форматирование значения по строке формата (PostgreSQL `to_char`)        |

### Итерация

`iterate[INTEGER, INTEGER, INTEGER]` — рекурсивная развёртка диапазона целых от `from` до `to`. `count[INTEGER, INTEGER]` — частный случай: `iterate(i, 1, count)`. См. аналогичный `iterate` для дат в [`Time`](System_Time.md).

### Цвет

`colorToHexString[COLOR]` — представление цвета в виде `'#RRGGBB'`.

### Полнотекстовый поиск

| Свойство                                                  | Что делает                                                              |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `toTsVector[STRING, STRING]` / `toTsVector[STRING]`       | построение `TSVECTOR` с указанным словарём или `english`                |
| `toTsQuery[STRING, STRING]` / `toTsQuery[STRING]`         | построение `TSQUERY`                                                    |
| `setWeight[TSVECTOR, STRING]`                             | пометка частей вектора весом (`'A'`–`'D'`)                              |
| `tsRank[…]` / `tsRankCD[…]` / `tsRankLN[…]`               | рейтинг релевантности с разными моделями                                |
| `numNode[TSQUERY]`                                        | число узлов в разобранном запросе                                       |

### Свойства доступа к JSON {#json-access}

Этот раздел собирает обёртки над PostgreSQL-функциями `jsonb_*`, читающие значения из существующего JSON. Дополняют оператор `JSON`, который, наоборот, строит JSON из свойств или формы. Все объявлены через оператор `FORMULA`.

| Свойство                                          | Что возвращает                                                                                  |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `field[JSON, STRING]`                             | значение по ключу как `JSON`                                                                     |
| `field[JSON, STRING, STRING]`                     | значение по пути из двух ключей как `JSON`                                                       |
| `field[JSON, STRING, STRING, STRING]`             | значение по пути из трёх ключей как `JSON`                                                       |
| `fieldText[JSON, STRING]` / `[…, STRING, STRING]` / `[…, STRING, STRING, STRING]` | то же, что `field`, но как `STRING`           |
| `array[JSON, INTEGER]`                            | элемент массива по индексу как `JSON`; параметр-индекс — это и итерационная переменная           |
| `arrayText[JSON, INTEGER]`                        | то же, что `array`, но как `STRING`                                                              |
| `arrayElement[JSON, INTEGER]`                     | элемент массива по 1-based индексу как сырая строка (jsonb → text)                               |
| `map[JSON, STRING]`                               | значение для каждой пары ключ-значение JSON-объекта как `JSON`; параметр-ключ — итерационная переменная |
| `mapText[JSON, STRING]`                           | то же, что `map`, но как `STRING`                                                                |
| `merge[JSON, JSON]`                               | рекурсивное слияние двух JSON                                                                    |
| `canonicalizeJSON[JSONFILE, BOOLEAN]` / `canonicalizeJSON[JSONFILE]` → `canonicalizedJSON[]` | каноническая нормализация JSON-файла; короткая форма эквивалентна `canonicalizeJSON(file, TRUE)` (закодировать unicode-символы) |

`field[JSON, STRING ...]` и `fieldText[JSON, STRING ...]` — скалярные обёртки над `jsonb_extract_path` и `jsonb_extract_path_text`. Возвращают значение по указанному пути из одного, двух или трёх ключей. Различаются только классом результата: `field` отдаёт `JSON` для дальнейшей композиции (например, передать результат в `array` или в следующий `field`), `fieldText` сразу даёт `STRING`. Для путей глубже трёх ключей используется композиция `field` от `field`.

`array[JSON, INTEGER]` и `arrayText[JSON, INTEGER]` — табличные обёртки над `jsonb_array_elements` и `jsonb_array_elements_text`. Параметр `INTEGER row` не упоминается в SQL-тексте, поэтому платформа сама проставляет в него номер строки через `ROW_NUMBER() OVER ()` — специальный shortcut для не-параметризованного `INTEGER`-параметра с именем `row`. На практике параметр играет двойную роль: индекс для прямого обращения (`array(j, 1)` — первый элемент) и итерационная переменная в выражениях вроде `array(j, INTEGER o)`, где `o` объявлен на месте и пробегает все индексы массива.

`map[JSON, STRING]` и `mapText[JSON, STRING]` — табличные обёртки над `jsonb_each` и `jsonb_each_text`, отдающие пары `(ключ, значение)` JSON-объекта. Параметр `STRING key` не упомянут в SQL-тексте и потому становится колонкой ключа результата; имя `key` совпадает с именем колонки в результате `jsonb_each`.

`arrayElement[JSON, INTEGER]` — отдельный шорткат для прямого доступа по индексу. Реализован через PG-оператор `->`, на входе ожидает 1-based индекс и сам сдвигает его к 0-based. Возвращает `STRING` — текстовое представление jsonb. Полезен для логирования и отладки, когда нужен сырой вид элемента, а не разобранное значение.

### Локальные файловые свойства

Удобные пустые `LOCAL`-свойства для каждого типового файлового класса: `file[]`, `wordFile[]`, `imageFile[]`, `pdfFile[]`, `dbfFile[]`, `rawFile[]`, `excelFile[]`, `textFile[]`, `csvFile[]`, `htmlFile[]`, `jsonFile[]`, `xmlFile[]`, `tableFile[]`. Подходят как промежуточные буферы для импорта / экспорта без объявления собственных `LOCAL`.

### Журналирование

`printToLog[TEXT, STRING, STRING]` и его перегрузки пишут сообщение в лог сервера. Параметры — текст сообщения, имя логгера (по умолчанию `'system'`) и уровень (по умолчанию `'info'`).

### Диалоговые формы

`dialogString`, `dialogDate`, `dialogInteger`, `dialogNumeric` — готовые однообъектные формы для запроса у пользователя значения соответствующего класса. Используются как `DIALOG dialog<...> OBJECTS ... INPUT DO { ... }`.

### Среда выполнения

`serverAvailableProcessors[]`, `serverFreeMemoryMB[]`, `serverTotalMemoryMB[]`, `serverMaxMemoryMB[]` — заполняются действием `readServerMemory[]`. Показывают параметры JVM сервера: количество доступных процессоров и три характеристики кучи.

### Язык

- [Оператор `JSON`](../language/JSON_operator.md) — построение JSON-значения из свойств или формы.
- [Оператор `FORMULA`](../language/FORMULA_operator.md) — синтаксис, через который объявлены большинство свойств модуля.
- [Оператор `IMPORT`](../language/IMPORT_operator.md) — чтение JSON-файла в форму или через `FIELDS … DO`.

### Связано

- [`System modules`](System_modules.md) — общий список модулей платформы.
- [`Custom formula (FORMULA)`](Custom_formula_FORMULA.md) — механизм, через который объявлены большинство свойств модуля.
- [`Time`](System_Time.md) — отдельный модуль работы со временем.
