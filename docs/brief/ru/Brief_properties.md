---
slug: "/Brief_properties"
title: 'Brief: свойства'
---

## Выражения и композиция

[Свойство](../paradigm/Properties.md) принимает набор объектов-параметров и возвращает ровно одно значение — как чистая функция, но вычисляемая сразу на всей базе, как колонка SQL-запроса. Значение либо хранится ([первичное свойство](../paradigm/Data_properties_DATA.md), [оператор `DATA`](../language/DATA_operator.md)), либо вычисляется выражением: арифметика, логика (`AND`, `OR`, `NOT`), сравнения, операции со строками, проверка и приведение класса (`IS`, `AS`). Функции строк, чисел и дат (`lpad`, `substr`, `mod`, `currentDate`) — не операторы, а свойства модулей `Utils` и `Time`.

Подстановка свойства в выражение другого свойства — [композиция](../paradigm/Composition_JOIN.md); [оператор `JOIN`](../language/JOIN_operator.md) записывает ее явно. Список создающих свойства операторов — [Операторы](../paradigm/Property_operators_paradigm.md).

```lsf
price = DATA NUMERIC[14,2] (Item);
vat = DATA NUMERIC[6,2] (Item);
priceWithVAT (Item i) = price(i) * (1 + vat(i) / 100);
```

## Группировка (GROUP)

[Оператор `GROUP`](../language/GROUP_operator.md) разбивает все наборы объектов на группы и вычисляет для каждой одну агрегирующую функцию — `SUM`, `MAX`, `MIN`, `CONCAT`, `LAST`, `EQUAL`, `AGGR` / `NAGGR`, `CUSTOM` (агрегат СУБД):

```
GROUP
type [expr1, ..., exprN]
[orderClause]
[TOP topExpr] [OFFSET offsetExpr]
[WHERE whereExpr]
[BY groupExpr1, ..., groupExprM]
```

Отдельной функции подсчета нет: количество считается как `GROUP SUM 1`. **Аналогия**: `GROUP BY` в SQL, только результат — самостоятельное свойство, а не часть запроса. Механизм — какие наборы объектов попадают в группу, какими получаются параметры создаваемого свойства и где важен порядок — описан в [группировке](../paradigm/Grouping_GROUP.md).

```lsf
sold (Sku s) = GROUP SUM quantity(OrderDetail d) BY sku(d);
```

## Разбиение и сортировка (PARTITION)

[Оператор `PARTITION`](../language/PARTITION_operator.md) тоже разбивает наборы объектов на группы блоком `BY`, но возвращает результат не на группу, а на каждый набор объектов — по окну внутри его группы, заданному порядком `ORDER`. Агрегирующие функции здесь — `SUM`, `PREV`, `LAST`, `CUSTOM`: отсюда места и ранги, сквозная нумерация, накопительные итоги, значение предыдущего или последнего набора в окне. **Аналогия**: оконные функции SQL (`OVER (PARTITION BY ... ORDER BY ...)`). Что именно попадает в окно, описано в [разбиении / упорядочивании](../paradigm/Partitioning_sorting_PARTITION_..._ORDER.md).

Форма `PARTITION UNGROUP` решает обратную задачу — [распределяет](../paradigm/Distribution_UNGROUP.md) значение свойства-источника по наборам объектов группы: пропорционально заданному выражению (`PROPORTION`) или по порядку (`LIMIT`).

```lsf
place (Team t) = PARTITION SUM 1 ORDER DESC points(t), t BY conference(t);
```

## Агрегация объектов (GROUP AGGR, AGGR)

Эти операторы работают не со значениями, а с объектами.

`GROUP AGGR` — форма [оператора `GROUP`](../language/GROUP_operator.md), возвращающая сам объект группы. Получается отображение, обратное перечисленным в `BY` свойствам, — например, поиск объекта по коду; платформа добавляет ограничение, что в группе такой объект не более одного.

[Оператор `AGGR`](../language/AGGR_operator.md) идет дальше: он сам создает объект, когда агрегируемое выражение становится не `NULL`, и удаляет, когда оно снова `NULL`, заполняя свойства-отображения на параметры. Механизм — [агрегации](../paradigm/Aggregations.md).

```lsf
countryName = GROUP AGGR Country c BY name(c);
shipment (Invoice i) = AGGR ShipmentInvoice WHERE createShipment(i);
```

## Выбор и переопределение (CASE, IF, OVERRIDE)

[Оператор выбора](../paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) проверяет условия по порядку и возвращает результат первого выполненного; условие выполнено, если его значение не `NULL`.

- [`CASE`](../language/CASE_operator.md) — явные пары `WHEN ... THEN ...` и необязательный `ELSE`.
- [`IF`](../language/IF_operator.md) — постфиксная одиночная форма `result IF condition`; [`IF ... THEN`](../language/IF_..._THEN_operator.md) добавляет блок `ELSE`.
- [`OVERRIDE`](../language/OVERRIDE_operator.md) — первый операнд, не равный `NULL`; так же подставляется значение по умолчанию вместо `NULL`.
- [`EXCLUSIVE`](../language/EXCLUSIVE_operator.md) — то же плюс декларация, что не `NULL` не более одного операнда.
- [`MULTI`](../language/MULTI_operator.md) — операнд выбирается по совместимости классов аргументов с его сигнатурой.

```lsf
signedQuantity (Ledger l) = MULTI quantity[InLedger](l), quantity[OutLedger](l);
price (Item i) = OVERRIDE salePrice(i), basePrice(i), 0;
```

## Рекурсия (RECURSION)

[Оператор `RECURSION`](../language/RECURSION_operator.md) создает свойство, вычисляемое итерациями; к нему обращаются на деревьях, графах и транзитивных замыканиях — уровень узла, все предки объекта, достижимость по цепочке ссылок. Его части — `STEP`, префикс `$` перед параметром и опция `CYCLES` — и то, как считаются итерации, описаны в [рекурсии](../paradigm/Recursion_RECURSION.md).

```lsf
level (Group child, Group parent) = RECURSION 1 IF child IS Group AND parent = child
                                              STEP 1 IF parent = parent($parent);
```

## Абстрактные свойства (ABSTRACT)

[Оператор `ABSTRACT`](../language/ABSTRACT_operator.md) объявляет свойство без реализации: базовый модуль задает класс значения и классы параметров, а другие модули добавляют реализации [инструкцией `+=`](../language/plus_equals_statement.md). Из них платформа собирает оператор выбора — это [расширение свойств](../paradigm/Property_extension.md), способ снять зависимость между модулями и получить полиморфизм свойств.

Какая реализация выбирается и что требуется от реализаций, задается опциями — `MULTI`, `CASE`, `VALUE`, `EXCLUSIVE`, `OVERRIDE`, `FULL` — и описано в [расширении свойств](../paradigm/Property_extension.md).

```lsf
name 'Наименование' = ABSTRACT ISTRING[250] (Document);      // базовый модуль
name (Shipment s) += ISTRING[250]('Поставка ' + number(s));  // модуль поставок
```
