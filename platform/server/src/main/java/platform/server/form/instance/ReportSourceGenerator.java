package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.ReportConstants;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.GroupObjectHierarchy;
import platform.server.logics.BusinessLogics;
import platform.server.session.Modifier;
import platform.server.session.SessionTableUsage;

import java.sql.SQLException;
import java.util.*;

import static platform.server.form.entity.GroupObjectHierarchy.ReportNode;

/**
 * User: DAle
 * Date: 12.08.2010
 * Time: 10:50:30
 */

public class ReportSourceGenerator<T extends BusinessLogics<T>>  {
    // Иерархия генерируемого отчета. Отличается от полной иерархии при отчете для одной таблицы
    private GroupObjectHierarchy.ReportHierarchy hierarchy;
    // Полная иерархия формы
    private GroupObjectHierarchy.ReportHierarchy fullFormHierarchy;
    private FormInstance<T> form;
    private Map<String, ReportData> sources = new HashMap<String, ReportData>();
    // ID группы при отчете для одной таблицы
    private final Integer groupId;
    // ID тех групп, которые идут в отчет таблицей значений.
    private final Set<Integer> gridGroupsId;
    private Map<Integer, GroupObjectInstance> idToInstance = new HashMap<Integer, GroupObjectInstance>();

    public static class ColumnGroupCaptionsData {
        // объекты, от которых зависит свойство
        public final Map<String, List<ObjectInstance>> propertyObjects = new HashMap<String, List<ObjectInstance>>();
        // объекты, идущие в колонки
        public final Map<String, List<ObjectInstance>> columnObjects = new HashMap<String, List<ObjectInstance>>();
        // таблицы значений свойств, ключ в таблице - набор значений объектов из propertyObjects
        public final Map<String, Map<List<Object>, Object>> data = new HashMap<String, Map<List<Object>, Object>>();
        // наборы различных значений объектов из columnObjects, идущих в колонки
        public final Map<String, LinkedHashSet<List<Object>>> columnData = new HashMap<String, LinkedHashSet<List<Object>>>();
    }
    
    public ReportSourceGenerator(FormInstance<T> form, GroupObjectHierarchy.ReportHierarchy hierarchy,
                                 GroupObjectHierarchy.ReportHierarchy fullFormHierarchy, Set<Integer> gridGroupsId, Integer groupId) {
        this.hierarchy = hierarchy;
        this.fullFormHierarchy = fullFormHierarchy;
        this.form = form;
        this.gridGroupsId = gridGroupsId;
        this.groupId = groupId;

        for (GroupObjectInstance group : form.groups) {
            idToInstance.put(group.getID(), group);
        }
    }

    public Map<String, ReportData> generate() throws SQLException {
        iterateChildReports(hierarchy.getRootNodes(), new ArrayList<GroupObjectInstance>(), null);
        return sources;
    }

