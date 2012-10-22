package platform.server.form.entity;

import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;

import java.util.*;

/**
 * User: DAle
 * Date: 17.09.2010
 * Time: 14:42:51
 */

public class FormGroupHierarchyCreator {
    private FormEntity<?> form;

    public FormGroupHierarchyCreator(FormEntity<?> form) {
        this.form = form;
    }

    private void addDependencies(Map<GroupObjectEntity, Set<GroupObjectEntity>> graph, Set<GroupObjectEntity> groupsSet, boolean reverse) {
        GroupObjectEntity prev = null, cur = null;
        for (GroupObjectEntity group : form.groups) {
            if (groupsSet.contains(group)) {
                prev = cur;
                cur = group;
            }
            if (prev != null) {
                if (reverse) {
                    graph.get(cur).add(prev);
                } else {
                    graph.get(prev).add(cur);
                }
            }
        }
    }

    private static Set<GroupObjectEntity> getGroupsByObjects(Collection<ObjectEntity> objects) {
        Set<GroupObjectEntity> groupsSet = new HashSet<GroupObjectEntity>();
        for (ObjectEntity object : objects) {
            groupsSet.add(object.groupTo);
        }
        return groupsSet;
    }

    /**
     * Строим граф по зависимостям между GroupObjectEntity.
     * Если две группы связаны каким-нибудь свойством, фильтром и т.п., то добавляется ребро от "нижней" группы к "верхней"
     * Порядок групп определяется порядком в form.groups
     */
    private void addDependenciesToGraph(Map<GroupObjectEntity, Set<GroupObjectEntity>> graph) {
        for (PropertyDrawEntity<?> property : form.propertyDraws) {
            if (property.getToDraw(form) == property.propertyObject.getApplyObject(form.groups)) {
                Set<GroupObjectEntity> propObjects = getGroupsByObjects(property.propertyObject.getObjectInstances());
                // Для свойств с группами в колонках не добавляем зависимости от групп, идущих в колонки
                propObjects.removeAll(property.columnGroupObjects);
                addDependencies(graph, propObjects, true);

                if (property.propertyCaption != null) {
                    Set<GroupObjectEntity> captionObjects = getGroupsByObjects(property.propertyCaption.getObjectInstances());
                    addDependencies(graph, captionObjects, true);
                }

                if (property.propertyFooter != null) {
                    Set<GroupObjectEntity> footerObjects = getGroupsByObjects(property.propertyFooter.getObjectInstances());
                    addDependencies(graph, footerObjects, true);
                }
            }
        }

        for (GroupObjectEntity group : form.groups) {
            if (group.propertyBackground != null) {
                Set<GroupObjectEntity> backgroundObjects = getGroupsByObjects(group.propertyBackground.getObjectInstances());
                addDependencies(graph, backgroundObjects, true);
            }
            if (group.propertyForeground != null) {
                Set<GroupObjectEntity> foregroundObjects = getGroupsByObjects(group.propertyForeground.getObjectInstances());
                addDependencies(graph, foregroundObjects, true);
            }
        }

        for (FilterEntity filter : form.fixedFilters) {
            addDependencies(graph, getGroupsByObjects(filter.getObjects()), true);
        }

        for (RegularFilterGroupEntity filterGroup : form.regularFilterGroups) {
            for (RegularFilterEntity filter : filterGroup.filters) {
                addDependencies(graph, getGroupsByObjects(filter.filter.getObjects()), true);
            }
        }

        // добавляем дополнительные зависимости для свойств с группами в колонках
        for (GroupObjectEntity targetGroup : form.groups) { // перебираем группы в этом порядке, чтобы не пропустить зависимости
            for (PropertyDrawEntity<?> property : form.propertyDraws) {
                if (property.getToDraw(form) == property.propertyObject.getApplyObject(form.groups) && !property.columnGroupObjects.isEmpty()) {
                    if (targetGroup == property.getToDraw(form)) {
                        for (GroupObjectEntity columnGroup : property.columnGroupObjects) {
                            graph.get(targetGroup).addAll(graph.get(columnGroup));
                        }
                    }
                }
            }
        }
    }

    private Map<GroupObjectEntity, Set<GroupObjectEntity>> createNewGraph() {
        Map<GroupObjectEntity, Set<GroupObjectEntity>> graph = new HashMap<GroupObjectEntity, Set<GroupObjectEntity>>();
        for (GroupObjectEntity group : form.groups) {
            graph.put(group, new HashSet<GroupObjectEntity>());
        }
        return graph;
    }

