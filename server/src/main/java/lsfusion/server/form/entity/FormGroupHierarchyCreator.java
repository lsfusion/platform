package lsfusion.server.form.entity;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;

import java.util.*;

public class FormGroupHierarchyCreator {
    private FormEntity form;
    
    private final boolean supportGroupColumns;

    public FormGroupHierarchyCreator(FormEntity form, boolean supportGroupColumns) {
        this.form = form;
        
        this.supportGroupColumns = supportGroupColumns;
    }
    
    private boolean addDependencies(Map<GroupObjectEntity, Set<GroupObjectEntity>> graph, Set<GroupObjectEntity> groupsSet) {
        boolean changed = false;
        GroupObjectEntity prev = null, cur = null;
        for (GroupObjectEntity group : getFormGroupsIt()) {
            if (groupsSet.contains(group)) {
                prev = cur;
                cur = group;
            }
            if (prev != null) {
                changed = graph.get(cur).add(prev) || changed;
            }
        }
        return changed;
    }

    private Iterable<GroupObjectEntity> getFormGroupsIt() {
        return form.getGroupsIt();
    }

    public static Set<GroupObjectEntity> getGroupsByObjects(ImSet<ObjectEntity> objects, ImOrderSet<GroupObjectEntity> groups) {
        Set<GroupObjectEntity> groupsSet = new HashSet<>();
        for (ObjectEntity object : objects) {
            GroupObjectEntity groupObject = object.groupTo;
            if(groups.contains(groupObject))
                groupsSet.add(groupObject);
        }
        return groupsSet;
    }

    /**
     * Строим граф по зависимостям между GroupObjectEntity.
     * Если две группы связаны каким-нибудь свойством, фильтром и т.п., то добавляется ребро от "нижней" группы к "верхней"
     * Порядок групп определяется порядком в form.groups
     */
    private void addDependenciesToGraph(ImOrderSet<GroupObjectEntity> groups, Map<GroupObjectEntity, Set<GroupObjectEntity>> graph, ImSet<GroupObjectEntity> excludeGroupObjects) {
        Iterable<PropertyDrawEntity> propertyDraws = form.getPropertyDrawsIt();
        for (PropertyDrawEntity<?> property : propertyDraws) {
            Set<GroupObjectEntity> propObjects = getGroupsByObjects(property.getObjectInstances(), groups);
            if(supportGroupColumns)
                propObjects.removeAll(property.getColumnGroupObjects().toJavaList());
            addDependencies(graph, propObjects);
        }

        for (GroupObjectEntity group : groups) {
            if (group.propertyBackground != null)
                addDependencies(graph, getGroupsByObjects(group.propertyBackground.getObjectInstances(), groups));
            if (group.propertyForeground != null)
                addDependencies(graph, getGroupsByObjects(group.propertyForeground.getObjectInstances(), groups));
        }

        for (FilterEntity filter : form.getFixedFilters()) {
            addDependencies(graph, getGroupsByObjects(filter.getObjects(), groups));
        }

        // temporary remove if assertion will not be broken
        for (RegularFilterGroupEntity filterGroup : form.getRegularFilterGroupsIt()) {
            for (RegularFilterEntity filter : filterGroup.getFiltersList()) {
                boolean changed = addDependencies(graph, getGroupsByObjects(filter.filter.getObjects(), groups));
                assert !changed;
            }
        }

        if(supportGroupColumns) { // temporary remove if assertion will not be broken
            for (GroupObjectEntity targetGroup : groups) {
                for (PropertyDrawEntity<?> property : propertyDraws) {
                    ImOrderSet<GroupObjectEntity> columnGroupObjects = property.getColumnGroupObjects();
                    if (!columnGroupObjects.isEmpty() && targetGroup == property.getApplyObject(form, excludeGroupObjects))
                        for (GroupObjectEntity columnGroup : columnGroupObjects) 
                            if(groups.contains(columnGroup)) {
                                assert graph.get(targetGroup).containsAll(graph.get(columnGroup));
                                graph.get(targetGroup).addAll(graph.get(columnGroup));
                            }
                }
            }
        }
    }

    private static Map<GroupObjectEntity, Set<GroupObjectEntity>> createNewGraph(ImOrderSet<GroupObjectEntity> groups) {
        Map<GroupObjectEntity, Set<GroupObjectEntity>> graph = new HashMap<>();
        for (GroupObjectEntity group : groups) {
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

    private Map<GroupObjectEntity, Set<GroupObjectEntity>> formForest(ImOrderSet<GroupObjectEntity> groups, Map<GroupObjectEntity, Set<GroupObjectEntity>> graph) {
        int groupsCount = groups.size();
        Map<GroupObjectEntity, Set<GroupObjectEntity>> newGraph = createNewGraph(groups);

        int[] parents = new int[groupsCount];
        Arrays.fill(parents, -1);

        for (int groupIndex = 0; groupIndex < groupsCount; groupIndex++) {
            GroupObjectEntity currentGroup = groups.get(groupIndex);
            for (GroupObjectEntity parentGroup : graph.get(currentGroup)) {
                int parentIndex = groups.indexOf(parentGroup);
                if (parents[groupIndex] == -1) {
                    parents[groupIndex] = parentIndex;
                } else {
                    List<Integer> visited = new ArrayList<>();
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
                GroupObjectEntity from = groups.get(parents[i]);
                GroupObjectEntity to = groups.get(i);
                newGraph.get(from).add(to);
            }
        }

        return newGraph;
    }

    public GroupObjectHierarchy createHierarchy(ImSet<GroupObjectEntity> excludeGroupObjects) {
        ImOrderSet<GroupObjectEntity> groups = form.getGroupsList().removeOrderIncl(excludeGroupObjects);
        
        Map<GroupObjectEntity, Set<GroupObjectEntity>> graph = createNewGraph(groups);
        addDependenciesToGraph(groups, graph, excludeGroupObjects);
        graph = formForest(groups, graph);
        
        // building list from set
        Map<GroupObjectEntity, ImOrderSet<GroupObjectEntity>> dependencies = new HashMap<>();
        MSet<GroupObjectEntity> mDependents = SetFact.mSet();
        for(GroupObjectEntity group : groups) {
            Set<GroupObjectEntity> edges = graph.get(group);
            ImOrderSet<GroupObjectEntity> orderedEdges = edges == null ? SetFact.<GroupObjectEntity>EMPTYORDER() : groups.filterOrderIncl(SetFact.fromJavaSet(edges));

            dependencies.put(group, orderedEdges);
            mDependents.addAll(orderedEdges.getSet());
        }
        dependencies.put(null, groups.removeOrderIncl(mDependents.immutable()));        
        return new GroupObjectHierarchy(null, dependencies);
    }

}
