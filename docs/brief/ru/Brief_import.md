---
slug: "/Brief_import"
title: 'Brief: импорт данных'
---

## Плоский импорт

[Оператор `IMPORT`](../language/IMPORT_operator.md) создает действие, которое читает файл, разбирает на колонки (поля) и [записывает](../paradigm/Property_change_CHANGE.md) каждую в свое свойство или параметр.

```
IMPORT [importFormat] FROM fileExpr importDestination
```

Приемник задается одним из двух способов ([оператор `IMPORT`](../language/IMPORT_operator.md)):

```
TO [(objClassId1, objClassId2, ..., objClassIdK)] propertyId1 [= columnId1], ..., propertyIdN [= columnIdN] [WHERE whereId]
FIELDS [(objClassId1 objAlias1, objClassId2 objAlias2, ..., objClassIdK objAliasK)] propClassId1 [propAlias1 =] columnId1 [NULL], ..., propClassIdN [propAliasN =] columnIdN [NULL] [DO actionOperator [ELSE elseActionOperator]]
```

Строки отображаются на импортируемые объекты: для числового класса это номер строки начиная с 0, для конкретного [пользовательского класса](../paradigm/User_classes.md) — новый объект на строку. Объект не более одного, по умолчанию `INTEGER` с именем `row`. Признак импортированной строки пишется в свойство из `WHERE whereId`, по умолчанию в `System.imported[INTEGER]` ([импорт данных](../paradigm/Data_import_IMPORT.md)).

```lsf
importSkus (FILE f) {
    IMPORT XLS SHEET 2 FROM f TO field1 = C, field2, field3 = F, field4 = A;
}
```

## Структурированный импорт и формы

Импорт формы — операция, обратная ее открытию в [структурированном представлении](../paradigm/Structured_view.md): значения из файла пишутся в свойства формы так, чтобы обратный экспорт воссоздал исходный файл ([Brief: экспорт данных](Brief_export.md)).

```
IMPORT formName [importFormat] [FROM (fileExpr | groupId1 = fileExpr1 [, ..., groupIdM = fileExprM])]
```

Иерархические форматы (**JSON**, **XML**) читаются одним файлом, плоские (**CSV**, **XLS**, **DBF**, **TABLE**) — по файлу на каждую группу объектов; пустая группа называется `root`. Без `FROM` берется `System.importFile`.

Импортируемая форма ограничена: объекты только числовых или конкретных пользовательских классов, в группе ровно один объект, свойства и фильтры изменяемые (как правило, [первичные свойства](Brief_properties.md)). Плоский импорт — его частный случай: форму платформа строит сама. Механизм — [в структурированном представлении](../paradigm/In_a_structured_view_EXPORT_IMPORT.md), представления формы — [Brief: формы](Brief_forms.md).

## Форматы и сопоставление полей

Формат задается ключевым словом и своими опциями:

```
JSON [ROOT rootExpr] [WHERE whereExpr] [CHARSET charsetStr]
XML [ROOT rootExpr] [ATTR] [WHERE whereExpr] [CHARSET charsetStr]
CSV [separator] [HEADER | NOHEADER] [ESCAPE | NOESCAPE] [WHERE whereExpr] [CHARSET charsetStr]
XLS [HEADER | NOHEADER] [SHEET (sheetExpr | ALL)] [WHERE whereExpr]
DBF [MEMO memoExpr] [WHERE whereExpr] [CHARSET charsetStr]
TABLE [WHERE whereExpr]
```

`XLS` читает и `xls`, и `xlsx`; отдельного ключевого слова `XLSX` на импорте нет. Без явного формата он определяется по классу файла — `JSONFILE`, `XMLFILE`, `CSVFILE`, `EXCELFILE`, `DBFFILE`, `TABLEFILE`, — а для класса `FILE` по расширению.

Колонка для свойства указывается как `= columnId` простым именем или строковым литералом; без нее берется колонка, следующая за указанной для предыдущего свойства. В `FIELDS` без псевдонима именем параметра становится имя поля файла. `WHERE whereExpr` отбирает строки текстовым условием вида `поле знак значение`.

## Обработка импортированных данных

`DO` относится к форме `FIELDS`: перечисленные в `FIELDS` имена становятся локальными параметрами, `DO` выполняется для каждой импортированной записи, и в его контексте доступны эти параметры, а `ELSE` — если не импортировано ни одной записи. У формы `TO` блока `DO` нет вовсе — значения остаются в перечисленных свойствах, а импортированные строки перебирают по свойству-признаку.

```lsf
importOrders (FILE t) {
    IMPORT FROM t FIELDS INTEGER a, DATE b, BPSTRING[50] c DO
        NEW o = Order {
            number(o) <- a;
            date(o) <- b;
            customer(o) <- c;
        }
}
```

Импорт пишет значения обычным изменением свойства, поэтому записанное попадает в текущую [сессию изменений](Brief_sessions.md) и применяется вместе с [событиями](Brief_events.md) и [ограничениями](Brief_constraints.md).

Сам файл берется из значения свойства: по URL его читает [оператор `READ`](../language/READ_operator.md), либо его возвращает [внешний вызов](Brief_integration.md), либо выбирает пользователь. Рекомендации по написанию импорта — [Правила: импорт данных](../rules/Rules_import.md).
