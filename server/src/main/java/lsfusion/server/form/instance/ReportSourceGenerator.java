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
import lsfusion.interop.form.*;
import lsfusion.server.Settings;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.GroupObjectHierarchy;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.SessionTableUsage;
import lsfusion.server.stack.ParamMessage;
import lsfusion.server.stack.StackMessage;

import java.sql.SQLException;
import java.util.*;

import static lsfusion.server.form.entity.GroupObjectHierarchy.ReportNode;

/**
 * User: DAle
 * Date: 12.08.2010
 * Time: 10:50:30
 */

public class ReportSourceGenerator<PropertyDraw extends PropertyReaderInstance, GroupObject, PropertyObject, CalcPropertyObject extends Order, Order, Obj extends Order, PropertyReaderInstance>  {
    // Иерархия генерируемого отчета. Отличается от полной иерархии при отчете для одной таблицы
    private GroupObjectHierarchy.ReportHierarchy hierarchy;
    // Полная иерархия формы
    private GroupObjectHierarchy.ReportHierarchy fullFormHierarchy;

    private final FormSourceInterface<PropertyDraw, GroupObject, PropertyObject, CalcPropertyObject, Order, Obj, PropertyReaderInstance> formInterface;

    private Map<String, ReportData> sources = new HashMap<>();
    // ID группы при отчете для одной таблицы
    private final Integer groupId;
    private FormUserPreferences userPreferences;
    // ID тех групп, которые идут в отчет таблицей значений.
    private final Set<Integer> gridGroupsId;
    private Map<Integer, GroupObject> idToInstance = new HashMap<>();

    public static class ColumnGroupCaptionsData<Obj> {
        // объекты, от которых зависит свойство
        public final Map<String, List<Obj>> propertyObjects = new HashMap<>();
        // объекты, идущие в колонки
        public final Map<String, List<Obj>> columnObjects = new HashMap<>();
        // таблицы значений свойств, ключ в таблице - набор значений объектов из propertyObjects
        public final Map<String, Map<List<Object>, Object>> data = new HashMap<>();
        // наборы различных значений объектов из columnObjects, идущих в колонки
        public final Map<String, LinkedHashSet<List<Object>>> columnData = new HashMap<>();
    }
    
    public ReportSourceGenerator(FormSourceInterface<PropertyDraw, GroupObject, PropertyObject, CalcPropertyObject, Order, Obj, PropertyReaderInstance> formInterface, GroupObjectHierarchy.ReportHierarchy hierarchy,
                                 GroupObjectHierarchy.ReportHierarchy fullFormHierarchy, Set<Integer> gridGroupsId, Integer groupId, FormUserPreferences userPreferences) {
        this.hierarchy = hierarchy;
        this.fullFormHierarchy = fullFormHierarchy;
        this.formInterface = formInterface;
        this.gridGroupsId = gridGroupsId;
        this.groupId = groupId;
        this.userPreferences = userPreferences;

        for (GroupObject group : formInterface.getOrderGroups()) {
            idToInstance.put(formInterface.getGroupID(group), group);
        }
    }

    public Map<String, ReportData> generate(ReportGenerationDataType reportType) throws SQLException, SQLHandledException {
        return generate(reportType, 0);
    }
    public Map<String, ReportData> generate(ReportGenerationDataType reportType, int selectTop) throws SQLException, SQLHandledException {
        iterateChildReports(hierarchy.getRootNodes(), SetFact.<GroupObject>EMPTYORDER(), null, reportType, selectTop);
        return sources;
    }

