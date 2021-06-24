---
title: 'How-to: Регистры'
---

### Регистр накоплений

Предположим необходимо реализовать логику по расчет остатков SKU на складах.

```lsf
REQUIRE Utils;

CLASS SKU 'SKU';
CLASS Stock 'Склад';
```

Теоретически, можно просто сделать [свойство](Properties.md), которое будет складывать все приходные операции, затем вычитать все расходные, при этом все операции будут задаваться в явную. Недостаток такого подхода в том, что при добавлении новой операции нужно будет добавлять ее в формулу расчета остатка. Кроме того, тяжело будет построить форму со списком всех операций, изменяющих остаток по конкретному SKU и складу. Также будет нарушена [модульность](Modularity.md), поскольку модуль, в котором будет объявлено свойство остаток, должен зависеть от всех модулей с операциями, который на него влияют.

Для реализации эффективной [расширяемости](Extensions.md) системы такой функционал лучше всего реализовывать при помощи *регистров*. Для этого вводится абстрактный класс `SKULedger`, один экземпляр которого будет отражать единичное изменение остатка по одному SKU и одному складу на определенное количество (положительное или отрицательное). Для него задаются абстрактные свойства, которые должны быть заданы при реализации класса.


:::info
Все регистры могут иметь произвольное количество и тип измерений, относительно которых они действуют. В данном примере измерениями являются SKU и Склад.
:::

```lsf
CLASS ABSTRACT SKULedger 'Регистр изменения остатка товара';

posted 'Проведен' = ABSTRACT BOOLEAN (SKULedger);
dateTime 'Дата/время' = ABSTRACT DATETIME (SKULedger);

sku 'SKU' = ABSTRACT SKU (SKULedger);
stock 'Склад' = ABSTRACT Stock (SKULedger);

quantity 'Кол-во' = ABSTRACT NUMERIC[14,2] (SKULedger);

balance 'Остаток' = GROUP SUM quantity(SKULedger l) IF posted(l) BY stock(l), sku(l);

balance 'Остаток на дату/время' = GROUP SUM quantity(SKULedger l) IF posted(l) AND dateTime(l) <= DATETIME dt BY stock(l), sku(l), dateTime(l);
```

Текущий остаток и остаток на определенное время рассчитываются только исходя из свойств класса `SKULedger` без привязки к конкретным операциям. Этот код можно и нужно объявить в отдельном модуле. Модули с конкретными операциями будут его использовать и расширять этот класс.

Например, рассмотрим одну из таких операций *Поступление на склад*.

```lsf
CLASS Receipt 'Поступление на склад';
posted 'Проведен' = DATA BOOLEAN (Receipt);
dateTime 'Дата/время' = DATA DATETIME (Receipt);

stock 'Склад' = DATA Stock (Receipt);

CLASS ReceiptDetail 'Строка поступления на склад';
receipt 'Поступление' = DATA Receipt (ReceiptDetail) NONULL DELETE;

sku 'SKU' = DATA SKU (ReceiptDetail);

quantity 'Кол-во' = DATA NUMERIC[14,2] (ReceiptDetail);
price 'Цена' = DATA NUMERIC[14,2] (ReceiptDetail);
```

Для того, чтобы "провести" ее по регистру, нужно [расширить класс](Class_extension.md) `SKULedger` классом строки поступления на склад `ReceiptDetail`. Также необходимо [расширить свойства](Property_extension.md) регистра.

```lsf
EXTEND CLASS ReceiptDetail : SKULedger;

// необходимо указывать [SKULedger], так как ReceiptDetail также наследует PriceLedger в этом же примере и платформе надо знать, какое именно свойство надо реализовать
posted[SKULedger](ReceiptDetail d) += posted(receipt(d));
dateTime[SKULedger](ReceiptDetail d) += dateTime(receipt(d));

stock[SKULedger](ReceiptDetail d) += stock(receipt(d));

sku[SKULedger](ReceiptDetail d) += sku(d);
quantity[SKULedger](ReceiptDetail d) += quantity(d);
```

Рассмотрим более сложный случай, когда есть документ перемещения со склада на склад.

```lsf
CLASS Transfer 'Перемещение со склада на склад';
posted 'Проведен' = DATA BOOLEAN (Transfer);
dateTime 'Дата/время' = DATA DATETIME (Transfer);

fromStock 'Склад (откуда)' = DATA Stock (Transfer);
toStock 'Склад (куда)' = DATA Stock (Transfer);

CLASS TransferDetail 'Строка отгрузки со склада';
transfer 'Поступление' = DATA Transfer (TransferDetail) NONULL DELETE;

sku 'SKU' = DATA SKU (TransferDetail);

quantity 'Кол-во' = DATA NUMERIC[14,2] (TransferDetail);
price 'Цена' = DATA NUMERIC[14,2] (TransferDetail);
```