    /**
     * Формирование графа зависимостей в виде леса (набора деревьев)
     *
     * Алгоритм:
     * Инвариант, который на каждом шаге алгоритма сохраняем: строящийся граф должен быть лесом на каждом шаге, то есть
     * в каждую вершину не может входить более одного ребра.
     *
     * Перебираем все группы в том же порядке в котором они находятся в form.groups.
     * Для каждой группы перебираем все исходящие ребра (то есть группы, от которых текущая группа зависит)
     * Каждое ребро (шаг алгоритма) пробуем добавить в результирующий граф, инвертируя его. Если получаем нарушение
     * инварианта, то берем это ребро, и ребро уже входящее в текущую группу, и начинаем по ним подниматься вверх по дереву
     * пока не найдем ближайшего общего предка (LCA).
     *      1. Если этот предок является одной из вершин двух исходных ребер, то это означает, что путь между этими вершинами
     *      уже существует, следовательно одно из ребер надо просто выкинуть
     *      2. Если же предком является какая-то другая вершина, или предок вообще не найден, то нам нужно объединить все
     *      вершины обоих путей в цепочку
     *
     * @param graph Инвертированный ациклический граф зависимостей между группами (в виде списков смежности).
     *              Все ребра идут от групп с большим индексом в form.groups к группам с меньшим индексом
     * @return Результирующий граф зависимостей в виде набора деревьев (в виде списков смежжнсти)
     */

    private Map<GroupObjectEntity, Set<GroupObjectEntity>> formForest(Map<GroupObjectEntity, Set<GroupObjectEntity>> graph) {
        int groupsCount = form.groups.size();
        Map<GroupObjectEntity, Set<GroupObjectEntity>> newGraph = createNewGraph();

        int[] parents = new int[groupsCount];
        Arrays.fill(parents, -1);

        for (int groupIndex = 0; groupIndex < groupsCount; groupIndex++) {
            GroupObjectEntity currentGroup = form.groups.get(groupIndex);
            for (GroupObjectEntity parentGroup : graph.get(currentGroup)) {
                int parentIndex = form.groups.indexOf(parentGroup);
                if (parents[groupIndex] == -1) {
                    parents[groupIndex] = parentIndex;
                } else {
                    List<Integer> visited = new ArrayList<Integer>();
                    int i1 = parents[groupIndex];
                    int i2 = parentIndex;
                    while (parents[i1] != parents[i2]) {
                        if (parents[i1] > parents[i2]) {
                            visited.add(i1);
                            i1 = parents[i1];
                       } else {
                            visited.add(i2);
                            i2 = parents[i2];
                        }
                    }

                    if (i1 == i2) { // если общим предком является одна из вершин двух исходных ребер
                        if (i1 == parents[groupIndex]) {  // если общий предок является старым предком
                            parents[groupIndex] = parentIndex;
                        }
                    } else {
                        visited.add(groupIndex);
                        visited.add(i1);
                        visited.add(i2);
                        if (parents[i1] != -1) {
                            visited.add(parents[i1]);
                        }
                        Collections.sort(visited);  // объединяем два пути в одну цепочку
                        for (int i = 0; i+1 < visited.size(); i++) {
                            parents[visited.get(i+1)] = visited.get(i);
                        }
                    }
                }
            }
        }
        // формируем результирующий граф
        for (int i = 0; i < parents.length; i++) {
            if (parents[i] != -1) {
                GroupObjectEntity from = form.groups.get(parents[i]);
                GroupObjectEntity to = form.groups.get(i);
                newGraph.get(from).add(to);
            }
        }

        return newGraph;
    }

    public GroupObjectHierarchy createHierarchy() {
        Map<GroupObjectEntity, Set<GroupObjectEntity>> graph = createNewGraph();
        addDependenciesToGraph(graph);
        graph = formForest(graph);
        Map<GroupObjectEntity, List<GroupObjectEntity>> dependencies = new HashMap<GroupObjectEntity, List<GroupObjectEntity>>();
        for (Map.Entry<GroupObjectEntity, Set<GroupObjectEntity>> entry : graph.entrySet()) {
            List<GroupObjectEntity> edges = new ArrayList<GroupObjectEntity>();
            for (GroupObjectEntity group : form.groups) {
                if (entry.getValue().contains(group)) {
                    edges.add(group);
                }
            }
            dependencies.put(entry.getKey(), edges);
        }
        return new GroupObjectHierarchy(form.groups, dependencies);
    }

}
