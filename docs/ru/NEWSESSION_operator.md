---
title: 'Оператор NEWSESSION'
---

Оператор `NEWSESSION` - создание [действия](Actions.md), которое выполняет другое действие в [новой сессии](New_session_NEWSESSION_NESTEDSESSION.md).

### Синтаксис

    NEWSESSION [NEWSQL] [nestedBlock] action 

где `nestedBlock` имеет один из двух вариантов синтаксиса:

    NESTED LOCAL
    NESTED (propertyId1, ..., propertyIdN)

### Описание

Оператор `NEWSESSION` создает действие, которое выполняет другое действие в новой сессии.

При указании ключевого слова `NESTED` в новой сессии будут видны изменения [локальных свойств](Data_properties_DATA.md#local). При указании ключевого слова `LOCAL` будут видны изменения всех локальных свойств, иначе указывается список тех локальных свойств, изменения которых будут видны в новой сессии. Также изменения этих локальных свойств в новой сессии попадут в текущую сессию при применении изменений в новой сессии.

### Параметры

- `NEWSQL`

    Ключевое слово, при указании которого будет создано новое sql-соединение. В этом случае указание блока с ключевым словом `NESTED` будет проигнорировано.

- `LOCAL`

    Ключевое слово, при указании которого в новой сессии будут видны изменения всех локальных свойств.

- `propertyId1, ..., propertyIdN`

    Список локальных свойств, изменения которых будут видны в новой сессии. Каждый элемент списка должен являться [идентификатором свойства](IDs.md#propertyid).

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
    NEWSESSION NESTED (local) {
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
```
