---
title: 'Управление материальными потоками'
---

## Описание задачи "Управление материальными потоками"

Создаваемая с помощью платформы **lsFusion** информационная система должна содержать функциональность для учета движения товаров на складе.

Для упрощения зададим в системе один вид документа, увеличивающего остаток на складе - приходная накладная от поставщика и один вид документа, уменьшающего остаток на складе - расходная накладная оптовой продажи товара покупателю.

## Задание предметной логики

Информационная система будет состоять из подмножества [модулей](Modules.md), в каждом из которых реализуется некоторая логически обособленная функциональность. В каждом из модулей может использоваться функциональность других модулей, для чего используются специальные конструкции задания зависимости модулей.

Исходя из постановки задачи выделим перечень модулей, подлежащих реализации: модуль склада, модуль товара, модуль организации, модуль приходной накладной, модуль расходной накладной, модуль текущих остатков. Отдельно также выделяется головной модуль, который будет запускаться на выполнение и фактически будет представлять собой скомпонованное прикладное решение. Состав модулей может быть и иным, и определяется разработчиком самостоятельно исходя из потребности повторного использования функциональности.

### Определение склада

Создаем модуль, в котором далее определим сущность склада и его атрибуты.

```lsf
MODULE Stock;
```

Задаем понятие склада и его атрибуты: наименование, адрес.

```lsf
CLASS Stock 'Склад';

name 'Наименование' = DATA STRING[100] (Stock) IN base;
address 'Адрес' = DATA STRING[150] (Stock) IN base;
```

### Определение товара

Создаем модуль, в котором определяем сущность товара и его атрибуты.

```lsf
MODULE Item;
```

Задаем понятие товара и его атрибуты: наименование, штрих-код.

```lsf
CLASS Item 'Товар';

name 'Наименование' = DATA STRING[100](Item) IN base;
barcode 'Штрихкод' = DATA BPSTRING[13](Item) IN base;
```

Для товара зададим цену продажи, по которой он будет реализовываться оптовым покупателям.

```lsf
salePrice 'Оптовая цена' = DATA NUMERIC[17,2](Item) IN base;
```

### Определение организации

Создаем модуль, в котором определяем сущность организации и ее атрибуты. Организации в системе будут выступать в роли поставщиков и покупателей.

```lsf
MODULE LegalEntity;
```

Задаем понятие организации и ее атрибуты: наименование, юридический адрес, ИНН.

```lsf
CLASS LegalEntity 'Организация';

name 'Наименование' = DATA STRING[100](LegalEntity) IN base;
address 'Адрес' = DATA STRING[150](LegalEntity) IN base;
inn 'ИНН' = DATA BPSTRING[9](LegalEntity) IN base;
```

Задаем уникальность ИНН для организации.

```lsf
legalEntityINN = GROUP AGGR LegalEntity legalEntity BY inn(legalEntity);
```

Свойство `legalEntityINN` связывает организацию и ИНН один к одному, и позволяет по заданному ИНН определить организацию. Выражение свойства можно трактовать следующим образом: при группировке организаций по ИНН (свойству `inn`) в каждой из групп должна быть не повторяющаяся организация.

### Определение приходной накладной

Создаем модуль, в котором определяем все сущности и атрибуты, необходимые для задания логики приходной накладной от поставщика.

```lsf
MODULE Receipt;
```

Зададим использование в модуле `Receipt` функциональности из других модулей.

```lsf
REQUIRE Stock, Item, LegalEntity;
```

Задаем понятия, определяющие логику приходной накладной. Будем исходить из принципа, что все документы (как приходные, так и расходные) в системе состоят из шапки и товарной спецификации. Соответственно определим понятия шапка приходной накладной и строка приходной накладной.

```lsf
CLASS Receipt 'Приходная накладная';
CLASS ReceiptDetail 'Строка приходной накладной';
```

  