    private Query<Obj, Pair<Object, PropertyType>> createQuery(ImOrderSet<GroupObject> groups, ImOrderSet<GroupObject> parentGroups, SessionTableUsage<Obj,
            Pair<Object, PropertyType>> parentTable, Result<ImOrderMap<Pair<Object, PropertyType>, Boolean>> orders, Result<ImMap<Pair<Object, PropertyType>, Type>> types, ReportGenerationDataType reportType) throws SQLException, SQLHandledException {

        assert parentTable == null || BaseUtils.hashEquals(formInterface.getObjects(parentGroups.getSet()),parentTable.getMapKeys().keys());

        QueryBuilder<Obj, Pair<Object, PropertyType>> newQuery;
        if (groupId == null) {
            newQuery = new QueryBuilder<>(formInterface.getObjects(groups.getSet()));
        } else {
            GroupObject ourGroup = null;
            for (GroupObject group : groups) {
                if (groupId.equals(formInterface.getGroupID(group))) {
                    ourGroup = group;
                    break;
                }
            }
            assert ourGroup != null;
            newQuery = new QueryBuilder<>(formInterface.getObjects(ourGroup));
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

        for (GroupObject group : groups) {
            int groupObjectID = formInterface.getGroupID(group);
            if (groupId == null || groupId.equals(groupObjectID)) {
                boolean inParent = parentGroups.contains(group);
                if(!(subReportTableOptimization && inParent)) {
                    newQuery.and(formInterface.getWhere(group, newQuery.getMapExprs()));

                    if (!gridGroupsId.contains(groupObjectID)) {
                        for (Obj object : formInterface.getObjects(group)) {
                            newQuery.and(formInterface.getExpr(object, newQuery.getMapExprs()).compare(formInterface.getObjectValue(object).getExpr(), Compare.EQUALS));
                        }
                    }
                }

                ImOrderMap<Order,Boolean> mergeOrders = formInterface.getOrders(group).mergeOrder(formInterface.getOrderObjects(group).toOrderMap(false));
                for(int i=0,size=mergeOrders.size();i<size;i++) {
                    Order order = mergeOrders.getKey(i);

                    Pair<Object, PropertyType> orderObject = addProperty(order, order, PropertyType.ORDER, inParent, parentJoin, newQuery, mTypes);
                    mOrders.exclAdd(orderObject, mergeOrders.getValue(i));
                }
            }
        }

        Set<PropertyDraw> parentProps = subReportTableOptimization && parentJoin != null ? new HashSet<>(filterProperties(parentGroups.getSet(), reportType)) : new HashSet<PropertyDraw>();
        for(PropertyDraw property : filterProperties(groups.getSet(), reportType)) {
            boolean inParent = parentProps.contains(property);
            if (formInterface.getColumnGroupObjects(property).isEmpty()) {
                addProperty(property, formInterface.getDrawInstance(property), PropertyType.PLAIN, inParent, parentJoin, newQuery, mTypes);

                CalcPropertyObject propertyCaption = formInterface.getPropertyCaption(property);
                if (propertyCaption != null) {
                    addProperty(property, propertyCaption, PropertyType.CAPTION, inParent, parentJoin, newQuery, mTypes);
                }

                CalcPropertyObject propertyFooter = formInterface.getPropertyFooter(property);
                if (propertyFooter != null) {
                    addProperty(property, propertyFooter, PropertyType.FOOTER, inParent, parentJoin, newQuery, mTypes);
                }
            }
        }
        types.set(mTypes.immutable());
        orders.set(mOrders.immutableOrder());
        return newQuery.getQuery();
    }

    private Pair<Object, PropertyType> addProperty(Object property, Order pObject, PropertyType type, boolean inParent, Join<Pair<Object, PropertyType>> parentJoin, QueryBuilder<Obj, Pair<Object, PropertyType>> newQuery, MExclMap<Pair<Object, PropertyType>, Type> mTypes) throws SQLException, SQLHandledException {
        Pair<Object, PropertyType> propertyObject = new Pair<>(property, type);
        newQuery.addProperty(propertyObject, inParent ? parentJoin.getExpr(propertyObject) : formInterface.getExpr(pObject, newQuery.getMapExprs()));
        mTypes.exclAdd(propertyObject, formInterface.getType(pObject));
        return propertyObject;
    }

    private void iterateChildReports(List<ReportNode> children, ImOrderSet<GroupObject> parentGroups, SessionTableUsage<Obj, Pair<Object, PropertyType>> parentTable, ReportGenerationDataType reportType, int selectTop) throws SQLException, SQLHandledException {
        for (ReportNode node : children) {
            iterateChildReport(node, parentGroups, parentTable, reportType, selectTop);
        }
    }

    @StackMessage("{message.form.read.report.node}")
    private void iterateChildReport(@ParamMessage ReportNode node, ImOrderSet<GroupObject> parentGroups, SessionTableUsage<Obj, Pair<Object, PropertyType>> parentTable, ReportGenerationDataType reportType, int selectTop) throws SQLException, SQLHandledException {
        String sid = node.getID();
        List<GroupObjectEntity> groupList = node.getGroupList();
        MOrderExclSet<GroupObject> mLocalGroups = SetFact.mOrderExclSet(groupList.size()); // пограничные List'ы
        for (GroupObjectEntity group : groupList) {
            GroupObject groupInstance = idToInstance.get(group.getID());
            mLocalGroups.exclAdd(groupInstance);
        }

        ImOrderSet<GroupObject> groups = parentGroups.mergeOrder(mLocalGroups.immutableOrder()); // тут хрен поймешь excl или нет

        Result<ImOrderMap<Pair<Object, PropertyType>, Boolean>> orders = new Result<>();
        Result<ImMap<Pair<Object, PropertyType>, Type>> propTypes = new Result<>();
        ImMap<Obj, Type> keyTypes = formInterface.getObjects(groups.getSet()).mapValues(new GetValue<Type, Obj>() {
            public Type getMapValue(Obj value) {
                return formInterface.getType(value);
            }});

        Query<Obj, Pair<Object, PropertyType>> query = createQuery(groups, parentGroups, parentTable, orders, propTypes, reportType);
        DataSession session = getSession();
        QueryEnvironment queryEnv = getQueryEnv();
        BaseClass baseClass = getBaseClass();
        SQLSession sql = session.sql;
        SessionTableUsage<Obj, Pair<Object, PropertyType>> reportTable = new SessionTableUsage<>(
                sql, query, baseClass, queryEnv, keyTypes, propTypes.result);

        try {
            ImOrderMap<ImMap<Obj, Object>, ImMap<Pair<Object, PropertyType>, Object>> resultData = reportTable.read(sql, getQueryEnv(), orders.result, selectTop);

            List<Pair<String, PropertyReaderInstance>> propertyList = new ArrayList<>();
            for(PropertyDraw property : filterProperties(groups.getSet(), reportType)) {
                String psid = formInterface.getPSID(property);
                propertyList.add(new Pair<String, PropertyReaderInstance>(psid, property));
                if (formInterface.getPropertyCaption(property) != null) {
                    propertyList.add(new Pair<>(psid, formInterface.getCaptionReader(property)));
                }
                if (formInterface.getPropertyFooter(property) != null) {
                    propertyList.add(new Pair<>(psid, formInterface.getFooterReader(property)));
                }
            }

            ImOrderSet<Obj> keyList = formInterface.getOrderObjects(groups);
            ReportData<Order, Obj, PropertyReaderInstance> data = new ReportData<>(keyList.toJavaList(), propertyList);

            for (int i=0,size=resultData.size();i<size;i++) {
                ImMap<Pair<Object, PropertyType>, Object> resultValue = resultData.getValue(i);

                List<Object> propertyValues = new ArrayList<>();
                for(PropertyDraw property : filterProperties(groups.getSet(), reportType)) {
                    propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.PLAIN)));
                    if (formInterface.getPropertyCaption(property) != null) {
                        propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.CAPTION)));
                    }
                    if (formInterface.getPropertyFooter(property) != null) {
                        propertyValues.add(resultValue.get(new Pair<Object, PropertyType>(property, PropertyType.FOOTER)));
                    }
                }

                List<Obj> objectsList = keyList.toJavaList();
                List<Object> keys = BaseUtils.mapList(objectsList, resultData.getKey(i));
                if (groupId != null) {
                    for (int keyIndex = 0; keyIndex < objectsList.size(); ++keyIndex) {
                        if (resultData.getKey(i).get(objectsList.get(keyIndex)) == null) {
                            keys.set(keyIndex, formInterface.getObjectValue(objectsList.get(keyIndex)).getValue());
                        }
                    }
                }
                data.add(keys, propertyValues);
            }

            sources.put(sid, data);

            iterateChildReports(hierarchy.getChildNodes(node), groups, reportTable, reportType, selectTop);
        } finally {
            reportTable.drop(sql, session.getOwner());
        }
    }

    private BaseClass getBaseClass() {
        return formInterface.getBaseClass();
    }

    private QueryEnvironment getQueryEnv() {
        return formInterface.getQueryEnv();
    }

    private DataSession getSession() {
        return formInterface.getSession();
    }

    // В отчет по одной группе объектов не добавляем свойства, которые идут в панель
    private boolean validForGroupReports(PropertyDraw property) {
        ClassViewType pViewType;
        return !(groupId != null && groupId.equals(formInterface.getGroupID(formInterface.getToDraw(property))) && (pViewType=formInterface.getPViewType(property))!= null && pViewType.isPanel());
    } 
    
    private List<PropertyDraw> filterProperties(ImSet<GroupObject> filterGroups, ReportGenerationDataType reportType) {
        List<PropertyDraw> resultList = new ArrayList<>();
        for (PropertyDraw property : formInterface.getProperties()) {
            GroupObject applyGroup = formInterface.getApplyObject(formInterface.getPropertyObject(property));
            // Отдельно рассматриваем случай свойства без параметров
            GroupObject toDraw = formInterface.getToDraw(property);
            Property pProperty;
            if (((applyGroup == null || toDraw == applyGroup) && toDraw != null && filterGroups.contains(toDraw) && validForGroupReports(property)) ||
                (toDraw == null && applyGroup == null && (pProperty = formInterface.getProperty(formInterface.getPropertyObject(property))) instanceof CalcProperty && pProperty.getInterfaceCount() == 0) ||
                (toDraw != applyGroup && !reportType.isDefault())) {
                boolean add = true;
                
                if (userPreferences != null && toDraw != null) {
                    GroupObjectUserPreferences groupObjectPreferences = userPreferences.getUsedPreferences(formInterface.getGroupSID(toDraw));
                    if (groupObjectPreferences != null) {
                        ColumnUserPreferences columnUP = groupObjectPreferences.getColumnUserPreferences().get(formInterface.getPSID(property));
                        if (columnUP != null && columnUP.userHide != null && columnUP.userHide) {
                            add = false;    
                        }
                    }
                }
                
                if (groupId != null && !formInterface.isPropertyShown(property)) {
                    add = false;
                }
                
                if (add) {
                    resultList.add(property);
                }
            }
        }
        return resultList;
    }

    public ColumnGroupCaptionsData<Obj> getColumnGroupCaptions() throws SQLException, SQLHandledException {
        ColumnGroupCaptionsData<Obj> resultData = new ColumnGroupCaptionsData<>();

        for (PropertyDraw property : formInterface.getProperties()) {
            ImOrderSet<GroupObject> columnGroupObjects = formInterface.getOrderColumnGroupObjects(property);
            if (columnGroupObjects.size() > 0) {
                ImOrderSet<GroupObject> groups = getNeededGroupsForColumnProp(property);
                ImOrderMap<ImMap<Obj, Object>, ImMap<Object, Object>> qResult = getColumnPropQueryResult(groups, property);

                ImCol<Obj> propObjects = formInterface.getPObjects(formInterface.getPropertyObject(property));

                Map<List<Object>, Object> data = new HashMap<>();
                Map<List<Object>, Object> captionData = new HashMap<>();
                Map<List<Object>, Object> footerData = new HashMap<>();
                LinkedHashSet<List<Object>> columnData = new LinkedHashSet<>();

                for (int i=0,size=qResult.size();i<size;i++) {
                    ImMap<Obj, Object> key = qResult.getKey(i); ImMap<Object, Object> value = qResult.getValue(i);

                    List<Object> values = new ArrayList<>();
                    for (Obj object : propObjects) {
                        if (key.containsKey(object)) {
                            values.add(key.get(object));
                        } else {
                            values.add(formInterface.getObjectValue(object).getValue());
                        }
                    }
                    data.put(values, value.get(property));
                    if (formInterface.getPropertyCaption(property) != null) {
                        captionData.put(values, value.get(formInterface.getCaptionReader(property)));
                    }
                    if (formInterface.getPropertyFooter(property) != null) {
                        footerData.put(values, value.get(formInterface.getFooterReader(property)));
                    }

                    List<Object> columnValues = new ArrayList<>();
                    for (Obj object : formInterface.getOrderObjects(columnGroupObjects)) {
                        if (key.containsKey(object)) {
                            columnValues.add(key.get(object));
                        } else {
                            assert false;
//                            columnValues.add(formInterface.getObjectValue(object).getValue());
                        }
                    }
                    columnData.add(columnValues);
                }

                String psid = formInterface.getPSID(property);
                resultData.propertyObjects.put(psid, propObjects.toList().toJavaList());
                resultData.columnObjects.put(psid, formInterface.getOrderObjects(columnGroupObjects).toJavaList());
                resultData.data.put(psid, data);
                if (formInterface.getPropertyCaption(property) != null) {
                    resultData.data.put(psid + ReportConstants.headerSuffix, captionData);
                }
                if (formInterface.getPropertyFooter(property) != null) {
                    resultData.data.put(psid + ReportConstants.footerSuffix, footerData);
                }
                resultData.columnData.put(psid, columnData);
            }
        }
        return resultData;
    }

    private ImOrderMap<ImMap<Obj, Object>, ImMap<Object, Object>> getColumnPropQueryResult(ImOrderSet<GroupObject> groups, PropertyDraw property) throws SQLException, SQLHandledException {
        ImOrderSet<GroupObject> keyGroupObjects = groups;
        if (groupId != null) {
            MOrderSet<GroupObject> queryGroups = SetFact.mOrderSet();
            for (GroupObject group : groups) {
                if (groupId.equals(formInterface.getGroupID(group)) || formInterface.getColumnGroupObjects(property).contains(group)) {
                    queryGroups.add(group);
                }
            }
            keyGroupObjects = queryGroups.immutableOrder();
        }
        
        ImSet<Obj> objects = formInterface.getObjects(keyGroupObjects.getSet());
        QueryBuilder<Obj, Object> query = new QueryBuilder<>(objects);
        
        MOrderMap<Object, Boolean> mQueryOrders = MapFact.mOrderMap();
        for (GroupObject group : keyGroupObjects) {
            query.and(formInterface.getWhere(group, query.getMapExprs()));

            ImOrderMap<Order, Boolean> groupOrders = formInterface.getOrders(group).mergeOrder(formInterface.getOrderObjects(group).toOrderMap(false));
            for (int i=0,size=groupOrders.size();i<size;i++) {
                Order order = groupOrders.getKey(i);
                query.addProperty(order, formInterface.getExpr(order, query.getMapExprs()));
                mQueryOrders.add(order, groupOrders.getValue(i));
            }

            if (formInterface.getGroupViewType(group).isPanel()) {
                for (Obj object : formInterface.getObjects(group)) {
                    query.and(formInterface.getExpr(object, query.getMapExprs()).compare(formInterface.getObjectValue(object).getExpr(), Compare.EQUALS));
                }
            }
        }

        query.addProperty(property, formInterface.getExpr(formInterface.getDrawInstance(property), query.getMapExprs()));
        CalcPropertyObject propertyCaption = formInterface.getPropertyCaption(property);
        if (propertyCaption != null) {
            query.addProperty(formInterface.getCaptionReader(property), formInterface.getExpr(propertyCaption, query.getMapExprs()));
        }
        CalcPropertyObject propertyFooter = formInterface.getPropertyFooter(property);
        if (propertyFooter != null) {
            query.addProperty(formInterface.getFooterReader(property), formInterface.getExpr(propertyFooter, query.getMapExprs()));
        }

        return query.execute(getSession().sql, mQueryOrders.immutableOrder(), 0, getQueryEnv());
    }

    private Set<GroupObject> getPropertyDependencies(PropertyDraw property) {
        Set<GroupObject> groups = new HashSet<>();
        for (Obj object : formInterface.getPObjects(formInterface.getPropertyObject(property))) {
            groups.add(formInterface.getGroupTo(object));
        }
        return groups;
    }

    // получает все группы, от которых зависит свойство
    // включаются и дополнительные объекты, которые не используются (в частности fullFormHierarchy), но нужны чтобы найти ВСЕ разновидности групп в колонки
    // это конечно "избыточно", но пока так 
    private ImOrderSet<GroupObject> getNeededGroupsForColumnProp(PropertyDraw property) {
        Set<GroupObject> initialGroups = getPropertyDependencies(property);
        MSet<GroupObject> groups = SetFact.mSet();
        
        for (GroupObject group : initialGroups) {
            GroupObjectEntity groupEntity = formInterface.getEntity(group);
            ReportNode curNode = fullFormHierarchy.getReportNode(groupEntity);
            List<GroupObjectEntity> nodeGroups = curNode.getGroupList();
            int groupIndex = nodeGroups.indexOf(groupEntity);
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

        return formInterface.getOrderGroups().filterOrderIncl(groups.immutable());
    }

    public enum PropertyType {PLAIN, ORDER, CAPTION, FOOTER, BACKGROUND}
}
