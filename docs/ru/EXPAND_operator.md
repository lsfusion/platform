---
title: 'Оператор EXPAND'
---

Оператор `EXPAND` - создание [действия](Actions.md), реализующего [разворачивание элементов](Object_tree_visibility_EXPAND_COLLAPSE.md) [дерева объектов](Interactive_view.md#tree).

### Синтаксис
```
EXPAND [expandType] formObjectGroupId [OBJECTS objName1 = expr1, ..., objNameN = exprN]
```

### Описание

Оператор `EXPAND` создает действие, которое позволяет развернуть определенные элементы дерева объектов на форме. Эти элементы могут быть определены с помощью блока `OBJECTS`. Если этот блок не указан, то операция разворачивания будет применена либо к текущему элементу дерева, либо к верхнему уровню элементов указанной [группы объектов](Form_structure.md#objects), в зависимости от типа операции. 

### Параметры

- `expandType`

    Тип разворачивания. Задается одним из следующих способов:
 
    - `DOWN`

        Ключевое слово, при указании которого будет происходить разворачивание элементов дерева. Если блок `OBJECTS` не указан, то операция применяется к текущему элементу.   

    - `UP`

        Ключевое слово, при указании которого будет происходить разворачивание элементов дерева, а также всех элементов, являющихся их предками. Если блок `OBJECTS` не указан, то операция применяется к текущему элементу.

    - `ALL`

        Ключевое слово, при указании которого будет происходить рекурсивное разворачивание элементов дерева и всех их потомков. Если блок `OBJECTS` не указан, то операция применяется к текущему элементу.

    - `ALL TOP`

        Два ключевых слова, при указании которых будет происходить рекурсивное разворачивание всех верхних элементов указанной группы объектов. Блок `OBJECTS` игнорируется.

    Если не указывается, то значением по умолчанию является `DOWN`.

- `formObjectGroupId`

    [Идентификатор группы объектов](IDs.md#groupobjectid), к которой применяется операция разворачивания.

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

expandDown {
    EXPAND DOWN expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

expandUp {
    EXPAND UP expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

expandAllTop {
    EXPAND ALL TOP expandCollapseTest.e;
}

EXTEND FORM expandCollapseTest
    PROPERTIES() expandDown, expandUp, expandAllTop
;
```
