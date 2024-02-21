---
title: 'Оператор EXPAND'
---

Оператор `EXPAND` - создание [действия](Actions.md), реализующего разворачивание [дерева объектов](Object_blocks/#tree).

### Синтаксис
```
EXPAND [DOWN | UP | ALL [TOP]]
formGroupObjectId [OBJECTS objName1 = expr1, ..., objNameN = exprN]
```
### Описание

Оператор `EXPAND` создает действие, которое позволяет развернуть указанное дерево объектов на форме. Также в блоке OBJECTS можно задать текущее значение для разворачиваемого дерева объектов.

### Параметры

- `DOWN`

    Ключевое слово. Если указывается, то в дереве объектов разворачивается текущий элемент.

- `UP`

  Ключевое слово. Если указывается, то в дереве объектов разворачивается текущий элемент, а также все элементы, являющиеся предками текущего элемента.

- `ALL`

    Ключевое слово. Если указывается, то в дереве объектов рекурсивно разворачиваются текущий элемент и все потомки текущего элемента.

- `TOP`

    Ключевое слово. Если указывается, то рекурсивно разворачиваются все элементы дерева объектов.

- `formGroupObjectId`

    Глобальный [идентификатор группы объектов](IDs.md#groupobjectid), указывает на дерево объектов, который разворачивается.

- `objName1 ...  objNameN`

    Список имен объектов на форме. Может содержать только часть объектов указанной группы объектов. Имя объекта задается [простым идентификатором](IDs.md#id).

- `expr1 ... exprN`

    Список выражений, значения которых являются искомыми значениями соответствующих объектов в указанной группе объектов.

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
    PROPERTIES() expandDown, expandUp, expandAllTop;
```
