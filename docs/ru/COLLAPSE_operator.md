---
title: 'Оператор COLLAPSE'
---

Оператор `COLLAPSE` - создание [действия](Actions.md), реализующего [сворачивание элементов](Object_tree_visibility_EXPAND_COLLAPSE.md) [дерева объектов](Interactive_view.md#tree).

### Синтаксис

```
COLLAPSE [collapseType] formObjectGroupId [OBJECTS objName1 = expr1, ..., objNameN = exprN]
```

### Описание

Оператор `COLLAPSE` создает действие, которое позволяет свернуть определенные элементы дерева объектов на форме. Эти элементы могут быть определены с помощью блока `OBJECTS`. Если этот блок не указан, то операция сворачивания будет применена либо к текущему элементу дерева, либо к верхнему уровню элементов указанной [группы объектов](Form_structure.md#objects), в зависимости от типа операции.

### Параметры

- `collapseType`

    Тип сворачивания. Задается одним из следующих способов:

    - `DOWN`

        Ключевое слово, при указании которого будет происходить сворачивание элементов дерева. Если блок `OBJECTS` не указан, то операция применяется к текущему элементу.

    - `ALL`

        Ключевое слово, при указании которого будет происходить рекурсивное сворачивание элементов дерева и всех их потомков. Если блок `OBJECTS` не указан, то операция применяется к текущему элементу.

    - `ALL TOP`

        Два ключевых слова, при указании которых будет происходить рекурсивное сворачивание всех верхних элементов указанной группы объектов. Блок `OBJECTS` игнорируется.

    Если не указывается, то значением по умолчанию является `DOWN`.

- `formObjectGroupId`

    [Идентификатор группы объектов](IDs.md#groupobjectid), к которой применяется операция сворачивания.

- `objName1 ... objNameN`

    Имена объектов на форме. Объекты должны входить в указанную группу объектов. Имя объекта задается [простым идентификатором](IDs.md#id).

- `expr1 ... exprN`

    [Выражения](Expression.md), значения которых являются искомыми значениями соответствующих объектов в указанной группе объектов.

### Примеры

```lsf
FORM expandCollapseTest
    TREE elements e = NavigatorElement PARENT parent(e)
    PROPERTIES(e) READONLY BACKGROUND NOT e IS NavigatorFolder VALUE, canonicalName, caption
;

collapseDown {
    COLLAPSE DOWN expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

collapseAllTop {
    COLLAPSE ALL TOP expandCollapseTest.e;
}

EXTEND FORM expandCollapseTest
    PROPERTIES() collapseDown, collapseAllTop
;
```