    private Query<ObjectInstance, Pair<Object, PropertyType>> createQuery(List<GroupObjectInstance> groups, SessionTableUsage<ObjectInstance,
            Pair<Object, PropertyType>> parentTable, OrderedMap<Pair<Object, PropertyType>, Boolean> orders, Map<Pair<Object, PropertyType>, Type> types) {

        Query<ObjectInstance, Pair<Object, PropertyType>> newQuery;
        if (groupId == null) {
            newQuery = new Query<ObjectInstance, Pair<Object, PropertyType>>(GroupObjectInstance.getObjects(groups));
        } else {
            GroupObjectInstance ourGroup = null;
            for (GroupObjectInstance group : groups) {
                if (groupId.equals(group.getID())) {
                    ourGroup = group;
                    break;
                }
            }
            assert ourGroup != null;
            newQuery = new Query<ObjectInstance, Pair<Object, PropertyType>>(ourGroup.objects);
        }

        if (parentTable != null) {
            newQuery.and(parentTable.getWhere(newQuery.mapKeys));
        }

        Modifier modifier = form.getModifier();
        for (GroupObjectInstance group : groups) {
            if (groupId == null || groupId.equals(group.getID())) {
                newQuery.and(group.getWhere(newQuery.mapKeys, modifier));

                for(Map.Entry<OrderInstance, Boolean> order : BaseUtils.mergeOrders(group.orders, BaseUtils.toOrderedMap(new ArrayList<OrderInstance>(group.objects), false)).entrySet()) {
                    Pair<Object, PropertyType> orderObject = new Pair<Object, PropertyType>(order.getKey(), PropertyType.ORDER);
                    newQuery.properties.put(orderObject, order.getKey().getExpr(newQuery.mapKeys, modifier));
                    orders.put(orderObject, order.getValue());
                    types.put(orderObject, order.getKey().getType());
                }

                if (group.propertyBackground != null) {
                    Pair<Object, PropertyType> backgroundObject = new Pair<Object, PropertyType>(group.propertyBackground, PropertyType.BACKGROUND);
                    newQuery.properties.put(backgroundObject, group.propertyBackground.getExpr(newQuery.mapKeys, modifier));
                    types.put(backgroundObject, group.propertyBackground.getType());
                }

                if (!gridGroupsId.contains(group.getID()))
                    for (ObjectInstance object : group.objects) {
                        newQuery.and(object.getExpr(newQuery.mapKeys, modifier).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                    }
            }
        }

        for(PropertyDrawInstance<?> property : filterProperties(groups)) {
            if (property.columnGroupObjects.isEmpty()) {
                Pair<Object, PropertyType> propertyObject = new Pair<Object, PropertyType>(property, PropertyType.PLAIN);
                newQuery.properties.put(propertyObject, property.getDrawInstance().getExpr(newQuery.mapKeys, modifier));
                types.put(propertyObject, property.propertyObject.getType());

                if (property.propertyCaption != null) {
                    Pair<Object, PropertyType> captionObject = new Pair<Object, PropertyType>(property, PropertyType.CAPTION);
                    newQuery.properties.put(captionObject, property.propertyCaption.getExpr(newQuery.mapKeys, modifier));
                    types.put(captionObject, property.propertyCaption.getType());
                }

                if (property.propertyFooter != null) {
                    Pair<Object, PropertyType> footerObject = new Pair<Object, PropertyType>(property, PropertyType.FOOTER);
                    newQuery.properties.put(footerObject, property.propertyFooter.getExpr(newQuery.mapKeys, modifier));
                    types.put(footerObject, property.propertyFooter.getType());
                }
            }
        }
        return newQuery;
    }

    private void iterateChildReports(List<ReportNode> children, List<GroupObjectInstance> parentGroups, SessionTableUsage<ObjectInstance, Pair<Object, PropertyType>> parentTable) throws SQLException {
        for (ReportNode node : children) {
            String sid = node.getID();
            List<GroupObjectInstance> groups = new ArrayList<GroupObjectInstance>(parentGroups);
            List<GroupObjectInstance> localGroups = new ArrayList<GroupObjectInstance>();
            for (GroupObjectEntity group : node.getGroupList()) {
                GroupObjectInstance groupInstance = idToInstance.get(group.getID());
                localGroups.add(groupInstance);
            }
            groups.addAll(localGroups);

            OrderedMap<Pair<Object, PropertyType>, Boolean> orders = new OrderedMap<Pair<Object, PropertyType>, Boolean>();
            Map<Pair<Object, PropertyType>, Type> propTypes = new HashMap<Pair<Object, PropertyType>, Type>();
            Map<ObjectInstance, Type> keyTypes = new HashMap<ObjectInstance, Type>();
            for (ObjectInstance object : GroupObjectInstance.getObjects(groups)) {
                keyTypes.put(object, object.getType());
            }

            Query<ObjectInstance, Pair<Object, PropertyType>> query = createQuery(groups, parentTable, orders, propTypes);
            SessionTableUsage<ObjectInstance, Pair<Object, PropertyType>> reportTable = new SessionTableUsage<ObjectInstance, Pair<Object, PropertyType>>(
                    form.session.sql, query, form.BL.LM.baseClass, form.getQueryEnv(), keyTypes, propTypes);

            try {
                OrderedMap<Map<ObjectInstance, Object>, Map<Pair<Object, PropertyType>, Object>> resultData = reportTable.read(form, orders);

                List<Pair<String, PropertyReaderInstance>> propertyList = new ArrayList<Pair<String, PropertyReaderInstance>>();
                for(PropertyDrawInstance property : filterProperties(groups)) {
                    propertyList.add(new Pair<String, PropertyReaderInstance>(property.getsID(), property));
                    if (property.propertyCaption != null) {
                        propertyList.add(new Pair<String, PropertyReaderInstance>(property.getsID(), property.captionReader));
                    }
                    if (property.propertyFooter != null) {
                        propertyList.add(new Pair<String, PropertyReaderInstance>(property.getsID(), property.footerReader));
                    }
                }

                for (GroupObjectInstance group : groups) {
                    if (group.propertyBackground != null) {
                        propertyList.add(new Pair<String, PropertyReaderInstance>(group.propertyBackground.property.getSID(), group.rowBackgroundReader));
                    }
                }

                List<ObjectInstance> keyList = GroupObjectInstance.getObjects(groups);
                ReportData data = new ReportData(keyList, propertyList);

                for (Map.Entry<Map<ObjectInstance,Object>,Map<Pair<Object,PropertyType>,Object>> row : resultData.entrySet()) {
                    List<Object> propertyValues = new ArrayList<Object>();
                    for(PropertyDrawInstance property : filterProperties(groups)) {
                        propertyValues.add(row.getValue().get(new Pair<Object, PropertyType>(property, PropertyType.PLAIN)));
                        if (property.propertyCaption != null) {
                            propertyValues.add(row.getValue().get(new Pair<Object, PropertyType>(property, PropertyType.CAPTION)));
                        }
                        if (property.propertyFooter != null) {
                            propertyValues.add(row.getValue().get(new Pair<Object, PropertyType>(property, PropertyType.FOOTER)));
                        }
                    }

                    for (GroupObjectInstance group : groups) {
                        if (group.propertyBackground != null) {
                            propertyValues.add(row.getValue().get(new Pair<Object, PropertyType>(group.propertyBackground, PropertyType.BACKGROUND)));
                        }
                    }

                    data.add(BaseUtils.mapList(keyList, row.getKey()), propertyValues);
                }

                sources.put(sid, data);

                iterateChildReports(hierarchy.getChildNodes(node), groups, reportTable);
            } finally {
                reportTable.drop(form.session.sql);
            }
        }
    }

    private List<PropertyDrawInstance> filterProperties(Collection<GroupObjectInstance> filterGroups) {
        List<PropertyDrawInstance> resultList = new ArrayList<PropertyDrawInstance>();
        for (PropertyDrawInstance property : form.properties) {
            GroupObjectInstance applyGroup = property.propertyObject.getApplyObject();
            if ((applyGroup == null || property.toDraw == applyGroup) && filterGroups.contains(property.toDraw)) {
                resultList.add(property);
            }
        }
        return resultList;
    }

    public ColumnGroupCaptionsData getColumnGroupCaptions() throws SQLException {
        ColumnGroupCaptionsData resultData = new ColumnGroupCaptionsData();

        for (PropertyDrawInstance<?> property : form.properties) {
            if (property.columnGroupObjects.size() > 0) {
                List<GroupObjectInstance> groups = getNeededGroupsForColumnProp(property);
                OrderedMap<Map<ObjectInstance, Object>, Map<Object, Object>> qResult = getColumnPropQueryResult(groups, property);

                Collection<ObjectInstance> propObjects = property.propertyObject.getObjectInstances();

                Map<List<Object>, Object> data = new HashMap<List<Object>, Object>();
                Map<List<Object>, Object> captionData = new HashMap<List<Object>, Object>();
                Map<List<Object>, Object> footerData = new HashMap<List<Object>, Object>();
                LinkedHashSet<List<Object>> columnData = new LinkedHashSet<List<Object>>();

                for (Map.Entry<Map<ObjectInstance, Object>, Map<Object, Object>> entry : qResult.entrySet()) {
                    List<Object> values = new ArrayList<Object>();
                    for (ObjectInstance object : propObjects) {
                        values.add(entry.getKey().get(object));
                    }
                    data.put(values, entry.getValue().get(property));
                    if (property.propertyCaption != null) {
                        captionData.put(values, entry.getValue().get(property.propertyCaption));
                    }
                    if (property.propertyFooter != null) {
                        footerData.put(values, entry.getValue().get(property.propertyFooter));
                    }

                    List<Object> columnValues = new ArrayList<Object>();
                    for (ObjectInstance object : GroupObjectInstance.getObjects(property.columnGroupObjects)) {
                        columnValues.add(entry.getKey().get(object));
                    }
                    columnData.add(columnValues);
                }

                resultData.propertyObjects.put(property.getsID(), new ArrayList<ObjectInstance>(propObjects));
                resultData.columnObjects.put(property.getsID(), GroupObjectInstance.getObjects(property.columnGroupObjects));
                resultData.data.put(property.getsID(), data);
                if (property.propertyCaption != null) {
                    resultData.data.put(property.getsID() + ReportConstants.captionSuffix, captionData);
                }
                if (property.propertyFooter != null) {
                    resultData.data.put(property.getsID() + ReportConstants.footerSuffix, footerData);
                }
                resultData.columnData.put(property.getsID(), columnData);
            }
        }
        return resultData;
    }

    private OrderedMap<Map<ObjectInstance, Object>, Map<Object, Object>> getColumnPropQueryResult(List<GroupObjectInstance> groups, PropertyDrawInstance<?> property) throws SQLException {
        List<ObjectInstance> objects = GroupObjectInstance.getObjects(groups);
        Query<ObjectInstance, Object> query = new Query<ObjectInstance, Object>(objects);
        OrderedMap<Object, Boolean> queryOrders = new OrderedMap<Object, Boolean>();

        Modifier modifier = form.getModifier();
        for (GroupObjectInstance group : groups) {
            query.and(group.getWhere(query.mapKeys, modifier));

            for (Map.Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                query.properties.put(order.getKey(), order.getKey().getExpr(query.mapKeys, modifier));
                queryOrders.put(order.getKey(), order.getValue());
            }

            for (ObjectInstance object : group.objects) {
                query.properties.put(object, object.getExpr(query.mapKeys, modifier));
                queryOrders.put(object, false);
            }

            if (group.curClassView != ClassViewType.GRID) {
                for (ObjectInstance object : group.objects) {
                    query.and(object.getExpr(query.mapKeys, modifier).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                }
            }
        }

        query.properties.put(property, property.getDrawInstance().getExpr(query.mapKeys, modifier));
        if (property.propertyCaption != null) {
            query.properties.put(property.propertyCaption, property.propertyCaption.getExpr(query.mapKeys, modifier));
        }
        if (property.propertyFooter != null) {
            query.properties.put(property.propertyFooter, property.propertyFooter.getExpr(query.mapKeys, modifier));
        }

        return query.execute(form, queryOrders, 0);
    }

    private Set<GroupObjectInstance> getPropertyDependencies(PropertyDrawInstance<?> property) {
        Set<GroupObjectInstance> groups = new HashSet<GroupObjectInstance>();
        for (ObjectInstance object : property.propertyObject.getObjectInstances()) {
            groups.add(object.groupTo);
        }
        return groups;
    }

    // получает все группы, от которых зависит (не только непосредственно) свойство
    private List<GroupObjectInstance> getNeededGroupsForColumnProp(PropertyDrawInstance<?> property) {
        Set<GroupObjectInstance> initialGroups = getPropertyDependencies(property);
        Set<GroupObjectInstance> groups = new HashSet<GroupObjectInstance>();
        
        for (GroupObjectInstance group : initialGroups) {
            ReportNode curNode = fullFormHierarchy.getReportNode(group.entity);
            List<GroupObjectEntity> nodeGroups = curNode.getGroupList();
            int groupIndex = nodeGroups.indexOf(group.entity);
            for (int i = 0; i <= groupIndex; i++) {
                groups.add(idToInstance.get(nodeGroups.get(i).getID()));
            }

            curNode = fullFormHierarchy.getParentNode(curNode);
            while (curNode != null) {
                for (GroupObjectEntity nodeGroup : curNode.getGroupList()) {
                    groups.add(idToInstance.get(nodeGroup.getID()));
                }
                curNode = fullFormHierarchy.getParentNode(curNode);
            }
        }

        return BaseUtils.filterList(form.groups, groups);
    }

    public enum PropertyType {PLAIN, ORDER, CAPTION, FOOTER, BACKGROUND}
}