У каждой строки приходной накладной есть ссылка на шапку документа, и в итоге шапка документа и подмножество строк со ссылкой на этот документ в совокупности определяют приходную накладную с точке зрения пользователя. Параметр `NONULL` обозначает, что ссылка должна быть задана. Параметр `DELETE` определяет, что при удалении основного объекта `Receipt`, все строки `ReceiptDetail`, ссылающиеся на него, будут также удалены. По умолчанию, при удалении объекта все ссылки на него обнуляются. Таким образом, без параметра `DELETE` будет выдана ошибка, что ссылка не задана.

```lsf
receipt 'Документ строки' = DATA Receipt (ReceiptDetail) NONULL DELETE;
```

  

Определяем номер строки в приходной накладной.

```lsf
index 'Номер строки' (ReceiptDetail d) =
        PARTITION SUM 1 IF d IS ReceiptDetail
        ORDER d BY receipt(d);
```
:::info
Использование в выражениях имени класса объекта равнозначно использованию его идентификационного номера (id), создаваемого системой для всех объектов автоматическим счетчиком. В данном случае использование для сортировки конструкции `ORDER d` позволяет отсортировать строки накладной в порядке возрастания их id, т.е. фактически в порядке последовательности их создания.
:::

Здесь в операторе `PARTITION` используется блок `BY`, группирующий объекты по некоторому атрибуту и расчет нарастающим итогом суммы выражения выполняется в рамках каждой из групп. В данном случае определение номера строки идет только в рамках документа этой строки (свойство `receipt`).

Задаем набор основных атрибутов шапки накладной: номер, дата, поставщик и его наименование, склад, на которой осуществляется оприходование товара, и его наименование. Наименование поставщика и склада в последующем понадобятся для удобного отображения на форме.

```lsf
number 'Номер накладной' = DATA BPSTRING[10] (Receipt);
date 'Дата накладной' = DATA DATE (Receipt);

supplier 'Поставщик' = DATA LegalEntity (Receipt);
nameSupplier 'Наименование поставщика' (Receipt r) = name(supplier(r));

stock 'Склад' = DATA Stock (Receipt);
nameStock 'Наименование склада' (Receipt r) = name(stock(r));
```

Задаем набор основных атрибутов строки накладной: товар и его наименование, количество, цена поставщика, сумма поставщика (рассчитывается умножением цены на количество).

```lsf
item 'Товар' = DATA Item (ReceiptDetail);
nameItem 'Наименование товара' (ReceiptDetail d) = name(item(d));

quantity 'Количество' = DATA NUMERIC[16,4] (ReceiptDetail);
price 'Цена поставщика' = DATA NUMERIC[17,2] (ReceiptDetail);
sum 'Сумма поставщика' (ReceiptDetail d) = quantity(d) * price(d);
```

### Определение расходной накладной

Создаем модуль, в котором определяем все сущности и атрибуты, необходимые для расходной накладной оптовой продажи товара.

```lsf
MODULE Shipment;
```

 Задаем использование в модуле `Shipment` функциональности из других модулей.

```lsf
REQUIRE Stock, Item, LegalEntity;
```

Аналогично приходной накладной задаем сущности шапка расходной накладной и строка расходной накладной, а также ссылку в строке на шапку и ее номер.

```lsf
CLASS Shipment 'Расходная накладная';
CLASS ShipmentDetail 'Строка расходной накладной';

shipment 'Документ строки' = DATA Shipment (ShipmentDetail) NONULL DELETE;
index 'Номер строки' (ShipmentDetail d) =
        PARTITION SUM 1 IF d IS ShipmentDetail
        ORDER d BY shipment(d);
```

  

Задаем набор атрибутов шапки накладной: номер, дата, покупатель и его наименование, склад, с которого осуществляется отгрузка товара, и его наименование.

```lsf
number 'Номер накладной' = DATA BPSTRING[10] (Shipment);
date 'Дата накладной' = DATA DATE (Shipment);

customer 'Покупатель' = DATA LegalEntity (Shipment);
nameCustomer 'Наименование покупателя' (Shipment s) = name(customer(s));

stock 'Склад' = DATA Stock(Shipment);
nameStock 'Наименование склада' (Shipment s) = name(stock(s));
```

  

Задаем набор основных атрибутов строки продажной накладной: товар и его наименование, количество, цена продажи, сумма продажи (рассчитывается умножением цены на количество).

