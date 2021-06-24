---
title: 'How-to: Пространства имен'
---

Иногда возникают ситуации, когда необходимо использовать одинаковое [имя](Naming.md) для разных элементов системы. Для этой цели их можно отнести к разным пространствам имен, которое задается для модуля через [инструкцию `NAMESPACE`](Module_header.md). По умолчанию, пространство имен совпадает с именем модуля.

Создадим два модуля `UseCaseNamePurchase` и `UseCaseNameSale`, которые объявляют логику заказов на закупку и продажу:

```lsf
MODULE UseCaseNamePurchase;

NAMESPACE Purchase;

CLASS Order 'Заказ';
date 'Дата' = DATA DATE (Order);
number 'Номер' = DATA INTEGER (Order);

FORM orders 'Заказы на закупку'
    OBJECTS o = Order
    PROPERTIES(o) date, number
;

NAVIGATOR {
    NEW orders;
}
```

```lsf
MODULE UseCaseNameSale;

NAMESPACE Sale;

CLASS Order 'Заказ';
date 'Дата' = DATA DATE (Order);
number 'Номер' = DATA INTEGER (Order);

FORM orders 'Заказы на продажу'
    OBJECTS o = Order
    PROPERTIES(o) date, number
;

NAVIGATOR {
    NEW orders;
}
```

В обоих из них объявлен класс `Order`, но так как у модулей разные пространства имен, то у первого класса оно будет `Purchase`, а у второго `Sale`.

Объявим тестовый модуль с пространством имен `Test`, который будет одновременно зависеть и от первого, и от второго модуля:

```lsf
REQUIRE UseCaseNamePurchase, UseCaseNameSale;

NAMESPACE Test;

messagePurchaseCount 'Вывести кол-во заказов на закупку' {
    MESSAGE GROUP SUM 1 IF o IS Purchase.Order;
}
messageSaleCount 'Вывести кол-во заказов на продажу' {
    MESSAGE GROUP SUM 1 IF o IS Sale.Order;
}

NAVIGATOR {
    NEW ACTION messagePurchaseCount;
    NEW ACTION messageSaleCount;
}
```

При попытке обратиться к классу `Order` без явного указания пространства имен будет выдана ошибка:

![](images/How-to_Namespaces.png)

Все такие обращения требуют явного указания пространства имен.

В случае, если пространство имен модуля совпадает с пространством искомого элемента системы (например, `Purchase`)

```lsf
NAMESPACE Purchase;
```

или указан приоритет пространств имен через [инструкцию `PRIORITY`](Module_header.md)

```lsf
PRIORITY Purchase;
```

то можно пространство имен не указывать

```lsf
messagePurchaseCount 'Вывести кол-во заказов на закупку' {
    MESSAGE GROUP SUM 1 IF o IS Order;
}
messageSaleCount 'Вывести кол-во заказов на продажу' {
    MESSAGE GROUP SUM 1 IF o IS Sale.Order;
}

NAVIGATOR {
    NEW ACTION messagePurchaseCount;
    NEW ACTION messageSaleCount;
}
```

Без указания пространства имен будет использоваться класс из `Purchase`. При этом остается возможность в явную указать пространство имен (например, `Sale`).
