---
title: 'Инструкция META'
---

Инструкция `META` - создание нового [метакода](Metaprogramming.md#metacode).

### Синтаксис

    META name(param1, ..., paramN)
        statement1
        ...
        statementM
    END

### Описание

Инструкция `META` объявляет новый метакод и добавляет его в текущий [модуль](Modules.md). 

Инструкция `МЕТА` является исключением - она не должна заканчиваться точкой с запятой.  

### Параметры

- `name`

    Имя метакода. [Простой идентификатор](IDs.md#id). Должно быть уникальным в пределах текущего пространства имен среди метакодов с таким же количеством параметров.

- `param1, ..., paramN`

    Список параметров метакода. Каждый параметр задается простым идентификатором. Список не может быть пустым.

- `statement1 ... statementM`

    Последовательность [инструкций](Statements.md), представляющих из себя блок кода. Инструкции могут содержать [специальные операции `##` и `###`](Metaprogramming.md#concat), предназначенные для объединения [лексем](Tokens.md). Инструкции не могут включать в себя еще одну инструкцию `META`.

### Примеры

```lsf
META objectProperties(object, type, caption)
    object##Name 'Имя'##caption = DATA BPSTRING[100](###object); // делаем заглавной первую букву
    object##Type 'Тип'##caption = DATA type (###object);
    object##Value 'Стоимость'##caption = DATA INTEGER (###object);
END

META objectProperties(object, type)
    @objectProperties(object, type, '');
END
```
