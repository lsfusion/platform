---
title: 'Видимость в дереве объектов (EXPAND, COLLAPSE)'
---

Операторы *разворачивания* и *сворачивания* элементов [дерева объектов](Interactive_view.md#tree) позволяют управлять видимостью элементов дерева на [форме](Forms.md). В качестве входных данных этим операторам передается [группа объектов](Form_structure.md#objects), к которой применяется операция. Также указывается над какими элементами дерева (каждый из которых соответствует определенному набору объектов указанной группы) необходимо выполнить операцию:

- над [текущим](Form_structure.md#currentObject) элементом 
- над указанным множеством элементов
- над всеми верхними элементами указанной группы объектов

Возможные операции включают в себя:

- разворачивание или сворачивание элемента
- разворачивание или сворачивание элемента и всех его потомков
- разворачивание всех предков элемента

### Язык

Для объявления действий, реализующих операции разворачивания и сворачивания элементов дерева объектов, используются операторы [`EXPAND`](EXPAND_operator.md) и [`COLLAPSE`](COLLAPSE_operator.md).

### Примеры

```lsf
FORM expandCollapseTest
    TREE elements e = NavigatorElement PARENT parent(e)
    PROPERTIES(e) READONLY BACKGROUND NOT e IS NavigatorFolder VALUE, canonicalName, caption
;

expandDown {
    EXPAND DOWN expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

expandAllTop {
    EXPAND ALL TOP expandCollapseTest.e;
}

collapseDown {
    COLLAPSE DOWN expandCollapseTest.e OBJECTS e = navigatorElementCanonicalName('System.administration');
}

EXTEND FORM expandCollapseTest
    PROPERTIES() expandDown, expandAllTop, collapseDown
;
```