```lsf
item 'Товар' = DATA Item (ShipmentDetail);
nameItem 'Наименование товара' (ShipmentDetail d) = name(item(d));

quantity 'Количество' = DATA NUMERIC[16,4](ShipmentDetail);
price 'Цена продажи' = DATA NUMERIC[17,2](ShipmentDetail);
sum 'Сумма продажи' (ShipmentDetail d) = quantity(d) * price(d);
```

  

Реализуем автозаполнение цены продажи товара по расходной накладной значением оптовой цены, заданной пользователем для товара (свойство `salePrice`). Автозаполнение должно срабатывать в момент изменения товара для строки расходной накладной (инструкция `WHEN CHANGED`).

```lsf
price(ShipmentDetail d) <- salePrice(item(d)) WHEN CHANGED(item(d));
```

  

### Определение текущего остатка товара на складе

Текущий остаток товара на складе определяется как разница между всеми приходами товара на склад и всеми его расходами со склада.

Создаем отдельный модуль.

```lsf
MODULE StockItem;
```

  

 Задаем использование в модуле `StockItem` функциональности из других модулей.

```lsf
REQUIRE Shipment, Receipt;
```

  

Задаем расчетное свойство текущего остатка товара на складе в количественном исчислении.

```lsf
receivedQuantity 'Суммарный приход' = GROUP SUM quantity(ReceiptDetail d) BY item(d), stock(receipt(d));
shippedQuantity 'Суммарный расход' = GROUP SUM quantity(ShipmentDetail d) BY item(d), stock(shipment(d));
currentBalance 'Текущий остаток' (Item i, Stock s) = receivedQuantity (i, s) (-) shippedQuantity (i, s);
```

  

Задаем запрет на отрицательный остаток по товару на складе. Запрет будет работать при любом действии пользователя, приводящему к возникновению товарного остатка меньше нуля. При этом на экране у пользователя будет появляться сообщение с заданным текстом.

```lsf
CONSTRAINT currentBalance(Item i, Stock s) < 0 MESSAGE 'Остаток по товару не может быть отрицательным';
```

  

## Задание логики представления

Для работы с созданным прикладным решением добавим формы справочников и форму текущих остатков, а также попарный набор форм работы с документами: форму просмотра приходных накладных и форму редактирования приходной накладной, форму просмотра расходных накладных и форму редактирования расходной накладной.

Вначале создаем формы справочников.

В модуле `Stock` добавляем форму, предоставляющую пользователю функциональность добавления и удаления складов, а также изменения их атрибутов.

```lsf
FORM stocks 'Склады'
	OBJECTS s = Stock
	PROPERTIES(s) name, address, NEW, DELETE
;
```

Аналогично в модуле `Item` создаем форму товаров, а в модуле `LegalEntity` - форму организаций.

```lsf
FORM items 'Товары'
	OBJECTS i = Item
	PROPERTIES(i) name, barcode, salePrice, NEW, DELETE
;
```

```lsf
FORM legalEntities 'Организации'
	OBJECTS l = LegalEntity
	PROPERTIES(l) name, inn, address, NEW, DELETE
;
```

Создаем формы редактирования приходной накладной и редактирования расходной накладной. Эти формы будут использоваться при создании новых документов или редактировании существующих. Формы будут строиться по одинаковому принципу: состоять из двух вертикально расположенных блоков, верхний из которых будет содержать в панельном виде атрибуты шапки создаваемого/редактируемого документа, а нижний - строки данного документа в табличном виде и их атрибуты.

В модуле `Receipt` создаем форму редактирования приходной накладной. Для создаваемой формы указываем, что она будет использоваться в качестве формы по умолчанию при создании/редактировании приходных накладных (блок `EDIT`).

```lsf
FORM receipt 'Приходная накладная'
	OBJECTS r = Receipt PANEL
	PROPERTIES(r) number, date, nameSupplier, nameStock

	OBJECTS d = ReceiptDetail
	PROPERTIES(d) index, nameItem, quantity, price, sum READONLY, NEW, DELETE GRID
	FILTERS receipt(d) = r

	EDIT Receipt OBJECT r
;
```

