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

    private Map<GroupObjectEntity, Set<GroupObjectEntity>> formForest(Map<GroupObjectEntity, Set<GroupObjectEntity>> graph) {
        int groupsCount = form.groups.size();
        Set<GroupObjectEntity> innerGroups = new HashSet<GroupObjectEntity>();
        Map<GroupObjectEntity, Set<GroupObjectEntity>> newGraph = createNewGraph();

        for (int i = groupsCount - 1; i >= 0; --i) {
            if (!innerGroups.contains(form.groups.get(i))) {
                Set<GroupObjectEntity> was = new HashSet<GroupObjectEntity>();
                Queue<GroupObjectEntity> queue = new ArrayDeque<GroupObjectEntity>();
                was.add(form.groups.get(i));
                queue.add(form.groups.get(i));
                while (!queue.isEmpty()) {
                    GroupObjectEntity group = queue.remove();
                    for (GroupObjectEntity nextGroup : graph.get(group)) {
                        if (!was.contains(nextGroup)) {
                            was.add(nextGroup);
                            queue.add(nextGroup);
                        }
                    }
                }
                innerGroups.addAll(was);
                addDependencies(newGraph, was, false);
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
