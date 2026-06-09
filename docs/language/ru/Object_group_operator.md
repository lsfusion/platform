---
slug: "/Object_group_operator"
title: 'Операторы групп объектов'
---

Операторы [групп объектов](../paradigm/Form_structure.md) - это набор операторов для создания [свойств](../paradigm/Properties.md), работающих с [текущим состоянием](../paradigm/Object_group_operators.md) группы объектов на [форме](../paradigm/Forms.md).

### Синтаксис

```
FILTER groupObjectId
VIEW groupObjectId
ORDER groupObjectId
SELECT groupObjectId
SELECT ACTIVE groupObjectId
VIEWTYPE groupObjectId
```

### Описание

Операторы `FILTER`, `VIEW`, `ORDER` и `SELECT` создают свойства, которые принимают на вход такое же количество параметров, как и количество объектов в группе объектов. Операторы групп объектов не могут использоваться внутри [выражений](Expression.md).

Оператор `FILTER` создает свойство, значением которого будет являться `TRUE`, если переданный в качестве параметров набор объектов проходит все критерии [фильтрации](../paradigm/Form_structure.md#filters) на форме, иначе значением свойства будет являться `NULL`.

Оператор `VIEW` создает свойство, значением которого будет являться `TRUE`, если переданный в качестве параметров набор объектов отображается в данный момент на форме, иначе значением свойства будет являться `NULL`.

Оператор `ORDER` создает свойство, значение которого определяет относительный порядок переданного в качестве параметра набора объектов на форме. Значение этого свойства обычно используется в блоках `ORDER` других свойств, например [`PARTITION`](PARTITION_operator.md), [`FOR`](FOR_operator.md), и т. д.

Оператор `SELECT` создаёт свойство, значением которого будет являться `TRUE`, если переданный в качестве параметров набор объектов выделен (отмечен) пользователем в группе объектов, иначе значением свойства будет являться `NULL`.

Оператор `SELECT ACTIVE` создаёт свойство без параметров, значением которого будет являться `TRUE`, если в группе объектов сейчас включено выделение нескольких строк, иначе `NULL`.

Оператор `VIEWTYPE` создаёт свойство без параметров, значением которого является текущий вид отображения группы объектов — объект системного класса `ListViewType` (`grid`, `pivot`, `map`, `custom` или `calendar`).

### Параметры

- `groupObjectId`

    Глобальный [идентификатор группы объектов](IDs.md#groupobjectid).

### Примеры

```lsf
CLASS Store;
name = DATA STRING[100] (Store);

FORM stores
    OBJECTS s = Store
;
countF 'Кол-во фильтр. складов' = GROUP SUM 1 IF [ VIEW stores.s](Store s);
orderF 'Порядок в группе объектов' (Store s) = PARTITION SUM 1 IF [ FILTER stores.s](s) ORDER [ ORDER stores.s](s), s;
isPivot 'Склады в виде сводной таблицы' () = [ VIEWTYPE stores.s]() == ListViewType.pivot;
selectedCount 'Количество выделенных складов' () = GROUP SUM 1 IF [ SELECT stores.s](Store s);
multiSelectActive 'Включено выделение нескольких строк' () = [ SELECT ACTIVE stores.s]();
setNameX 'Добавить X к имени'()  {
    LOCAL k = INTEGER ();
    k() <- 0;
    FOR [ FILTER stores.s](Store s) ORDER [ ORDER stores.s](s) DO {
        k() <- k() + 1;
        name(s) <- 'X' + k() + name(s);
    }
}
```