Фильтрация строк только текущей накладной выполняется с помощью выражения `FILTERS receipt(d) == r`. Конструкция `FILTERS` отображает объект соответствующего класса на форме, если выражение фильтра не возвращает `NULL`. В данном случае строка накладной отобразиться на форме в том случае, если шапка документа, на которую для строки задана ссылка (свойство `receipt`), равняется текущему объекту верхнего блока. Иными словами, отобразятся только строки создаваемого/редактируемого документа.

Кроме того, в случае, если для объектов данного класса на форме задан фильтр, то при нажатии пользователем кнопки `NEW` вновь добавленному объекту автоматически заполниться свойство исходя из того, чтобы этот объект удовлетворял заданному фильтру. В данном случае, при добавлении новой строки накладной этой строке автоматически заполниться свойство `receipt` ссылкой на текущую шапку накладной.

В модуле `Shipment` создаем форму редактирования расходной накладной. Для создаваемой формы указываем, что она будет использоваться в качестве формы по умолчанию при создании/редактировании расходных накладных (блок `EDIT`).

```lsf
FORM shipment 'Расходная накладная'
	OBJECTS s = Shipment PANEL
	PROPERTIES(s) number, date, nameCustomer, nameStock

	OBJECTS d = ShipmentDetail
	PROPERTIES(d) nameItem, quantity, price, sum READONLY, NEW, DELETE GRID
	FILTERS shipment(d) = s

	EDIT Shipment OBJECT s
;
```

Формы приходных и расходных накладных графически будут выглядеть практически идентичными и состоять из двух вертикально расположенных блоков табличного вида - блока шапок документов и блока строк документов. Строки документа должны визуально фильтроваться по документам и их отображаемое на форме подмножество будет изменяться при навигации в верхнем блоке.

Создаем форму приходных накладных. На форму выведем все свойства, определенные выше для шапок документов и их строк. Дополнительно выносим автоматически определенные кнопки добавления и редактирования приходной накладной с помощью формы редактирования, созданной выше. Все свойства как шапки документов, так и их строк, кроме кнопок добавления и редактирования приходной накладной, делаем недоступным для изменения непосредственно на форме (оператор `READONLY`).

```lsf
FORM receipts 'Приходные накладные'
	OBJECTS r = Receipt
	PROPERTIES(r) READONLY number, date, nameSupplier, nameStock
	PROPERTIES(r) NEWSESSION NEW, EDIT, DELETE

	OBJECTS d = ReceiptDetail
	PROPERTIES(d) READONLY index, nameItem, quantity, price, sum
	FILTERS receipt(d) = r
;
```

Аналогичным образом создаем форму расходных накладных.

```lsf
FORM shipments 'Расходные накладные'
	OBJECTS s = Shipment
	PROPERTIES(s) READONLY number, date, nameCustomer, nameStock
	PROPERTIES(s) NEWSESSION NEW, EDIT, DELETE

	OBJECTS d = ShipmentDetail
	PROPERTIES(d) READONLY nameItem, quantity, price, sum
	FILTERS shipment(d) = s
;
```

Далее в модуле `StockItem` создадим форму отображения текущих остатков. Форма должна представлять собой таблицу, в строках которой указывается товар (его наименование и штрих-код), наименование склада и текущий остаток данного товара на данном складе.Количество строк на форме по умолчанию будет равняться количеству введенных в систему товаров умноженному на количество введенных складов. Для отображения только значимых данных (т.е. только тех товаров и складов, для пересечения которых есть текущий остаток) добавим на форму фильтр.

```lsf
FORM currentBalanceItemStock 'Текущие остатки'
    OBJECTS si = (s = Stock, i = Item)
    PROPERTIES READONLY name(i), barcode(i), name(s), currentBalance(i, s)
    FILTERS currentBalance(i, s)
;
```

Конструкция `OBJECTS si = (s = Stock, i = Item)` добавляет группу объектов с псевдонином `si`, представляющая собой декартовое произведение объектов класса `Stock` и класса `Item`.

