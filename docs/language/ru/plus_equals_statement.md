---
slug: "/plus_equals_statement"
title: 'Инструкция +='
---

Инструкция `+=` добавляет реализацию к [абстрактному свойству](../paradigm/Property_extension.md).

### Синтаксис

```lsf
abstractProperty(param1, ..., paramN) +=
    [WHEN conditionExpr THEN]
    implementationExpr;
```

### Описание

Инструкция `+=` не создает новое свойство. Она добавляет еще одну реализацию к уже объявленному абстрактному свойству.

Для абстрактного свойства типа `CASE` блок `WHEN conditionExpr THEN` обязателен. Для абстрактных свойств типов `MULTI` и `VALUE` блок `WHEN ... THEN` не используется, и выражение реализации записывается сразу после `+=`.

Позиция добавляемой реализации в [списке реализаций](../paradigm/Property_extension.md#poly) абстрактного свойства определяется его настройкой `OVERRIDE FIRST` / `OVERRIDE LAST`; доступные режимы — в статье об [операторе `ABSTRACT`](ABSTRACT_operator.md).

### Параметры

- `abstractProperty`

    [Идентификатор](IDs.md#propertyid) расширяемого абстрактного свойства.

- `param1, ..., paramN`

    Список [типизированных параметров](IDs.md#paramid) добавляемой реализации; задает ее сигнатуру. Список может быть пустым. Количество параметров и их классы должны быть совместимы с сигнатурой абстрактного свойства. Эти параметры можно использовать в `implementationExpr` и, для формы `CASE`, в `conditionExpr`.

- `conditionExpr`

    [Выражение](Expression.md) для условия выбора этой реализации. Используется только для абстрактного свойства типа `CASE`.

- `implementationExpr`

    Выражение для реализации. Его класс результата должен быть совместим с классом результата абстрактного свойства.

### Примеры

```lsf
CLASS ABSTRACT AClass;
CLASS BClass : AClass;
CLASS CClass : AClass;
CLASS DClass : AClass;

name(AClass a) = ABSTRACT BPSTRING[50] (AClass);
innerName(BClass b) = DATA BPSTRING[50] (BClass);
innerName(CClass c) = DATA BPSTRING[50] (CClass);
innerName(DClass d) = DATA BPSTRING[50] (DClass);

name(BClass b) = 'B' + innerName(b);
name(CClass c) = 'C' + innerName(c);

name[AClass](BClass b) += name(b);
name(CClass c) += name(c);
name(DClass d) += 'DClass' + innerName(d) IF d IS DClass;
```

```lsf
CLASS Person;
CLASS PersonDocumentType;
name = DATA ISTRING[64] (PersonDocumentType);

caption = ABSTRACT CASE ISTRING[100] (Person, PersonDocumentType);

caption(Person p, PersonDocumentType t) +=
    WHEN p IS Person AND name(t) == 'Passport' THEN 'Паспорт';
```
