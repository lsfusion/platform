---
slug: "/MULTI_action_operator"
title: 'Оператор MULTI'
---

Оператор `MULTI` создает [действие](../paradigm/Actions.md), реализующее [ветвление](../paradigm/Branching_CASE_IF_MULTI.md#poly) (полиморфная форма).

### Синтаксис

```
MULTI [exclusionType] action1, ..., actionN 
```

### Описание

Оператор `MULTI` создает действие, которое выполняет одно из переданных ему действий в зависимости от выполнения условий выбора. Условием выбора для каждого действия является [принадлежность параметров вызова сигнатуре](Property_signature_ISCLASS.md) этого действия; выполняется действие, для которого условие выбора выполнено.

### Параметры

- `exclusionType`

    [Тип взаимоисключения](../paradigm/Branching_CASE_IF_MULTI.md#exclusive). Определяет, могут ли несколько условий выбора действия выполняться одновременно при некотором наборе параметров:

    - `EXCLUSIVE` - условия выбора действия не могут выполняться одновременно. Используется по умолчанию.
    - `OVERRIDE` - несколько условий могут выполняться одновременно; в этом случае будет выбрано первое в списке действие с выполняющимся условием выбора.

- `action1, ..., actionN`

    Список [контекстно-зависимых операторов-действий](Action_operators.md#contextdependent), описывающих действия, из которых будет производиться выбор.

### Пример

```lsf
CLASS Shape;

CLASS Square : Shape;
CLASS Circle : Shape;

message (Square s)  { MESSAGE 'Square'; }
message (Circle c)  { MESSAGE 'Circle'; }

message (Shape s) = MULTI message[Square](s), message[Circle](s);
```