Объявляем головной модуль и указываем использование в нем функциональности других модулей.

```lsf
MODULE StockAccounting;

REQUIRE Stock, Item, LegalEntity, Receipt, Shipment, StockItem;
```

В модуле `StockAccounting` компонуем меню системы. Справочники добавляем в предопределенную папку навигатора `masterData`, а для документов создаем свою отдельную папку, которую показываем сразу после справочников. Вызов формы текущих остатков выносим в основное меню (горизонтальное окно `root`). Ссылки на формы справочников и документов будут отображаться в вертикальной панели инструментов `toolbar` при выборе пользователем соответствующей папки `root`.

```lsf
NAVIGATOR {
    NEW FOLDER masterData 'Справочники' FIRST WINDOW toolbar {
        NEW items;
        NEW stocks;
        NEW legalEntities;
    }
    NEW FOLDER documents 'Документы' AFTER masterData WINDOW toolbar {
        NEW receipts;
        NEW shipments;
    }
    NEW currentBalanceItemStock AFTER documents;
}
```

Процесс создания информационной системы завершен.

## Исходный код целиком (на [Github](https://github.com/lsfusion/samples/tree/master/mm))

```lsf
MODULE Stock;

CLASS Stock 'Склад';

name 'Наименование' = DATA STRING[100] (Stock) IN base;
address 'Адрес' = DATA STRING[150] (Stock) IN base;

FORM stocks 'Склады'
	OBJECTS s = Stock
	PROPERTIES(s) name, address, NEW, DELETE
;
```

```lsf
MODULE Item;

CLASS Item 'Товар';

name 'Наименование' = DATA STRING[100](Item) IN base;
barcode 'Штрихкод' = DATA BPSTRING[13](Item) IN base;

salePrice 'Оптовая цена' = DATA NUMERIC[17,2](Item) IN base;

FORM items 'Товары'
	OBJECTS i = Item
	PROPERTIES(i) name, barcode, salePrice, NEW, DELETE
;
```

```lsf
MODULE LegalEntity;

CLASS LegalEntity 'Организация';

name 'Наименование' = DATA STRING[100](LegalEntity) IN base;
address 'Адрес' = DATA STRING[150](LegalEntity) IN base;
inn 'ИНН' = DATA BPSTRING[9](LegalEntity) IN base;

legalEntityINN = GROUP AGGR LegalEntity legalEntity BY inn(legalEntity);

FORM legalEntities 'Организации'
	OBJECTS l = LegalEntity
	PROPERTIES(l) name, inn, address, NEW, DELETE
;
```

```lsf
MODULE Receipt;

REQUIRE Stock, Item, LegalEntity;

CLASS Receipt 'Приходная накладная';
CLASS ReceiptDetail 'Строка приходной накладной';

receipt 'Документ строки' = DATA Receipt (ReceiptDetail) NONULL DELETE;

index 'Номер строки' (ReceiptDetail d) =
        PARTITION SUM 1 IF d IS ReceiptDetail
        ORDER d BY receipt(d);

number 'Номер накладной' = DATA BPSTRING[10] (Receipt);
date 'Дата накладной' = DATA DATE (Receipt);

supplier 'Поставщик' = DATA LegalEntity (Receipt);
nameSupplier 'Наименование поставщика' (Receipt r) = name(supplier(r));

stock 'Склад' = DATA Stock (Receipt);
nameStock 'Наименование склада' (Receipt r) = name(stock(r));

item 'Товар' = DATA Item (ReceiptDetail);
nameItem 'Наименование товара' (ReceiptDetail d) = name(item(d));

quantity 'Количество' = DATA NUMERIC[16,4] (ReceiptDetail);
price 'Цена поставщика' = DATA NUMERIC[17,2] (ReceiptDetail);
sum 'Сумма поставщика' (ReceiptDetail d) = quantity(d) * price(d);

FORM receipt 'Приходная накладная'
	OBJECTS r = Receipt PANEL
	PROPERTIES(r) number, date, nameSupplier, nameStock

	OBJECTS d = ReceiptDetail
	PROPERTIES(d) index, nameItem, quantity, price, sum READONLY, NEW, DELETE GRID
	FILTERS receipt(d) = r

	EDIT Receipt OBJECT r
;

FORM receipts 'Приходные накладные'
	OBJECTS r = Receipt
	PROPERTIES(r) READONLY number, date, nameSupplier, nameStock
	PROPERTIES(r) NEWSESSION NEW, EDIT, DELETE

	OBJECTS d = ReceiptDetail
	PROPERTIES(d) READONLY index, nameItem, quantity, price, sum
	FILTERS receipt(d) = r
;
```

