---
title: 'Оператор CASE'
---

Оператор `CASE` - создание [свойства](Properties.md), осуществляющего [выбор](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) результата по условию.

### Синтаксис 

    CASE [exclusionType]
        WHEN condition1 THEN result1
        ...
        WHEN conditionN THEN resultN
        [ELSE elseResult]

### Описание

Оператор `CASE` создает свойство, реализующее выбор по условию. Условия выбора задаются с помощью свойств, определенных в блоке `WHEN`. Если условие выбора выполняется, то значением свойства будет являться значение свойства указанного в соответствующем блоке `THEN`. Если ни одно из условий не выполняется, то значением свойства будет являться значение свойства в блоке `ELSE`, если этот блок указан (если не указан возвращается `NULL`).

### Параметры

- `exclusionType`

    [Тип взаимоисключения](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#exclusive). Определяет, могут ли одновременно несколько свойств-условий одновременно выполняться при некотором наборе параметров. Задается одним из ключевых слов:

    - `EXCLUSIVE`
    - `OVERRIDE`

  Тип `EXCLUSIVE` указывает на то, что никакие из перечисленных условий не могут выполняться одновременно. Тип `OVERRIDE` допускает несколько выполняющихся одновременно условий, в этом случае значением свойства будет значение первого из подходящих свойств-значений. 

    Тип `OVERRIDE`используется по умолчанию.

- `condition1 ... conditionN`

    [Выражения](Expression.md), значения которых определяют условие выбора. 

- `result1 ... resultN`

    Выражения, значения которых определяют результат выбора.

- `elseResult`

    Выражение, значение которого определяет значение свойства, если ни одно из условий не выполняется.

 
### Примеры 

```lsf
CLASS Color;
id = DATA STRING[100] (Color);

background 'Цвет' (Color c) = CASE
    WHEN id(c) == 'Black' THEN RGB(0,0,0)
    WHEN id(c) == 'Red' THEN RGB(255,0,0)
    WHEN id(c) == 'Green' THEN RGB(0,255,0)
;

id (TypeExecEnv type) = CASE EXCLUSIVE
    WHEN type == TypeExecEnv.materialize THEN 3
    WHEN type == TypeExecEnv.disablenestloop THEN 2
    WHEN type == TypeExecEnv.none THEN 1
    ELSE 0
;
```
