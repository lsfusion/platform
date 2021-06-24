---
title: 'How-to: FORMULA'
---

## Пример 1

### Условие

Задан список заказов.

```lsf
CLASS Order 'Заказ';
date 'Дата' = DATA DATE (Order);
number 'Номер' = DATA STRING[30] (Order);

FORM orders 'Заказы на закупку'
    OBJECTS o = Order
    PROPERTIES(o) date, number, NEW, DELETE
;

NAVIGATOR {
    NEW orders;
}
```

Необходимо экспортировать в CSV этот список, причем дату экспортировать в формате ISO `YYYY-MM-DD`.

### Решение

```lsf
toISO = FORMULA STRING[10] 'to_char($1,\'YYYY-MM-DD\')';

exportToCSV 'Экспорт в CSV' () {
    LOCAL file = FILE ();
    EXPORT CSV FROM toISO(date(Order o)), number(o) TO file;
    open(file());
}

EXTEND FORM orders
    PROPERTIES() exportToCSV
;
```

Для решения задачи создаем свойство при помощи [оператора `FORMULA`](FORMULA_operator.md), которое будет принимать на вход дату и возвращать значение в виде строки в формате `YYYY-MM-DD`. В выражении формулы используется стандартная функция PostgreSQL [`to_char`](https://www.postgresql.org/docs/11/functions-formatting.html).

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1). Также добавлены строки заказов с параметрами количество и сумма.

```lsf
CLASS OrderDetail;
order = DATA Order (OrderDetail) NONULL DELETE;

quantity 'Кол-во' = DATA NUMERIC[14,3] (OrderDetail);
sum 'Сумма' = DATA NUMERIC[14,2] (OrderDetail);

EXTEND FORM orders
    OBJECTS d = OrderDetail
    PROPERTIES(d) quantity, sum, NEW, DELETE
    FILTERS order(d) = o
;
```

Нужно выгрузить по одному заказу CSV-файл с его строками, в котором количества и суммы будут отформатированы до 3х и 2х знаков соответственно. Кроме того, нужно чтобы числа разбивались по триадам.

### Решение

```lsf
toString = FORMULA TEXT 'to_char($1,$2)';

exportToCSV 'Экспорт в CSV' (Order o) {
    LOCAL file = FILE ();
    EXPORT CSV FROM toISO(date(o)), number(o), toString(quantity(OrderDetail d), '999 999.999'), toString(sum(d), '999 999.99') WHERE order(d) = o TO file;
    open(file());
}

EXTEND FORM orders
    PROPERTIES(o) exportToCSV
;
```

Создаем свойство `toString`, которое принимает на вход два параметра (число и формат) и возвращает значение типа `TEXT`. При выгрузке передаем нужный формат в качестве второго параметра.

## Пример 3

### Условие

Аналогично [**Примеру 2**](#пример-2).

Нужно сделать колонку, которая будет отмечена, если в номере заказа находятся одни цифры.

### Решение

```lsf
onlyDigits = FORMULA NULL BOOLEAN 'CASE WHEN trim($1) ~ \'^[0-9]*$\' THEN 1 ELSE NULL END';

EXTEND FORM orders
    PROPERTIES 'Только цифры' = onlyDigits(number(o))
;
```

Так как внутри формулы используются одинарные кавычки, то их требуется [экранировать](https://ru.wikipedia.org/wiki/%D0%AD%D0%BA%D1%80%D0%B0%D0%BD%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5_%D1%81%D0%B8%D0%BC%D0%B2%D0%BE%D0%BB%D0%BE%D0%B2) при помощи обратного слэша `\`.

Важно заметить, что в платформе тип `BOOLEAN` имеет 2 значения : `TRUE` и `NULL`. Поэтому нельзя просто написать логическое выражение, а необходимо отрицательное значение преобразовывать в `NULL`. Кроме того, платформа должна в явную знать, что выражение может возвращать неопределенное значение. Для этого используется соответствующий маркер после ключевого слова `FORMULA`.

На уровне базы данных тип `BOOLEAN` хранится как числовое значение (`1` или `null`), поэтому свойства этого типа должны возвращать числовое значение. Разработчик сам несет ответственность за то, чтобы возвращаемый тип выражения соответствовал указанному типу. В случае каких-либо расхождение поведение будет непредсказуемым (чаще всего будут происходить ошибки выполнения запросов).

Следует помнить ,что если на вход любому из свойств, построенных оператором `FORMULA`, передается `NULL`, то результатом всегда будет также `NULL`.

  
