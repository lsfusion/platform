---
title: 'Инструкция ACTION+'
---

Инструкция `ACTION+` добавляет реализацию к [абстрактному действию](Action_extension.md).

### Синтаксис

```lsf
[ACTION] abstractAction(param1, ..., paramN) +
    [WHEN conditionExpr THEN]
    { actionBody }
    [OPTIMISTICASYNC]
```

### Описание

Инструкция `ACTION+` не создает новое действие, а добавляет еще одну реализацию к уже объявленному [абстрактному действию](Action_extension.md).

Для абстрактного действия типа `CASE` используется блок `WHEN conditionExpr THEN`. Для действий типов `MULTI` и `LIST` реализация записывается без блока `WHEN ... THEN`.

### Параметры

- `ACTION`

    Необязательное ключевое слово. Явно показывает, что расширяется действие.

- `abstractAction`

    [Идентификатор](IDs.md#propertyid) расширяемого абстрактного действия.

- `param1, ..., paramN`

    Список [типизированных параметров](IDs.md#paramid) добавляемой реализации. Он задает ее сигнатуру. Список может быть пустым. Количество параметров и их классы должны быть совместимы с сигнатурой абстрактного действия. Эти параметры можно использовать в `actionBody` и, для формы `CASE`, в `conditionExpr`.

- `conditionExpr`

    [Выражение](Expression.md) для условия выбора этой реализации. Используется только для абстрактного действия типа `CASE`.

- `actionBody`

    Тело добавляемой реализации: содержимое [оператора `{...}`](Braces_operator.md), то есть последовательность [операторов-действий](Action_operators.md) и, при необходимости, объявлений `LOCAL`. Если абстрактное действие объявляет результат, возвращаемое значение и его параметры должны быть совместимы с этим результатом.

- `OPTIMISTICASYNC`

    Ключевое слово, помечающее добавляемую реализацию как оптимистично-асинхронную. Используется только в формах, где из нескольких реализаций выбирается одна.

### Примеры

```lsf
CLASS ABSTRACT Animal;
whoAmI ABSTRACT (Animal);

CLASS Dog : Animal;
whoAmI(Dog d) + {
    MESSAGE 'I am a dog!';
}

CLASS Cat : Animal;
whoAmI(Cat c) + {
    MESSAGE 'I am a cat!';
}
```

```lsf
CLASS ABSTRACT Animal;
CLASS Dog : Animal;

notify(Animal a) ABSTRACT (Animal);
notify(Dog d) {
    MESSAGE 'Dog';
}

notify[Animal](Dog d) + {
    notify(d);
}
```

```lsf
CLASS Human;
name = DATA STRING[100] (Human);

testName ABSTRACT CASE (Human);

testName(Human h) + WHEN name(h) == 'John' THEN {
    MESSAGE 'I am John';
}
testName(Human h) + WHEN name(h) == 'Bob' THEN {
    MESSAGE 'I am Bob';
}
```

```lsf
onStarted ABSTRACT LIST ();

onStarted() + {
    MESSAGE 'Подготовка данных';
}
onStarted() + {
    MESSAGE 'Запуск обработчиков';
}
```

```lsf
edit '{logics.edit}' ABSTRACT MULTI OVERRIDE FIRST (Object) TOOLBAR;

ACTION edit(Object o) + {
    SHOW EDIT Object = o DOCKED;
} OPTIMISTICASYNC
```