```lsf
MODULE Shipment;

REQUIRE Stock, Item, LegalEntity;

CLASS Shipment 'Расходная накладная';
CLASS ShipmentDetail 'Строка расходной накладной';

shipment 'Документ строки' = DATA Shipment (ShipmentDetail) NONULL DELETE;
index 'Номер строки' (ShipmentDetail d) =
        PARTITION SUM 1 IF d IS ShipmentDetail
        ORDER d BY shipment(d);

number 'Номер накладной' = DATA BPSTRING[10] (Shipment);
date 'Дата накладной' = DATA DATE (Shipment);

customer 'Покупатель' = DATA LegalEntity (Shipment);
nameCustomer 'Наименование покупателя' (Shipment s) = name(customer(s));

stock 'Склад' = DATA Stock(Shipment);
nameStock 'Наименование склада' (Shipment s) = name(stock(s));

item 'Товар' = DATA Item (ShipmentDetail);
nameItem 'Наименование товара' (ShipmentDetail d) = name(item(d));

quantity 'Количество' = DATA NUMERIC[16,4](ShipmentDetail);
price 'Цена продажи' = DATA NUMERIC[17,2](ShipmentDetail);
sum 'Сумма продажи' (ShipmentDetail d) = quantity(d) * price(d);

price(ShipmentDetail d) <- salePrice(item(d)) WHEN CHANGED(item(d));

FORM shipment 'Расходная накладная'
	OBJECTS s = Shipment PANEL
	PROPERTIES(s) number, date, nameCustomer, nameStock

	OBJECTS d = ShipmentDetail
	PROPERTIES(d) nameItem, quantity, price, sum READONLY, NEW, DELETE GRID
	FILTERS shipment(d) = s

	EDIT Shipment OBJECT s
;

FORM shipments 'Расходные накладные'
	OBJECTS s = Shipment
	PROPERTIES(s) READONLY number, date, nameCustomer, nameStock
	PROPERTIES(s) NEWSESSION NEW, EDIT, DELETE

	OBJECTS d = ShipmentDetail
	PROPERTIES(d) READONLY nameItem, quantity, price, sum
	FILTERS shipment(d) = s
;
```

```lsf
MODULE StockItem;

REQUIRE Shipment, Receipt;

receivedQuantity 'Суммарный приход' = GROUP SUM quantity(ReceiptDetail d) BY item(d), stock(receipt(d));
shippedQuantity 'Суммарный расход' = GROUP SUM quantity(ShipmentDetail d) BY item(d), stock(shipment(d));
currentBalance 'Текущий остаток' (Item i, Stock s) = receivedQuantity (i, s) (-) shippedQuantity (i, s);

CONSTRAINT currentBalance(Item i, Stock s) < 0 MESSAGE 'Остаток по товару не может быть отрицательным';

FORM currentBalanceItemStock 'Текущие остатки'
    OBJECTS si = (s = Stock, i = Item)
    PROPERTIES READONLY name(i), barcode(i), name(s), currentBalance(i, s)
    FILTERS currentBalance(i, s)
;
```

```lsf
MODULE StockAccounting;

REQUIRE Stock, Item, LegalEntity, Receipt, Shipment, StockItem;

NAVIGATOR {
    NEW FOLDER masterData 'Справочники' FIRST WINDOW toolbar {
        NEW items;
        NEW stocks;
        NEW legalEntities;
    }
    NEW FOLDER documents 'Документы' AFTER masterData WINDOW toolbar {
        NEW receipts;
        NEW shipments;
    }
    NEW currentBalanceItemStock AFTER documents;
}
```
