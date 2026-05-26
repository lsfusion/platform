---
slug: "/NESTEDSESSION_operator"
title: 'Оператор NESTEDSESSION'
---

Оператор `NESTEDSESSION` создаёт [действие](../paradigm/Actions.md), которое выполняет другое действие во [вложенной сессии](../paradigm/New_session_NEWSESSION_NESTEDSESSION.md#nested).

### Синтаксис

```
NESTEDSESSION [SINGLE] action
```

### Описание

Оператор `NESTEDSESSION` создаёт действие, которое выполняет другое действие во вложенной сессии. Применение изменений во вложенной сессии копирует их обратно в окружающую сессию, а не записывает в базу данных.

### Параметры

- `SINGLE`

    Опциональное ключевое слово. Если `NESTEDSESSION` сам вызывается внутри [транзакции применения](../paradigm/Apply_changes_APPLY.md), этот флаг распространяется на внутреннее действие: изменения хранимых свойств, используемых им, записываются в базу инкрементально по ходу транзакции, а не одним пакетом в конце применения.

- `action`

    [Контекстно-зависимый оператор-действие](Action_operators.md#contextdependent), описывающий действие, которое должно быть выполнено во вложенной сессии.

### Примеры

```lsf
testNestedSession ()  {
    NESTEDSESSION {
        name(Sku s) <- 'aaa';
        APPLY; // на самом деле изменения применятся не в базу данных, а в "верхнюю" сессию
    }

    MESSAGE (GROUP SUM 1 IF name(Sku s) == 'aaa'); // возвращает все строки
    CANCEL;
    // возвращает NULL, если в базе не было раньше Sku с именем aaa
    MESSAGE (GROUP SUM 1 IF name(Sku s) == 'aaa'); 

}

FORM sku
    OBJECTS s = Sku PANEL
    PROPERTIES(s) id, name
;
newNestedSession()  {
    NESTEDSESSION {
        NEW s = Sku {
            // показывает форму, но любые изменения в ней не будут применены в базу данных,
            // а будут сохранены в "верхней сессии"
            SHOW sku OBJECTS s = s;
        }
    }
}

// SINGLE имеет смысл только когда NESTEDSESSION сам вызывается внутри транзакции применения
recalcNested ()  {
    APPLY {
        NESTEDSESSION SINGLE {
            // изменения здесь записываются в базу инкрементально по ходу внешнего apply
            name(Sku s) <- 'recalculated';
        }
    }
}
```
