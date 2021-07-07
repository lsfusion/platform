---
title: 'How-to: Наследование и агрегации'
---

Для демонстрации принципов наследования и агрегирования объектов, реализуем логику по созданию партий на основе документов поступления и производства. Сделаем так, чтобы каждый такой документ, для которого проставлен признак Проведен, автоматически генерировал ровно одну новую партию товара.

Создаем в логике понятие товар, партии которого будут учитываться:

```lsf
CLASS Item 'Товар';
name 'Имя' = DATA ISTRING[50] (Item) IN id;
FORM items 'Товары'
    OBJECTS i = Item
    PROPERTIES(i) name, NEW, DELETE
;
NAVIGATOR {
    NEW items;
}
```

Создадим [класс](User_classes.md) `Receipt`, объекты которого будут отражать поступление товаров:

```lsf
CLASS Receipt 'Поступление';
date 'Дата' = DATA DATE (Receipt) IN id;
item 'Товар' = DATA Item (Receipt);
nameItem 'Товар' (Receipt r) = name(item(r)) IN id;
posted 'Проведен' = DATA BOOLEAN (Receipt);

FORM receipts 'Поступления'
    OBJECTS r = Receipt
    PROPERTIES(r) date, nameItem, posted, NEW, DELETE
;
NAVIGATOR {
    NEW receipts;
}
```

В данном случае, для примера используем упрощенную схему с одним классом. На практике обычно используются два класса: `Receipt` (для документов) и `ReceiptDetail` (для строк документов).

По аналогии создадим класс `Production`, который будет использоваться для отражения производства товаров:

```lsf
CLASS Production 'Производство';
date 'Дата' = DATA DATE (Production) IN id;
item 'Товар' = DATA Item (Production);
nameItem 'Товар' (Production p) = name(item(p)) IN id;
posted 'Проведен' = DATA BOOLEAN (Production);

FORM productions 'Производства'
    OBJECTS p = Production
    PROPERTIES(p) date, nameItem, posted, NEW, DELETE
;
NAVIGATOR {
    NEW productions;
}
```

На данный момент мы создавали только обычные классы без какого-либо наследования. Для реализации логики партии создадим абстрактный класс `Batch`:

```lsf
CLASS ABSTRACT Batch 'Партия';
date 'Дата' = ABSTRACT DATE (Batch) IN id;
item 'Товар' = ABSTRACT Item (Batch);
nameItem 'Товар' (Batch b) = name(item(b));
type 'Тип' = ABSTRACT STRING[30] (Batch);

FORM batches 'Партии'
    OBJECTS b = Batch
    PROPERTIES(b) READONLY date, nameItem, type, objectClassName
;
NAVIGATOR {
    NEW batches;
}
```

Каждый объект этого класса будет соответствовать одной партии конкретного товара. Все его [свойства](Properties.md) объявлены абстрактными, то есть их реализация будет отличаться в зависимости от конкретного класса партии.

В системе не могут быть созданы объекта непосредственно абстрактного класса `Batch`. Для этого должны быть объявлены конкретные классы, которые будут от него унаследованы. В частности, создадим класс для партий, создаваемых на основе поступления товаров:

```lsf
CLASS ReceiptBatch 'Партия на основе поступления';
batch (Receipt receipt) = AGGR ReceiptBatch WHERE posted(receipt);
```

При помощи [оператора `AGGR`](AGGR_operator.md) для каждого объекта класса `Receipt`, у которого значение свойства `posted` определено, будет автоматически создаваться (и удаляться) объект класса `ReceiptBatch`. При этом создаются два свойства со ссылками этих объектов друг на друга: `batch(Receipt r)` и `receipt(ReceiptBatch b)`.

Дальше остается унаследовать класс `ReceiptBatch` от `Batch`, чтобы все партии, создаваемые документом поступления, также являлись объектами абстрактного класса (то есть ранее объявленными партиями):

```lsf
EXTEND CLASS ReceiptBatch : Batch;
date(ReceiptBatch rb) += date(receipt(rb));
item(ReceiptBatch rb) += item(receipt(rb));
type(ReceiptBatch rb) += 'Поступление' IF rb IS ReceiptBatch;
```

Наследование производится при помощи [инструкции `EXTEND CLASS`](EXTEND_CLASS_statement.md). После этого для каждого абстрактного свойства `Batch` задается, каким образом оно должно рассчитываться для конкретного класса `ReceiptBatch`. Значения даты и товара подтягиваются от документа поступления через ссылку `receipt(ReceiptBatch b)`. В тип партии подставляется нужная строка с условием, что объект нужного класса (иначе выражение будет определено для объектов любого класса, и система выдаст ошибку о несоответствии сигнатуры).

Следует отметить, что унаследовать класс можно было и непосредственно при объявлении класса `ReceiptBatch`.

По аналогии создаем партии для документов производства:

```lsf
CLASS ProductionBatch 'Партия на основе производства';
batch (Production production) = AGGR ProductionBatch WHERE posted(production);

EXTEND CLASS ProductionBatch : Batch;
date(ProductionBatch rb) += date(production(rb));
item(ProductionBatch rb) += item(production(rb));
type(ProductionBatch rb) += 'Производство' IF rb IS ProductionBatch;
```

При необходимости можно создать класс для ручного ввода партий самим пользователем:

```lsf
CLASS UserBatch 'Партия, созданная пользователем вручную';
date 'Дата' = DATA DATE (UserBatch) IN id;
item 'Товар' = DATA Item (UserBatch);
nameItem 'Товар' (UserBatch b) = name(item(b));

FORM userBatches 'Партии (ручные)'
    OBJECTS b = UserBatch
    PROPERTIES(b) date, nameItem, NEW, DELETE
;

NAVIGATOR {
    NEW userBatches;
}

EXTEND CLASS UserBatch : Batch;
date(UserBatch ub) += date(ub);
item(UserBatch ub) += item(ub);
type(UserBatch ub) += 'Ручные' IF ub IS UserBatch;
```
