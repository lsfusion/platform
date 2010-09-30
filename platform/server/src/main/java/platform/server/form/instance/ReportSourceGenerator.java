package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.ClassViewType;
import platform.interop.Compare;
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

/**
 * User: DAle
 * Date: 12.08.2010
 * Time: 10:50:30
 */

public class ReportSourceGenerator<T extends BusinessLogics<T>>  {
    private GroupObjectHierarchy.ReportHierarchy hierarchy;
    private FormInstance<T> form;
    private Map<String, FormData> sources = new HashMap<String, FormData>();
    private Map<Integer, GroupObjectInstance> idToInstance = new HashMap<Integer, GroupObjectInstance>();

    private Map<ObjectInstance, KeyExpr> instanceToExpr = new HashMap<ObjectInstance, KeyExpr>();
    private Map<Object, Boolean> orders = new HashMap<Object, Boolean>();

    public ReportSourceGenerator(FormInstance<T> form, GroupObjectHierarchy.ReportHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        this.form = form;
        for (GroupObjectInstance group : form.groups) {
            idToInstance.put(group.getID(), group);
        }
    }

    public Map<String, FormData> generate() throws SQLException {
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

        for (GroupObjectInstance group : groups)
        {
            newQuery.and(group.getWhere(instanceToExpr, form));

            for(Map.Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                PropertyField orderProp = table.ordersToFields.get(order.getKey());
                newQuery.properties.put(orderProp, order.getKey().getExpr(instanceToExpr, form));
            }

            for(ObjectInstance object : group.objects) {
                PropertyField objectProp = table.objectsToFields.get(object);
                newQuery.properties.put(objectProp, object.getExpr(instanceToExpr, form));
            }

            if (group.curClassView != ClassViewType.GRID) {
                for (ObjectInstance object : group.objects) {
                    newQuery.and(object.getExpr(instanceToExpr, form).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                }
            }
        }

        for(PropertyDrawInstance<?> property : form.properties)
            if (groups.contains(property.propertyObject.getApplyObject())) {
                PropertyField propField = table.objectsToFields.get(property);
                newQuery.properties.put(propField, property.propertyObject.getExpr(instanceToExpr, form));
            }

        if (parentTable != null) {
            newQuery.and(parentTable.joinAnd(BaseUtils.join(parentTable.mapKeys, instanceToExpr)).getWhere());
        }

        return newQuery;
    }

    private void iterateChildReports(List<ReportNode> children, List<GroupObjectInstance> parentGroups, ReportTable parentTable) throws SQLException {
        // Apply filters & orders in FormInstance!
        // Different states of the group!
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

            
            FormData data = new FormData();
            Map<ObjectInstance, KeyField> keyFields = BaseUtils.reverse(resTable.mapKeys);

            List<ObjectInstance> keyList = new ArrayList<ObjectInstance>();
            for (GroupObjectInstance group : groups) {
                for (ObjectInstance object : group.objects) {
                    keyList.add(object);
                }
            }
            data.setOrderedKeys(keyList);

            for (Map.Entry<Map<KeyField, Object>, Map<PropertyField, Object>> row : resultData.entrySet()) {
                Map<ObjectInstance, Object> groupValue = new HashMap<ObjectInstance, Object>();
                for (GroupObjectInstance group : groups) {
                    for (ObjectInstance object : group.objects) {
                        groupValue.put(object, row.getKey().get(keyFields.get(object)));
                    }
                }

                Map<PropertyDrawInstance,Object> propertyValues = new HashMap<PropertyDrawInstance, Object>();
                for(PropertyDrawInstance property : form.properties)
                    if (groups.contains(property.toDraw))
                        propertyValues.put(property, row.getValue().get(resTable.objectsToFields.get(property)));

                data.add(groupValue, propertyValues);
            }
            sources.put(sid, data);

            iterateChildReports(hierarchy.getChildNodes(node), groups, resTable);
            form.session.dropTemporaryTable(table);
        }
    }
}
