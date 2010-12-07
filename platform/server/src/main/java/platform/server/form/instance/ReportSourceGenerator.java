package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.ReportConstants;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.GroupObjectHierarchy;
import platform.server.logics.BusinessLogics;

import java.sql.SQLException;
import java.util.*;

import static platform.server.form.entity.GroupObjectHierarchy.ReportNode;
import static platform.server.form.instance.ReportTable.PropertyType;

/**
 * User: DAle
 * Date: 12.08.2010
 * Time: 10:50:30
 */

public class ReportSourceGenerator<T extends BusinessLogics<T>>  {
    private GroupObjectHierarchy.ReportHierarchy hierarchy;
    private FormInstance<T> form;
    private Map<String, ReportData> sources = new HashMap<String, ReportData>();
    private Map<Integer, GroupObjectInstance> idToInstance = new HashMap<Integer, GroupObjectInstance>();

    private Map<ObjectInstance, KeyExpr> instanceToExpr = new HashMap<ObjectInstance, KeyExpr>();
    private Map<Object, Boolean> orders = new HashMap<Object, Boolean>();

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
    
    public ReportSourceGenerator(FormInstance<T> form, GroupObjectHierarchy.ReportHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        this.form = form;
        for (GroupObjectInstance group : form.groups) {
            idToInstance.put(group.getID(), group);
        }
    }

    public Map<String, ReportData> generate() throws SQLException {
        form.endApply();
        form.applyFilters();
        form.applyOrders();

        createMaps();
        iterateChildReports(hierarchy.getRootNodes(), new ArrayList<GroupObjectInstance>(), null);
        return sources;
    }

    private void createMaps() {
        instanceToExpr.clear();
        orders.clear();

        Collection<ReportNode> nodes = hierarchy.getAllNodes();
        for (ReportNode node : nodes) {
            for (GroupObjectEntity group : node.getGroupList()) {
                GroupObjectInstance instance = idToInstance.get(group.getID());
                Map<ObjectInstance, KeyExpr> groupExprs = KeyExpr.getMapKeys(instance.objects);
                instanceToExpr.putAll(groupExprs);
            }
        }

        for (ReportNode node : nodes) {
            for (GroupObjectEntity group : node.getGroupList()) {
                GroupObjectInstance instance = idToInstance.get(group.getID());
                for (Map.Entry<OrderInstance, Boolean> order : instance.orders.entrySet()) {
                    orders.put(order.getKey(), order.getValue());
                }

                for(ObjectInstance object : instance.objects) {
                    orders.put(object, false);
                }
            }
        }
    }

