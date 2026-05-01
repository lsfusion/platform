---
title: 'Оператор NEWSESSION'
---

Оператор `NEWSESSION` - создание [действия](Actions.md), которое выполняет другое действие в [новой сессии](New_session_NEWSESSION_NESTEDSESSION.md).

### Синтаксис

```
NEWSESSION [NEWSQL] [FORMS formId1, ..., formIdM] [NESTED [nestedPropertySelector] [CLASSES]] [SINGLE] action
```

где `nestedPropertySelector` имеет один из следующих вариантов:

```
LOCAL
(propertyId1, ..., propertyIdN)
```

### Описание

Оператор `NEWSESSION` создает действие, которое выполняет другое действие в новой сессии.

При указании `NESTED LOCAL` или `NESTED (propertyId1, ..., propertyIdN)` в новой сессии становятся видны изменения соответствующих [локальных свойств](Data_properties_DATA.md#local) текущей сессии. Также изменения этих локальных свойств в новой сессии попадут в текущую сессию при применении изменений в новой сессии.

### Параметры

- `NEWSQL`

    Ключевое слово, при указании которого будет создано новое sql-соединение. В этом случае весь блок `NESTED ... [CLASSES]` игнорируется — в новую сессию не переносятся ни значения локальных свойств, ни изменения классов.

- `formId1, ..., formIdM`

    Список [идентификаторов форм](IDs.md#cid), указываемый после `FORMS`, к которым привязывается новая сессия. Сессия становится сессией изменений этих форм; используется, когда выполняемое действие должно вести себя так, как если бы оно было вызвано из этих форм.

- `NESTED`

    Опциональное ключевое слово, после которого можно указать, какие локальные свойства текущей сессии переносятся в новую. Само по себе, без `LOCAL` и без списка свойств, на поведение оператора не влияет.

- `LOCAL`

    Ключевое слово. Если указывается после `NESTED`, в новой сессии будут видны изменения всех локальных свойств.

- `propertyId1, ..., propertyIdN`

    Непустой список локальных свойств, указываемый после `NESTED` в круглых скобках, изменения которых будут видны в новой сессии. Каждый элемент списка должен являться [идентификатором свойства](IDs.md#propertyid).

- `CLASSES`

    Опциональное ключевое слово. Если указывается после `NESTED` и необязательного селектора локальных свойств, в новую сессию помимо тех локальных свойств, которые покрыл селектор, также переносятся [изменения классов](Class_change_CHANGECLASS_DELETE.md) существующих объектов (и объекты, [созданные](New_object_NEW.md) в текущей сессии).

- `SINGLE`

    Опциональное ключевое слово. Если `NEWSESSION` сам вызывается внутри [транзакции применения](Apply_changes_APPLY.md), этот флаг распространяется на внутреннее действие: изменения хранимых свойств, используемых им, записываются в базу инкрементально по ходу транзакции, а не одним пакетом в конце применения.

- `action`

    [Контекстно-зависимый оператор-действие](Action_operators.md#contextdependent), описывающий действие, которое должно быть выполнено в новой сессии.

### Примеры

```lsf
testNewSession ()  {
    NEWSESSION {
        NEW c = Currency {
            name(c) <- 'USD';
            code(c) <- 866;
        }
        APPLY;
    }
    // здесь новый объект класса Currency уже в базе данных

    LOCAL local = BPSTRING[10] (Currency);
    local(Currency c) <- 'Local';
    NEWSESSION {
        MESSAGE (GROUP SUM 1 IF local(Currency c) == 'Local'); // возвратит NULL
    }
    NEWSESSION NESTED (local[Currency]) {
        // возвратит кол-во объектов класса Currency
        MESSAGE (GROUP SUM 1 IF local(Currency c) == 'Local'); 
    }

    NEWSESSION {
        NEW s = Sku {
            id(s) <- 1234;
            name(s) <- 'New Sku';
            SHOW sku OBJECTS s = s;
        }
    }

}

// переносим в новую сессию созданный объект вместе с локальным свойством selected
selected = DATA LOCAL BOOLEAN (Sku);
markSelected ()  {
    NEW s = Sku;
    selected(s) <- TRUE;
    NEWSESSION NESTED (selected[Sku]) CLASSES {
        // здесь видны и созданный Sku, и selected[Sku]
        MESSAGE (GROUP SUM 1 IF selected(Sku s));
    }
}

// привязываем новую сессию к конкретной форме
showOnOrders ()  {
    NEWSESSION FORMS orders {
        SHOW orders;
    }
}

// запускаем действие в свежем SQL-соединении
backgroundJob ()  {
    NEWSESSION NEWSQL {
        APPLY;
    }
}

// SINGLE имеет смысл только когда NEWSESSION сам вызывается внутри транзакции применения
recalc ()  {
    APPLY {
        NEWSESSION SINGLE {
            // изменения здесь записываются в базу инкрементально по ходу внешнего apply
            id(Sku s) <- (GROUP MAX id(Sku ss)) (+) 1;
        }
    }
}
```
