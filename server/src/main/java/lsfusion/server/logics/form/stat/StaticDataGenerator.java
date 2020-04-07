package lsfusion.server.logics.form.stat;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.controller.stack.ParamMessage;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.form.stat.struct.hierarchy.ParseNode;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.CompareEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class StaticDataGenerator<SDP extends PropertyReaderEntity> {
    
    protected final FormDataInterface formInterface;
    private final Hierarchy hierarchy;
    
    private final boolean supportColumnGroups;

    public StaticDataGenerator(FormDataInterface formInterface, Hierarchy hierarchy, boolean supportColumnGroups) {
        this.formInterface = formInterface;
        this.hierarchy = hierarchy;
        this.supportColumnGroups = supportColumnGroups; 
    }

    public static class ReportHierarchy {
        public final GroupObjectHierarchy.ReportHierarchy reportHierarchy;
        public final Hierarchy hierarchy;
        
        public ReportHierarchy(GroupObjectHierarchy.ReportHierarchy reportHierarchy, Hierarchy hierarchy) {
            this.reportHierarchy = reportHierarchy;
            this.hierarchy = hierarchy;
        }
    }
        
    public static class Hierarchy {
        private final GroupObjectHierarchy groupHierarchy;
        private final ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> properties;
        private final ImSet<GroupObjectEntity> valueGroups; // all groups that are not in groupHierarchy

        public StaticDataGenerator.ReportHierarchy getReportHierarchy() {
            ImSet<GroupObjectEntity> shallowGroups = Settings.get().getBackwardCompatibilityVersion() > 3 ? properties.filterFnValues(ImList::isEmpty).keys() : SetFact.EMPTY();
            return new StaticDataGenerator.ReportHierarchy(groupHierarchy.getReportHierarchy(shallowGroups), this);
        }
        
        @IdentityInstanceLazy
        public ParseNode getIntegrationHierarchy() {
            return ParseNode.getIntegrationHierarchy(this);
        }
        
        public GroupObjectEntity getParentGroup(GroupObjectEntity group) {
            return groupHierarchy.getParentGroup(group);
        }
        
        public GroupObjectEntity getRoot() {
            return groupHierarchy.getRoot();
        }
        
        public ImOrderSet<GroupObjectEntity> getDependencies(GroupObjectEntity group) {
            return groupHierarchy.getDependencies(group);
        }
        
        public Iterable<ImOrderSet<PropertyDrawEntity>> getAllProperties() {
            return properties.valueIt();
        }
        
        public ImOrderSet<PropertyDrawEntity> getProperties(GroupObjectEntity group) {
            ImOrderSet<PropertyDrawEntity> groupProperties = properties.get(group != null ? group : GroupObjectEntity.NULL);
            if(groupProperties != null)
                return groupProperties;
            return SetFact.EMPTYORDER();
        }

        public Hierarchy(GroupObjectHierarchy groupHierarchy, ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> properties, ImSet<GroupObjectEntity> valueGroups) {
            this.groupHierarchy = groupHierarchy;
            this.properties = properties;
            this.valueGroups = valueGroups;
        }
        
        public ImSet<GroupObjectEntity> getValueGroups() {
            return valueGroups;
        }
    }

    public Pair<Map<GroupObjectEntity, StaticKeyData>, StaticPropertyData<SDP>> generate(int selectTop) throws SQLException, SQLHandledException {
        Map<GroupObjectEntity, StaticKeyData> keySources = new HashMap<>();
        StaticPropertyData<SDP> propSources = new StaticPropertyData<>();
        iterateChildGroup(hierarchy.getRoot(),  SetFact.EMPTYORDER(), MapFact.EMPTYORDER(), null, selectTop, keySources, propSources, hierarchy.getValueGroups());
        return new Pair<>(keySources, propSources);
    }
    
    protected void fillQueryProps(PropertyDrawEntity property, MExclSet<SDP> mResult) {
        mResult.exclAdd((SDP) property);        
    }

    protected ImMap<ObjectEntity, Expr> addObjectValues(ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) {
        return ObjectValue.getMapExprs(formInterface.getObjectValues(valueGroups)).override(mapExprs); // override and not exclusive because column group objects can also be value groups 
    }
    protected Where getWhere(GroupObjectEntity group, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException {
        return formInterface.getWhere(group, valueGroups, mapExprs).and(formInterface.getValueWhere(group, valueGroups, mapExprs));
    }
    protected ImOrderMap<CompareEntity, Boolean> getOrders(GroupObjectEntity group, ImSet<GroupObjectEntity> valueGroups) {
        return formInterface.getOrders(group, valueGroups).mergeOrder(group.getOrderObjects().toOrderMap(false)).mapOrderKeys(new Function<CompareEntity, CompareEntity>() {
            public CompareEntity apply(final CompareEntity value) {
                if(value instanceof ObjectEntity) // hack, need this because in Query keys and values should not intersect (because of ClassWhere), but CompareEntity and ObjectEntity have common class ObjectEntity
                    return new CompareEntity() {
                        public Type getType() {
                            return value.getType();
                        }
                        public Expr getEntityExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException {
                            return value.getEntityExpr(mapExprs, modifier);
                        }
                    };
                return value;
            }
        });
    }

    protected Where getWhere(ImSet<GroupObjectEntity> groups, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException {
        Where where = Where.TRUE();
        for(GroupObjectEntity group : groups)
            where = where.and(getWhere(group, valueGroups, mapExprs));
        return where;
    }
    protected ImOrderMap<CompareEntity, Boolean> getOrders(ImOrderSet<GroupObjectEntity> groups, ImSet<GroupObjectEntity> valueGroups) {
        MOrderExclMap<CompareEntity, Boolean> mOrders = MapFact.mOrderExclMap();
        for(GroupObjectEntity group : groups)
            mOrders.exclAddAll(getOrders(group, valueGroups));
        return mOrders.immutableOrder();
    }

    private void iterateChildGroups(ImOrderSet<GroupObjectEntity> children, ImOrderSet<GroupObjectEntity> parentGroups, ImOrderMap<CompareEntity, Boolean> parentOrders, SessionTableUsage<ObjectEntity, CompareEntity> parentTable, int selectTop, Map<GroupObjectEntity, StaticKeyData> keySources, StaticPropertyData<SDP> propSources, ImSet<GroupObjectEntity> valueGroups) throws SQLException, SQLHandledException {
        for (GroupObjectEntity node : children) {
            iterateChildGroup(node, parentGroups, parentOrders, parentTable, selectTop, keySources, propSources, valueGroups);
        }
    }

    @StackMessage("{message.form.read.report.node}")
    private void iterateChildGroup(@ParamMessage final GroupObjectEntity thisGroup, final ImOrderSet<GroupObjectEntity> parentGroups, ImOrderMap<CompareEntity, Boolean> parentOrders, SessionTableUsage<ObjectEntity, CompareEntity> parentTable, int selectTop, Map<GroupObjectEntity, StaticKeyData> keySources, StaticPropertyData<SDP> propSources, ImSet<GroupObjectEntity> valueGroups) throws SQLException, SQLHandledException {

        ImSet<SDP> queryProperties = getQueryProperties(hierarchy.getProperties(thisGroup).getSet());
        
        ImMap<ImOrderSet<GroupObjectEntity>, ImSet<SDP>> columnGroupObjectProps;
        if(supportColumnGroups)
            // for group-in-columns
            columnGroupObjectProps = queryProperties.group(key -> key.getColumnGroupObjects()).addIfNotContains(SetFact.EMPTYORDER(), SetFact.EMPTY()); // we need keys anyway
        else 
            columnGroupObjectProps = MapFact.singleton(SetFact.EMPTYORDER(), queryProperties);
                
        for(int g=0,sizeG=columnGroupObjectProps.size();g<sizeG;g++) {
            ImOrderSet<GroupObjectEntity> columnGroupObjects = columnGroupObjectProps.getKey(g);
            final ImOrderSet<GroupObjectEntity> allColumnGroupObjects = getParentColumnGroupObjects(columnGroupObjects);
            
            ImSet<GroupObjectEntity> parentGroupsAndThisGroup = parentGroups.getSet();
            if(thisGroup != null)
                parentGroupsAndThisGroup = parentGroupsAndThisGroup.addExcl(thisGroup);
            
            ImOrderSet<GroupObjectEntity> thisGroups = allColumnGroupObjects.removeOrder(parentGroupsAndThisGroup);
            if(thisGroup != null)
                thisGroups = thisGroups.addOrderExcl(thisGroup);

            ImOrderSet<GroupObjectEntity> allGroups = parentGroups.addOrderExcl(thisGroups);
            final ImOrderSet<ObjectEntity> allObjects = GroupObjectEntity.getOrderObjects(allGroups);

            // building query
            QueryBuilder<ObjectEntity, CompareEntity> queryBuilder = new QueryBuilder<>(allObjects.getSet());
            ImMap<ObjectEntity, Expr> mapExprs = queryBuilder.getMapExprs();

            Modifier modifier = formInterface.getModifier();
            
            // adding filters, orders and properties for parent groups
            if (parentTable != null) {
                Join<CompareEntity> parentJoin = parentTable.join(mapExprs);
                queryBuilder.and(parentJoin.getWhere());
                for (CompareEntity order : parentOrders.keyIt())
                    queryBuilder.addProperty(order, parentJoin.getExpr(order));
            }

            // adding objects for value groups 
            mapExprs = addObjectValues(valueGroups, mapExprs); // strictly speaking for current InteractiveDataInterface filters / orders implementation there's no need to add this exprs

            // adding filters, orders for this group
            queryBuilder.and(getWhere(thisGroups.getSet(), valueGroups, mapExprs));
            ImOrderMap<CompareEntity, Boolean> thisOrders = getOrders(thisGroups, valueGroups);
            for (CompareEntity order : thisOrders.keyIt())
                queryBuilder.addProperty(order, order.getEntityExpr(mapExprs, modifier));

            ImOrderMap<CompareEntity, Boolean> allOrders = parentOrders.addOrderExcl(thisOrders);

            final Query<ObjectEntity, CompareEntity> query = queryBuilder.getQuery();

            // reading types
            ImMap<ObjectEntity, Type> keyTypes = query.getKeyTypes(ObjectEntity::getType);
            ImMap<CompareEntity, Type> orderTypes = query.getPropertyTypes(CompareEntity::getType);

            // saving to table
            DataSession session = formInterface.getSession();
            QueryEnvironment queryEnv = formInterface.getQueryEnv();
            BaseClass baseClass = formInterface.getBaseClass();
            SQLSession sql = session.sql;
            SessionTableUsage<ObjectEntity, CompareEntity> keysTable = new SessionTableUsage<>("ichreports", sql, query, baseClass, queryEnv, keyTypes, orderTypes, selectTop);

            try {
                // column groups data
                boolean noColumnGroups = columnGroupObjects.isEmpty();
                final ImSet<ObjectEntity> thisColumnObjects;
                final ImSet<ObjectEntity> parentColumnObjects;
                ImMap<ImMap<ObjectEntity, Object>, ImOrderSet<ImMap<ObjectEntity, Object>>> columnData;

                // reading key values
                ImOrderSet<ImMap<ObjectEntity, Object>> keyData = keysTable.read(sql, queryEnv, allOrders).keyOrderSet();
                if(noColumnGroups) { // optimization (because usually there are no column objects)
                    keySources.put(thisGroup, new StaticKeyData(allObjects, keyData));

                    thisColumnObjects = SetFact.EMPTY();
                    parentColumnObjects = SetFact.EMPTY();
                    columnData = MapFact.singleton(MapFact.EMPTY(), SetFact.singletonOrder(MapFact.EMPTY()));
                } else {
                    ImSet<GroupObjectEntity> parentColumnGroupObjects = allColumnGroupObjects.getSet().filter(parentGroupsAndThisGroup).remove(columnGroupObjects.getSet());

                    thisColumnObjects = GroupObjectEntity.getObjects(columnGroupObjects.getSet());
                    parentColumnObjects = GroupObjectEntity.getObjects(parentColumnGroupObjects);
                    final ImSet<ObjectEntity> allColumnObjects = parentColumnObjects.addExcl(thisColumnObjects);

                    columnData = keyData.mapMergeOrderSetValues(value -> value.filterIncl(allColumnObjects)).groupOrder(key -> key.filterIncl(parentColumnObjects));
                }
                
                // reading property values
                ImMap<ImSet<ObjectEntity>, ImSet<SDP>> groupObjectProps = columnGroupObjectProps.getValue(g).group(key -> {
                    return ((PropertyObjectEntity<?>) key.getPropertyObjectEntity()).getObjectInstances().filter(allObjects.getSet()); // because of the value groups
                });
                for(int i=0,size=groupObjectProps.size();i<size;i++) {
                    ImSet<ObjectEntity> objects = groupObjectProps.getKey(i);
                    ImSet<SDP> props = groupObjectProps.getValue(i);

                    QueryBuilder<ObjectEntity, SDP> propQueryBuilder = new QueryBuilder<>(objects);
                    ImMap<ObjectEntity, Expr> mapPropExprs = propQueryBuilder.getMapExprs();

                    propQueryBuilder.and(keysTable.getGroupWhere(mapPropExprs));

                    // adding objects for value groups
                    mapPropExprs = addObjectValues(valueGroups, mapPropExprs);
                    
                    // adding properties
                    for (SDP queryProp : props)
                        propQueryBuilder.addProperty(queryProp, queryProp.getPropertyObjectEntity().getExpr(mapPropExprs, modifier));

                    Query<ObjectEntity, SDP> propQuery = propQueryBuilder.getQuery();
                    final ImOrderMap<ImMap<ObjectEntity, Object>, ImMap<SDP, Object>> propData = propQuery.execute(sql, formInterface.getQueryEnv());

                    ImMap<SDP, Type> propTypes = propQuery.getPropertyTypes(PropertyReaderEntity::getType);

                    // converting from row-based to column-based (it's important to keep keys, to reduce footprint)
                    propSources.add(objects, props.mapValues(new Function<SDP, ImMap<ImMap<ObjectEntity, Object>, Object>>() {
                        public ImMap<ImMap<ObjectEntity, Object>, Object> apply(final SDP prop) {
                            return propData.getMap().mapValues(map -> map.get(prop));
                        }
                    }), propTypes, parentColumnObjects, thisColumnObjects, columnData);
                }

                if(noColumnGroups)
                    iterateChildGroups(hierarchy.getDependencies(thisGroup), allGroups, allOrders, keysTable, selectTop, keySources, propSources, valueGroups);
            } finally {
                keysTable.drop(sql, session.getOwner());
            }
        }
    }

    private ImSet<SDP> getQueryProperties(ImSet<PropertyDrawEntity> properties) {
        MExclSet<SDP> mQueryProps = SetFact.mExclSet();
        for(PropertyDrawEntity property : properties)
            fillQueryProps(property, mQueryProps);
        return mQueryProps.immutable();
    }

    // we need not only direct column groups, but all their hierarchy (except current parent groups)
    private ImOrderSet<GroupObjectEntity> getParentColumnGroupObjects(ImOrderSet<GroupObjectEntity> groupObjects) {
        MOrderSet<GroupObjectEntity> mGroups = SetFact.mOrderSet();
        for (GroupObjectEntity group : groupObjects)
            fillParentGroupObjects(mGroups, group);
        return mGroups.immutableOrder();
    }

    private void fillParentGroupObjects(MOrderSet<GroupObjectEntity> mGroups, GroupObjectEntity group) {
        GroupObjectEntity parentGroup = hierarchy.getParentGroup(group);
        if(parentGroup != null)
            fillParentGroupObjects(mGroups, parentGroup);
        
        mGroups.add(group);
    }
}