    private Query<KeyField, PropertyField> createQuery(List<GroupObjectInstance> groups, ReportTable table, ReportTable parentTable) throws SQLException {

        Query<KeyField, PropertyField> newQuery =
                new Query<KeyField, PropertyField>(BaseUtils.join(table.mapKeys, instanceToExpr));

        for (GroupObjectInstance group : groups) {
            newQuery.and(group.getWhere(instanceToExpr, form));

            for(Map.Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                PropertyField orderProp = table.objectsToFields.get(new Pair<Object, PropertyType>(order.getKey(), PropertyType.ORDER));
                newQuery.properties.put(orderProp, order.getKey().getExpr(instanceToExpr, form));
            }

            for(ObjectInstance object : group.objects) {
                PropertyField objectProp = table.objectsToFields.get(new Pair<Object, PropertyType>(object, PropertyType.PLAIN));
                newQuery.properties.put(objectProp, object.getExpr(instanceToExpr, form));
            }

            if (group.propertyHighlight != null) {
                PropertyField highlightField =
                        table.objectsToFields.get(new Pair<Object, PropertyType>(group.propertyHighlight, PropertyType.HIGHLIGHT));
                newQuery.properties.put(highlightField, group.propertyHighlight.getExpr(instanceToExpr, form));
            }

            if (group.curClassView != ClassViewType.GRID) {
                for (ObjectInstance object : group.objects) {
                    newQuery.and(object.getExpr(instanceToExpr, form).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                }
            }
        }

        for(PropertyDrawInstance<?> property : form.properties) {
            if (groups.contains(property.propertyObject.getApplyObject()) && property.columnGroupObjects.isEmpty()) { // todo [dale]: getApplyObject???
                PropertyField propField = table.objectsToFields.get(new Pair<Object, PropertyType>(property, PropertyType.PLAIN));
                newQuery.properties.put(propField, property.propertyObject.getExpr(instanceToExpr, form));

                if (property.propertyCaption != null) {
                    PropertyField captionField = table.objectsToFields.get(new Pair<Object, PropertyType>(property, PropertyType.CAPTION));
                    newQuery.properties.put(captionField, property.propertyCaption.getExpr(instanceToExpr, form));
                }
            }
        }

        if (parentTable != null) {
            newQuery.and(parentTable.joinAnd(BaseUtils.join(parentTable.mapKeys, instanceToExpr)).getWhere());
        }

        return newQuery;
    }

    private void iterateChildReports(List<ReportNode> children, List<GroupObjectInstance> parentGroups, ReportTable parentTable) throws SQLException {
        for (ReportNode node : children) {
            String sid = node.getID();
            List<GroupObjectInstance> groups = new ArrayList<GroupObjectInstance>(parentGroups);
            List<GroupObjectInstance> localGroups = new ArrayList<GroupObjectInstance>();
            for (GroupObjectEntity group : node.getGroupList()) {
                GroupObjectInstance groupInstance = idToInstance.get(group.getID());
                localGroups.add(groupInstance);
            }
            groups.addAll(localGroups);

            ReportTable table = new ReportTable("report_" + sid, groups, form.properties);
            form.session.createTemporaryTable(table);
            Query<KeyField, PropertyField> query = createQuery(groups, table, parentTable);
            ReportTable resTable = table.writeRows(form.session, query, form.BL.baseClass);

            Query<KeyField, PropertyField> resultQuery = new Query<KeyField, PropertyField>(resTable);
            Join<PropertyField> tableJoin = resTable.join(resultQuery.mapKeys);
            resultQuery.properties.putAll(tableJoin.getExprs());
            resultQuery.and(tableJoin.getWhere());
            OrderedMap<Map<KeyField, Object>, Map<PropertyField, Object>> resultData =
                    resultQuery.execute(form.session, BaseUtils.innerJoin(resTable.orders, orders), 0);

            Map<ObjectInstance, KeyField> keyFields = BaseUtils.reverse(resTable.mapKeys);

            List<ObjectInstance> keyList = GroupObjectInstance.getObjects(groups);

            List<Pair<String, PropertyReadInstance>> propertyList = new ArrayList<Pair<String, PropertyReadInstance>>();
            for(PropertyDrawInstance property : form.properties) {
                if (groups.contains(property.toDraw)) {
                    propertyList.add(new Pair<String, PropertyReadInstance>(property.getsID(), property));
                    if (property.propertyCaption != null) {
                        propertyList.add(new Pair<String, PropertyReadInstance>(property.getsID(), property.caption));
                    }
                }
            }

            for (GroupObjectInstance group : groups) {
                if (group.propertyHighlight != null) {
                    propertyList.add(new Pair<String, PropertyReadInstance>(group.propertyHighlight.property.sID, group));
                }
            }
            
            ReportData data = new ReportData(keyList, propertyList);

            for (Map.Entry<Map<KeyField, Object>, Map<PropertyField, Object>> row : resultData.entrySet()) {
                List<Object> keyValues = new ArrayList<Object>();
                for (ObjectInstance object : keyList) {
                    keyValues.add(row.getKey().get(keyFields.get(object)));
                }

                List<Object> propertyValues = new ArrayList<Object>();
                for(PropertyDrawInstance property : form.properties)
                    if (groups.contains(property.toDraw)) {          // todo [dale]: разобраться c toDraw/getApplyObject (при генерации дизайнов тоже)
                        PropertyField field = resTable.objectsToFields.get(new Pair<Object, PropertyType>(property, PropertyType.PLAIN));
                        propertyValues.add(row.getValue().get(field));
                        if (property.propertyCaption != null) {
                            field = resTable.objectsToFields.get(new Pair<Object, PropertyType>(property, PropertyType.CAPTION));
                            propertyValues.add(row.getValue().get(field));
                        }
                    }

                for (GroupObjectInstance group : groups) {
                    if (group.propertyHighlight != null) {
                        PropertyField field = resTable.objectsToFields.get(new Pair<Object, PropertyType>(group.propertyHighlight, PropertyType.HIGHLIGHT));
                        propertyValues.add(row.getValue().get(field));
                    }
                }

                data.add(keyValues, propertyValues);
            }
            sources.put(sid, data);

            iterateChildReports(hierarchy.getChildNodes(node), groups, resTable);
            form.session.dropTemporaryTable(table);
        }
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
                resultData.columnData.put(property.getsID(), columnData);
            }
        }
        return resultData;
    }

    private OrderedMap<Map<ObjectInstance, Object>, Map<Object, Object>> getColumnPropQueryResult(List<GroupObjectInstance> groups, PropertyDrawInstance<?> property) throws SQLException {
        List<ObjectInstance> objects = GroupObjectInstance.getObjects(groups);
        Query<ObjectInstance, Object> query = new Query<ObjectInstance, Object>(objects);
        OrderedMap<Object, Boolean> queryOrders = new OrderedMap<Object, Boolean>();

        for (GroupObjectInstance group : groups) {
            query.and(group.getWhere(query.mapKeys, form));

            for (Map.Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                query.properties.put(order.getKey(), order.getKey().getExpr(query.mapKeys, form));
                queryOrders.put(order.getKey(), order.getValue());
            }

            for (ObjectInstance object : group.objects) {
                query.properties.put(object, object.getExpr(query.mapKeys, form));
                queryOrders.put(object, false);
            }

            if (group.curClassView != ClassViewType.GRID) {
                for (ObjectInstance object : group.objects) {
                    query.and(object.getExpr(query.mapKeys, form).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                }
            }
        }

        query.properties.put(property, property.propertyObject.getExpr(query.mapKeys, form));
        if (property.propertyCaption != null) {
            query.properties.put(property.propertyCaption, property.propertyCaption.getExpr(query.mapKeys, form));
        }

        return query.execute(form.session, queryOrders, 0);
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
            ReportNode curNode = hierarchy.getReportNode(group.entity);
            List<GroupObjectEntity> nodeGroups = curNode.getGroupList();
            int groupIndex = nodeGroups.indexOf(group.entity);
            for (int i = 0; i <= groupIndex; i++) {
                groups.add(idToInstance.get(nodeGroups.get(i).getID()));
            }

            curNode = hierarchy.getParentNode(curNode);
            while (curNode != null) {
                for (GroupObjectEntity nodeGroup : curNode.getGroupList()) {
                    groups.add(idToInstance.get(nodeGroup.getID()));
                }
                curNode = hierarchy.getParentNode(curNode);
            }
        }

        return BaseUtils.filterList(form.groups, groups);
    }
}
