---
title: 'Инструкция WHEN'
---

Инструкция `WHEN` - добавление обработчика [простого события](Simple_event.md).

### Синтаксис 

    WHEN eventClause eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;

### Описание

Инструкция `WHEN` добавляет обработчик простого события. В выражении условия можно неявно объявлять локальные параметры, которые затем могут быть использованы в обработчике события.

Также с помощью блока `ORDER` можно установить порядок, в котором будет вызываться обработчик для наборов объектов, для которых выполнилось условие простого события. 


:::info
Использование инструкции `WHEN` во многом аналогично следующей инструкции:

    ON eventClause FOR eventExpr [ORDER [DESC] orderExpr1, ..., orderExprN] DO eventAction;

но при этом имеет [ряд преимуществ](Simple_event.md).
:::

### Параметры

- `eventClause`

    [Блок описания события](Event_description_block.md). Описывает [базовое событие](Events.md) для создаваемого обработчика.

- `eventExpr`

    [Выражение](Expression.md), значение которого используется в качестве условия создаваемого простого события. Если полученное свойство не содержит внутри [оператора `PREV`](Previous_value_PREV.md), то платформа автоматически оборачивает его в [оператор `CHANGE`](Property_change_CHANGE.md).

- `eventAction`

    [Контекстно-зависимый оператор](Action_operators.md#contextdependent), описывающий действие, которое будет добавлено в качестве обработчика события.

- `DESC`

    Ключевое слово. Указывает на обратный порядок просмотра наборов объектов. 

- `orderExpr1, ..., orderExprM`

    Список выражений, определяющих порядок, в котором будут вызываться обработки для наборов объектов, для которых выполнилось условие события. Для определения порядка сначала используется значение первого выражения, затем при равенстве используется значение второго и т.д. 

### Примеры

```lsf
CLASS Stock;
name = DATA STRING[50] (Stock);

balance = DATA INTEGER (Sku, Stock);

// отправить email, когда остаток в результате применения изменений сессии стал меньше 0
WHEN balance(Sku s, Stock st) < 0 DO
      EMAIL SUBJECT 'Остаток стал отрицательным по товару ' + name(s) + ' на складе ' + name(st);

CLASS OrderDetail;
order = DATA Order (OrderDetail) NONULL DELETE;
discount = DATA NUMERIC[6,2] (OrderDetail);

WHEN LOCAL CHANGED(customer(Order o)) AND name(customer(o)) == 'Best customer' DO
    discount(OrderDetail d) <- 50 WHERE order(d) == o;
```

