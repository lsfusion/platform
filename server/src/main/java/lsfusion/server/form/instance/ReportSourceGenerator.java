package lsfusion.server.form.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.Compare;
import lsfusion.interop.form.ReportConstants;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.GroupObjectHierarchy;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.SessionTableUsage;

import java.sql.SQLException;
import java.util.*;

import static lsfusion.server.form.entity.GroupObjectHierarchy.ReportNode;

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

        for (GroupObjectInstance group : form.getGroups()) {
            idToInstance.put(group.getID(), group);
        }
    }

    public Map<String, ReportData> generate() throws SQLException {
        iterateChildReports(hierarchy.getRootNodes(), SetFact.<GroupObjectInstance>EMPTYORDER(), null);
        return sources;
    }

    private Query<ObjectInstance, Pair<Object, PropertyType>> createQuery(ImOrderSet<GroupObjectInstance> groups, SessionTableUsage<ObjectInstance,
            Pair<Object, PropertyType>> parentTable, Result<ImOrderMap<Pair<Object, PropertyType>, Boolean>> orders, Result<ImMap<Pair<Object, PropertyType>, Type>> types) {

        QueryBuilder<ObjectInstance, Pair<Object, PropertyType>> newQuery;
        if (groupId == null) {
            newQuery = new QueryBuilder<ObjectInstance, Pair<Object, PropertyType>>(GroupObjectInstance.getObjects(groups.getSet()));
        } else {
            GroupObjectInstance ourGroup = null;
            for (GroupObjectInstance group : groups) {
                if (groupId.equals(group.getID())) {
                    ourGroup = group;
                    break;
                }
            }
            assert ourGroup != null;
            newQuery = new QueryBuilder<ObjectInstance, Pair<Object, PropertyType>>(ourGroup.objects);
        }
        MExclMap<Pair<Object, PropertyType>, Type> mTypes = MapFact.mExclMap();
        MOrderExclMap<Pair<Object, PropertyType>, Boolean> mOrders = MapFact.mOrderExclMap();

        if (parentTable != null) {
            newQuery.and(parentTable.getWhere(newQuery.getMapExprs()));
        }

        Modifier modifier = form.getModifier();
        for (GroupObjectInstance group : groups) {
            if (groupId == null || groupId.equals(group.getID())) {
                newQuery.and(group.getWhere(newQuery.getMapExprs(), modifier));

                ImOrderMap<OrderInstance,Boolean> mergeOrders = group.orders.mergeOrder(group.getOrderObjects().toOrderMap(false));
                for(int i=0,size=mergeOrders.size();i<size;i++) {
                    OrderInstance order = mergeOrders.getKey(i);

                    Pair<Object, PropertyType> orderObject = new Pair<Object, PropertyType>(order, PropertyType.ORDER);
                    newQuery.addProperty(orderObject, order.getExpr(newQuery.getMapExprs(), modifier));
                    mOrders.exclAdd(orderObject, mergeOrders.getValue(i));
                    mTypes.exclAdd(orderObject, order.getType());
                }

                if (group.propertyBackground != null) {
                    Pair<Object, PropertyType> backgroundObject = new Pair<Object, PropertyType>(group.propertyBackground, PropertyType.BACKGROUND);
                    newQuery.addProperty(backgroundObject, group.propertyBackground.getExpr(newQuery.getMapExprs(), modifier));
                    mTypes.exclAdd(backgroundObject, group.propertyBackground.getType());
                }

                if (!gridGroupsId.contains(group.getID()))
                    for (ObjectInstance object : group.objects) {
                        newQuery.and(object.getExpr(newQuery.getMapExprs(), modifier).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                    }

            }
        }

        for(PropertyDrawInstance<?> property : filterProperties(groups.getSet())) {
            if (property.getColumnGroupObjects().isEmpty()) {
                Pair<Object, PropertyType> propertyObject = new Pair<Object, PropertyType>(property, PropertyType.PLAIN);
                newQuery.addProperty(propertyObject, property.getDrawInstance().getExpr(newQuery.getMapExprs(), modifier));
                mTypes.exclAdd(propertyObject, property.propertyObject.getType());

                if (property.propertyCaption != null) {
                    Pair<Object, PropertyType> captionObject = new Pair<Object, PropertyType>(property, PropertyType.CAPTION);
                    newQuery.addProperty(captionObject, property.propertyCaption.getExpr(newQuery.getMapExprs(), modifier));
                    mTypes.exclAdd(captionObject, property.propertyCaption.getType());
                }

                if (property.propertyFooter != null) {
                    Pair<Object, PropertyType> footerObject = new Pair<Object, PropertyType>(property, PropertyType.FOOTER);
                    newQuery.addProperty(footerObject, property.propertyFooter.getExpr(newQuery.getMapExprs(), modifier));
                    mTypes.exclAdd(footerObject, property.propertyFooter.getType());
                }
            }
        }
        types.set(mTypes.immutable());
        orders.set(mOrders.immutableOrder());
        return newQuery.getQuery();
    }

    private void iterateChildReports(List<ReportNode> children, ImOrderSet<GroupObjectInstance> parentGroups, SessionTableUsage<ObjectInstance, Pair<Object, PropertyType>> parentTable) throws SQLException {
        for (ReportNode node : children) {
            String sid = node.getID();
            List<GroupObjectEntity> groupList = node.getGroupList();
            MOrderExclSet<GroupObjectInstance> mLocalGroups = SetFact.mOrderExclSet(groupList.size()); // пограничные List'ы
            for (GroupObjectEntity group : groupList) {
                GroupObjectInstance groupInstance = idToInstance.get(group.getID());
                mLocalGroups.exclAdd(groupInstance);
            }

            ImOrderSet<GroupObjectInstance> groups = parentGroups.mergeOrder(mLocalGroups.immutableOrder()); // тут хрен поймешь excl или нет

            Result<ImOrderMap<Pair<Object, PropertyType>, Boolean>> orders = new Result<ImOrderMap<Pair<Object, PropertyType>, Boolean>>();
            Result<ImMap<Pair<Object, PropertyType>, Type>> propTypes = new Result<ImMap<Pair<Object, PropertyType>, Type>>();
            ImMap<ObjectInstance, Type> keyTypes = GroupObjectInstance.getObjects(groups.getSet()).mapValues(new GetValue<Type, ObjectInstance>() {
                public Type getMapValue(ObjectInstance value) {
                    return value.getType();
                }});

            Query<ObjectInstance, Pair<Object, PropertyType>> query = createQuery(groups, parentTable, orders, propTypes);
            SessionTableUsage<ObjectInstance, Pair<Object, PropertyType>> reportTable = new SessionTableUsage<ObjectInstance, Pair<Object, PropertyType>>(
                    form.session.sql, query, form.BL.LM.baseClass, form.getQueryEnv(), keyTypes, propTypes.result);

            try {
                ImOrderMap<ImMap<ObjectInstance, Object>, ImMap<Pair<Object, PropertyType>, Object>> resultData = reportTable.read(form, orders.result);

                List<Pair<String, PropertyReaderInstance>> propertyList = new ArrayList<Pair<String, PropertyReaderInstance>>();
                for(PropertyDrawInstance property : filterProperties(groups.getSet())) {
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

                ImOrderSet<ObjectInstance> keyList = GroupObjectInstance.getOrderObjects(groups);
                ReportData data = new ReportData(keyList.toJavaList(), propertyList);

                for (int i=0,size=resultData.size();i<size;i++) {
                    ImMap<Pair<Object, PropertyType>, Object> resultValue = resultData.getValue(i);

                    List<Object> propertyValues = new ArrayList<Object>();
                    for(PropertyDrawInstance property : filterProperties(groups.getSet())) {
                        propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.PLAIN)));
                        if (property.propertyCaption != null) {
                            propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.CAPTION)));
                        }
                        if (property.propertyFooter != null) {
                            propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.FOOTER)));
                        }
                    }

                    for (GroupObjectInstance group : groups) {
                        if (group.propertyBackground != null) {
                            propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(group.propertyBackground, PropertyType.BACKGROUND)));
                        }
                    }

                    data.add(BaseUtils.mapList(keyList.toJavaList(), resultData.getKey(i)), propertyValues);
                }

                sources.put(sid, data);

                iterateChildReports(hierarchy.getChildNodes(node), groups, reportTable);
            } finally {
                reportTable.drop(form.session.sql);
            }
        }
    }

    private List<PropertyDrawInstance> filterProperties(ImSet<GroupObjectInstance> filterGroups) {
        List<PropertyDrawInstance> resultList = new ArrayList<PropertyDrawInstance>();
        for (PropertyDrawInstance property : form.properties) {
            GroupObjectInstance applyGroup = property.propertyObject.getApplyObject();
            if ((applyGroup == null || property.toDraw == applyGroup) && property.toDraw!=null && filterGroups.contains(property.toDraw)) {
                resultList.add(property);
            }
        }
        return resultList;
    }

    public ColumnGroupCaptionsData getColumnGroupCaptions() throws SQLException {
        ColumnGroupCaptionsData resultData = new ColumnGroupCaptionsData();

        for (PropertyDrawInstance<?> property : form.properties) {
            ImOrderSet<GroupObjectInstance> columnGroupObjects = property.getOrderColumnGroupObjects();
            if (columnGroupObjects.size() > 0) {
                ImOrderSet<GroupObjectInstance> groups = getNeededGroupsForColumnProp(property);
                ImOrderMap<ImMap<ObjectInstance, Object>, ImMap<Object, Object>> qResult = getColumnPropQueryResult(groups, property);

                ImCol<ObjectInstance> propObjects = property.propertyObject.getObjectInstances();

                Map<List<Object>, Object> data = new HashMap<List<Object>, Object>();
                Map<List<Object>, Object> captionData = new HashMap<List<Object>, Object>();
                Map<List<Object>, Object> footerData = new HashMap<List<Object>, Object>();
                LinkedHashSet<List<Object>> columnData = new LinkedHashSet<List<Object>>();

                for (int i=0,size=qResult.size();i<size;i++) {
                    ImMap<ObjectInstance, Object> key = qResult.getKey(i); ImMap<Object, Object> value = qResult.getValue(i);

                    List<Object> values = new ArrayList<Object>();
                    for (ObjectInstance object : propObjects) {
                        values.add(key.get(object));
                    }
                    data.put(values, value.get(property));
                    if (property.propertyCaption != null) {
                        captionData.put(values, value.get(property.captionReader));
                    }
                    if (property.propertyFooter != null) {
                        footerData.put(values, value.get(property.footerReader));
                    }

                    List<Object> columnValues = new ArrayList<Object>();
                    for (ObjectInstance object : GroupObjectInstance.getOrderObjects(columnGroupObjects)) {
                        columnValues.add(key.get(object));
                    }
                    columnData.add(columnValues);
                }

                resultData.propertyObjects.put(property.getsID(), propObjects.toList().toJavaList());
                resultData.columnObjects.put(property.getsID(), GroupObjectInstance.getOrderObjects(columnGroupObjects).toJavaList());
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

    private ImOrderMap<ImMap<ObjectInstance, Object>, ImMap<Object, Object>> getColumnPropQueryResult(ImOrderSet<GroupObjectInstance> groups, PropertyDrawInstance<?> property) throws SQLException {
        ImSet<ObjectInstance> objects = GroupObjectInstance.getObjects(groups.getSet());
        QueryBuilder<ObjectInstance, Object> query = new QueryBuilder<ObjectInstance, Object>(objects);
        MOrderMap<Object, Boolean> mQueryOrders = MapFact.mOrderMap();

        Modifier modifier = form.getModifier();
        for (GroupObjectInstance group : groups) {
            query.and(group.getWhere(query.getMapExprs(), modifier));

            ImOrderMap<OrderInstance, Boolean> groupOrders = group.orders.mergeOrder(group.getOrderObjects().toOrderMap(false));
            for (int i=0,size=groupOrders.size();i<size;i++) {
                OrderInstance order = groupOrders.getKey(i);
                query.addProperty(order, order.getExpr(query.getMapExprs(), modifier));
                mQueryOrders.add(order, groupOrders.getValue(i));
            }

            if (group.curClassView != ClassViewType.GRID) {
                for (ObjectInstance object : group.objects) {
                    query.and(object.getExpr(query.getMapExprs(), modifier).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                }
            }
        }

        query.addProperty(property, property.getDrawInstance().getExpr(query.getMapExprs(), modifier));
        if (property.propertyCaption != null) {
            query.addProperty(property.captionReader, property.propertyCaption.getExpr(query.getMapExprs(), modifier));
        }
        if (property.propertyFooter != null) {
            query.addProperty(property.footerReader, property.propertyFooter.getExpr(query.getMapExprs(), modifier));
        }

        return query.execute(form, mQueryOrders.immutableOrder(), 0);
    }

    private Set<GroupObjectInstance> getPropertyDependencies(PropertyDrawInstance<?> property) {
        Set<GroupObjectInstance> groups = new HashSet<GroupObjectInstance>();
        for (ObjectInstance object : property.propertyObject.getObjectInstances()) {
            groups.add(object.groupTo);
        }
        return groups;
    }

    // получает все группы, от которых зависит (не только непосредственно) свойство
    private ImOrderSet<GroupObjectInstance> getNeededGroupsForColumnProp(PropertyDrawInstance<?> property) {
        Set<GroupObjectInstance> initialGroups = getPropertyDependencies(property);
        MSet<GroupObjectInstance> groups = SetFact.mSet();
        
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

        return form.getOrderGroups().filterOrderIncl(groups.immutable());
    }

    public enum PropertyType {PLAIN, ORDER, CAPTION, FOOTER, BACKGROUND}
}
