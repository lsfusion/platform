---
title: 'Оператор EXPAND'
---

Оператор `COLLAPSE` - создание [действия](Actions.md), реализующего сворачивание [дерева объектов](Object_blocks/#tree).

### Синтаксис
```
COLLAPSE [DOWN | ALL [TOP]]
formGroupObjectId [OBJECTS objName1 = expr1, ..., objNameN = exprN]
```
### Описание

Оператор `COLLAPSE` создает действие, которое позволяет свернуть указанное дерево объектов на форме. Также в блоке OBJECTS можно задать текущее значение для сворачиваемого дерева объектов.

### Параметры

- `DOWN`

    Ключевое слово. Если указывается, то в дереве объектов сворачивается текущий элемент.

- `ALL`

    Ключевое слово. Если указывается, то в дереве объектов рекурсивно сворачиваются текущий элемент и все потомки текущего элемента.

- `TOP`

    Ключевое слово. Если указывается, то рекурсивно сворачиваются все элементы дерева объектов.

- `formGroupObjectId`

    Глобальный [идентификатор группы объектов](IDs.md#groupobjectid), указывает на дерево объектов, который сворачивается.

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

collapseDown {
COLLAPSE DOWN expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

collapseAllTop {
COLLAPSE ALL TOP expandCollapseTest.e;
}

EXTEND FORM expandCollapseTest
    PROPERTIES() collapseDown, collapseAllTop;
```
