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
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import lsfusion.interop.form.ReportConstants;
import lsfusion.server.Settings;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.GroupObjectHierarchy;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.CalcProperty;
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
    private Map<String, ReportData> sources = new HashMap<>();
    // ID группы при отчете для одной таблицы
    private final Integer groupId;
    private FormUserPreferences userPreferences;
    // ID тех групп, которые идут в отчет таблицей значений.
    private final Set<Integer> gridGroupsId;
    private Map<Integer, GroupObjectInstance> idToInstance = new HashMap<>();

    public static class ColumnGroupCaptionsData {
        // объекты, от которых зависит свойство
        public final Map<String, List<ObjectInstance>> propertyObjects = new HashMap<>();
        // объекты, идущие в колонки
        public final Map<String, List<ObjectInstance>> columnObjects = new HashMap<>();
        // таблицы значений свойств, ключ в таблице - набор значений объектов из propertyObjects
        public final Map<String, Map<List<Object>, Object>> data = new HashMap<>();
        // наборы различных значений объектов из columnObjects, идущих в колонки
        public final Map<String, LinkedHashSet<List<Object>>> columnData = new HashMap<>();
    }
    
    public ReportSourceGenerator(FormInstance<T> form, GroupObjectHierarchy.ReportHierarchy hierarchy,
                                 GroupObjectHierarchy.ReportHierarchy fullFormHierarchy, Set<Integer> gridGroupsId, Integer groupId, FormUserPreferences userPreferences) {
        this.hierarchy = hierarchy;
        this.fullFormHierarchy = fullFormHierarchy;
        this.form = form;
        this.gridGroupsId = gridGroupsId;
        this.groupId = groupId;
        this.userPreferences = userPreferences;

        for (GroupObjectInstance group : form.getGroups()) {
            idToInstance.put(group.getID(), group);
        }
    }

    public Map<String, ReportData> generate() throws SQLException, SQLHandledException {
        iterateChildReports(hierarchy.getRootNodes(), SetFact.<GroupObjectInstance>EMPTYORDER(), null);
        return sources;
    }

    private Query<ObjectInstance, Pair<Object, PropertyType>> createQuery(ImOrderSet<GroupObjectInstance> groups, ImOrderSet<GroupObjectInstance> parentGroups, SessionTableUsage<ObjectInstance,
            Pair<Object, PropertyType>> parentTable, Result<ImOrderMap<Pair<Object, PropertyType>, Boolean>> orders, Result<ImMap<Pair<Object, PropertyType>, Type>> types) throws SQLException, SQLHandledException {

        assert parentTable == null || BaseUtils.hashEquals(GroupObjectInstance.getObjects(parentGroups.getSet()),parentTable.getMapKeys().keys());

        QueryBuilder<ObjectInstance, Pair<Object, PropertyType>> newQuery;
        if (groupId == null) {
            newQuery = new QueryBuilder<>(GroupObjectInstance.getObjects(groups.getSet()));
        } else {
            GroupObjectInstance ourGroup = null;
            for (GroupObjectInstance group : groups) {
                if (groupId.equals(group.getID())) {
                    ourGroup = group;
                    break;
                }
            }
            assert ourGroup != null;
            newQuery = new QueryBuilder<>(ourGroup.objects);
        }
        MExclMap<Pair<Object, PropertyType>, Type> mTypes = MapFact.mExclMap();
        MOrderExclMap<Pair<Object, PropertyType>, Boolean> mOrders = MapFact.mOrderExclMap();

        boolean subReportTableOptimization = Settings.get().isSubReportTableOptimization();
        Join<Pair<Object, PropertyType>> parentJoin = null;
        if(parentTable != null) {
            parentJoin = parentTable.join(newQuery.getMapExprs());
        }

        if (parentJoin != null) {
            newQuery.and(parentJoin.getWhere());
        }

        Modifier modifier = form.getModifier();
        for (GroupObjectInstance group : groups) {
            if (groupId == null || groupId.equals(group.getID())) {
                boolean inParent = parentGroups.contains(group);
                if(!(subReportTableOptimization && inParent)) {
                    newQuery.and(group.getWhere(newQuery.getMapExprs(), modifier));

                    if (!gridGroupsId.contains(group.getID())) {
                        for (ObjectInstance object : group.objects) {
                            newQuery.and(object.getExpr(newQuery.getMapExprs(), modifier).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                        }
                    }
                }

                ImOrderMap<OrderInstance,Boolean> mergeOrders = group.orders.mergeOrder(group.getOrderObjects().toOrderMap(false));
                for(int i=0,size=mergeOrders.size();i<size;i++) {
                    OrderInstance order = mergeOrders.getKey(i);

                    Pair<Object, PropertyType> orderObject = addProperty(order, order, PropertyType.ORDER, modifier, inParent, parentJoin, newQuery, mTypes);
                    mOrders.exclAdd(orderObject, mergeOrders.getValue(i));
                }
            }
        }

        Set<PropertyDrawInstance> parentProps = subReportTableOptimization ? new HashSet<>(filterProperties(parentGroups.getSet())) : new HashSet<PropertyDrawInstance>();
        for(PropertyDrawInstance<?> property : filterProperties(groups.getSet())) {
            boolean inParent = parentProps.contains(property);
            if (property.getColumnGroupObjects().isEmpty()) {
                addProperty(property, property.getDrawInstance(), PropertyType.PLAIN, modifier, inParent, parentJoin, newQuery, mTypes);

                if (property.propertyCaption != null) {
                    addProperty(property, property.propertyCaption, PropertyType.CAPTION, modifier, inParent, parentJoin, newQuery, mTypes);
                }

                if (property.propertyFooter != null) {
                    addProperty(property, property.propertyFooter, PropertyType.FOOTER, modifier, inParent, parentJoin, newQuery, mTypes);
                }
            }
        }
        types.set(mTypes.immutable());
        orders.set(mOrders.immutableOrder());
        return newQuery.getQuery();
    }

    private Pair<Object, PropertyType> addProperty(Object property, OrderInstance pObject, PropertyType type, Modifier modifier, boolean inParent, Join<Pair<Object, PropertyType>> parentJoin, QueryBuilder<ObjectInstance, Pair<Object, PropertyType>> newQuery, MExclMap<Pair<Object, PropertyType>, Type> mTypes) throws SQLException, SQLHandledException {
        Pair<Object, PropertyType> propertyObject = new Pair<Object, PropertyType>(property, type);
        newQuery.addProperty(propertyObject, inParent ? parentJoin.getExpr(propertyObject) : pObject.getExpr(newQuery.getMapExprs(), modifier));
        mTypes.exclAdd(propertyObject, pObject.getType());
        return propertyObject;
    }

    private void iterateChildReports(List<ReportNode> children, ImOrderSet<GroupObjectInstance> parentGroups, SessionTableUsage<ObjectInstance, Pair<Object, PropertyType>> parentTable) throws SQLException, SQLHandledException {
        for (ReportNode node : children) {
            String sid = node.getID();
            List<GroupObjectEntity> groupList = node.getGroupList();
            MOrderExclSet<GroupObjectInstance> mLocalGroups = SetFact.mOrderExclSet(groupList.size()); // пограничные List'ы
            for (GroupObjectEntity group : groupList) {
                GroupObjectInstance groupInstance = idToInstance.get(group.getID());
                mLocalGroups.exclAdd(groupInstance);
            }

            ImOrderSet<GroupObjectInstance> groups = parentGroups.mergeOrder(mLocalGroups.immutableOrder()); // тут хрен поймешь excl или нет

            Result<ImOrderMap<Pair<Object, PropertyType>, Boolean>> orders = new Result<>();
            Result<ImMap<Pair<Object, PropertyType>, Type>> propTypes = new Result<>();
            ImMap<ObjectInstance, Type> keyTypes = GroupObjectInstance.getObjects(groups.getSet()).mapValues(new GetValue<Type, ObjectInstance>() {
                public Type getMapValue(ObjectInstance value) {
                    return value.getType();
                }});

            Query<ObjectInstance, Pair<Object, PropertyType>> query = createQuery(groups, parentGroups, parentTable, orders, propTypes);
            SessionTableUsage<ObjectInstance, Pair<Object, PropertyType>> reportTable = new SessionTableUsage<>(
                    form.session.sql, query, form.BL.LM.baseClass, form.getQueryEnv(), keyTypes, propTypes.result);

            try {
                ImOrderMap<ImMap<ObjectInstance, Object>, ImMap<Pair<Object, PropertyType>, Object>> resultData = reportTable.read(form, orders.result);

                List<Pair<String, PropertyReaderInstance>> propertyList = new ArrayList<>();
                for(PropertyDrawInstance property : filterProperties(groups.getSet())) {
                    propertyList.add(new Pair<String, PropertyReaderInstance>(property.getsID(), property));
                    if (property.propertyCaption != null) {
                        propertyList.add(new Pair<String, PropertyReaderInstance>(property.getsID(), property.captionReader));
                    }
                    if (property.propertyFooter != null) {
                        propertyList.add(new Pair<String, PropertyReaderInstance>(property.getsID(), property.footerReader));
                    }
                }

                ImOrderSet<ObjectInstance> keyList = GroupObjectInstance.getOrderObjects(groups);
                ReportData data = new ReportData(keyList.toJavaList(), propertyList);

                for (int i=0,size=resultData.size();i<size;i++) {
                    ImMap<Pair<Object, PropertyType>, Object> resultValue = resultData.getValue(i);

                    List<Object> propertyValues = new ArrayList<>();
                    for(PropertyDrawInstance property : filterProperties(groups.getSet())) {
                        propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.PLAIN)));
                        if (property.propertyCaption != null) {
                            propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.CAPTION)));
                        }
                        if (property.propertyFooter != null) {
                            propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.FOOTER)));
                        }
                    }

                    List<ObjectInstance> objectsList = keyList.toJavaList(); 
                    List<Object> keys = BaseUtils.mapList(objectsList, resultData.getKey(i));
                    if (groupId != null) {
                        for (int keyIndex = 0; keyIndex < objectsList.size(); ++keyIndex) {
                            if (resultData.getKey(i).get(objectsList.get(keyIndex)) == null) {
                                keys.set(keyIndex, objectsList.get(keyIndex).getObjectValue().getValue());
                            }
                        }
                    }
                    data.add(keys, propertyValues);
                }

                sources.put(sid, data);

                iterateChildReports(hierarchy.getChildNodes(node), groups, reportTable);
            } finally {
                reportTable.drop(form.session.sql, form.session.getOwner());
            }
        }
    }

    // В отчет по одной группе объектов не добавляем свойства, которые идут в панель  
    private boolean validForGroupReports(PropertyDrawInstance property) {
        return groupId == null || !groupId.equals(property.toDraw.getID()) || property.getForceViewType() != ClassViewType.PANEL;     
    } 
    
    private List<PropertyDrawInstance> filterProperties(ImSet<GroupObjectInstance> filterGroups) {
        List<PropertyDrawInstance> resultList = new ArrayList<>();
        for (PropertyDrawInstance property : form.properties) {
            GroupObjectInstance applyGroup = property.propertyObject.getApplyObject();
            // Отдельно рассматриваем случай свойства без параметров
            if (((applyGroup == null || property.toDraw == applyGroup) && property.toDraw != null && filterGroups.contains(property.toDraw) && validForGroupReports(property)) ||  
                (property.toDraw == null && applyGroup == null && property.propertyObject.property instanceof CalcProperty && property.propertyObject.property.getOrderInterfaces().isEmpty())) {
                boolean add = true;
                
                if (userPreferences != null && property.toDraw != null) {
                    GroupObjectUserPreferences groupObjectPreferences = userPreferences.getUsedPreferences(property.toDraw.getSID());
                    if (groupObjectPreferences != null) {
                        ColumnUserPreferences columnUP = groupObjectPreferences.getColumnUserPreferences().get(property.getsID());
                        if (columnUP != null && columnUP.userHide != null && columnUP.userHide) {
                            add = false;    
                        }
                    }
                }
                if (add) {
                    resultList.add(property);
                }
            }
        }
        return resultList;
    }

    public ColumnGroupCaptionsData getColumnGroupCaptions() throws SQLException, SQLHandledException {
        ColumnGroupCaptionsData resultData = new ColumnGroupCaptionsData();

        for (PropertyDrawInstance<?> property : form.properties) {
            ImOrderSet<GroupObjectInstance> columnGroupObjects = property.getOrderColumnGroupObjects();
            if (columnGroupObjects.size() > 0) {
                ImOrderSet<GroupObjectInstance> groups = getNeededGroupsForColumnProp(property);
                ImOrderMap<ImMap<ObjectInstance, Object>, ImMap<Object, Object>> qResult = getColumnPropQueryResult(groups, property);

                ImCol<ObjectInstance> propObjects = property.propertyObject.getObjectInstances();

                Map<List<Object>, Object> data = new HashMap<>();
                Map<List<Object>, Object> captionData = new HashMap<>();
                Map<List<Object>, Object> footerData = new HashMap<>();
                LinkedHashSet<List<Object>> columnData = new LinkedHashSet<>();

                for (int i=0,size=qResult.size();i<size;i++) {
                    ImMap<ObjectInstance, Object> key = qResult.getKey(i); ImMap<Object, Object> value = qResult.getValue(i);

                    List<Object> values = new ArrayList<>();
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

                    List<Object> columnValues = new ArrayList<>();
                    for (ObjectInstance object : GroupObjectInstance.getOrderObjects(columnGroupObjects)) {
                        columnValues.add(key.get(object));
                    }
                    columnData.add(columnValues);
                }

                resultData.propertyObjects.put(property.getsID(), propObjects.toList().toJavaList());
                resultData.columnObjects.put(property.getsID(), GroupObjectInstance.getOrderObjects(columnGroupObjects).toJavaList());
                resultData.data.put(property.getsID(), data);
                if (property.propertyCaption != null) {
                    resultData.data.put(property.getsID() + ReportConstants.headerSuffix, captionData);
                }
                if (property.propertyFooter != null) {
                    resultData.data.put(property.getsID() + ReportConstants.footerSuffix, footerData);
                }
                resultData.columnData.put(property.getsID(), columnData);
            }
        }
        return resultData;
    }

    private ImOrderMap<ImMap<ObjectInstance, Object>, ImMap<Object, Object>> getColumnPropQueryResult(ImOrderSet<GroupObjectInstance> groups, PropertyDrawInstance<?> property) throws SQLException, SQLHandledException {
        ImSet<ObjectInstance> objects = GroupObjectInstance.getObjects(groups.getSet());
        QueryBuilder<ObjectInstance, Object> query = new QueryBuilder<>(objects);
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

            if (group.curClassView != ClassViewType.GRID || (groupId != null && !(groupId.equals(group.getID()) || property.getColumnGroupObjects().contains(group)))) {
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
        Set<GroupObjectInstance> groups = new HashSet<>();
        for (ObjectInstance object : property.propertyObject.getObjectInstances()) {
            groups.add(object.groupTo);
        }
        return groups;
    }

    // получает все группы, от которых зависит свойство
    // включаются и дополнительные объекты, которые не используются (в частности fullFormHierarchy), но нужны чтобы найти ВСЕ разновидности групп в колонки
    // это конечно "избыточно", но пока так 
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