В этом случае, строки документа нужно "проводить" по регистру дважды. По аналогии с поступлением проведем строку по регистру как расходную операцию с отрицательным количеством.

```lsf
EXTEND CLASS TransferDetail : SKULedger;

posted(TransferDetail d) += posted(transfer(d));
dateTime(TransferDetail d) += dateTime(transfer(d));

stock(TransferDetail d) += fromStock(transfer(d));

sku(TransferDetail d) += sku(d);
quantity(TransferDetail d) += -quantity(d);
```

Для того, чтобы провести его по регистру для склада куда перемещается товар, воспользуемся [агрегацией](Aggregations.md) объектов. Строка документа перемещения будет генерировать объект класса, который в свою очередь будет "проводиться" по регистру.

```lsf
CLASS TransferSKULedger 'Перемещение на склад (регистр)' : SKULedger;
transferSKULedger = AGGR TransferSKULedger WHERE posted(TransferDetail transferDetail);

posted(TransferSKULedger d) += d IS TransferSKULedger;
dateTime(TransferSKULedger d) += dateTime(transfer(transferDetail(d)));

stock(TransferSKULedger d) += toStock(transfer(transferDetail(d)));

sku(TransferSKULedger d) += sku(transferDetail(d));
quantity(TransferSKULedger d) += quantity(transferDetail(d));
```

Объект регистра будет создаваться только в том случае, когда документ перемещения проведен. Соответственно, в таком случае свойство `posted` в таком случае будет всегда равно `TRUE`.

Следует отметить, что проведение по регистру документов с одним складом может быть также реализовано через агрегацию. Схема агрегаций более гибкая, но требует создания дополнительных объектов в системе, что может быть хуже с точке зрения производительности.

### Регистр сведений

Техника *регистра сведений* позволяет гибко реализовывать логику изменения некоторого показателя во времени. В отличие от регистра накоплений она рассчитывает не сумму показателя, а последнее значение действующее на определенное время.

Для реализации техники вводится абстрактный класс `PriceLedger`, один экземпляр которого отражает единичное изменение цены по одному SKU и одному складу в определенное время.

```lsf
CLASS ABSTRACT PriceLedger 'Регистр изменения цены поступления';

posted 'Проведен' = ABSTRACT BOOLEAN (PriceLedger);
dateTime 'Дата/время' = ABSTRACT DATETIME (PriceLedger);

sku 'SKU' = ABSTRACT SKU (PriceLedger);
stock 'Склад' = ABSTRACT Stock (PriceLedger);

price 'Цена' = ABSTRACT NUMERIC[14,2] (PriceLedger);

price 'Цена' (Stock stock, SKU sku, DATETIME dateTime) =
    GROUP LAST price(PriceLedger l)
          ORDER dateTime(l), l
          WHERE posted(l) AND dateTime(l) <= dateTime
          BY stock(l), sku(l);

price 'Цена' (Stock stock, SKU sku) =
    GROUP LAST price(PriceLedger l)
          ORDER dateTime(l), l
          WHERE posted(l)
          BY stock(l), sku(l);

price 'Цена' (SKU sku, DATETIME dateTime) =
    GROUP LAST price(PriceLedger l)
          ORDER dateTime(l), l
          WHERE posted(l) AND dateTime(l) <= dateTime
          BY sku(l);
```

На выходе получаем свойства, которые определяет цену по SKU и складу на дату/время, последнюю цену, а также последнюю цену по SKU для всех складов.

Аналогично регистру накоплений проводим документы по регистру сведений.

```lsf
EXTEND CLASS ReceiptDetail : PriceLedger;

// необходимо указывать [PriceLedger], так как ReceiptDetail также наследует SKULedger в этом же примере и платформе надо знать, какое именно свойство надо реализовать
posted[PriceLedger](ReceiptDetail d) += posted(receipt(d));
dateTime[PriceLedger](ReceiptDetail d) += dateTime(receipt(d));

stock[PriceLedger](ReceiptDetail d) += stock(receipt(d));

sku[PriceLedger](ReceiptDetail d) += sku(d);
price[PriceLedger](ReceiptDetail d) += price(d);
```

В данном случае, сигнатуру абстрактного свойства надо указывать в явную, так как с одним именем и пространством имен их существует несколько (точно также свойства называются для класса `SKULedger`).
