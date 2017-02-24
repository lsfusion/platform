package lsfusion.server.remote;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.form.PropertyReadType;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyType;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class StaticFormReportManager extends FormReportManager<PropertyDrawEntity, GroupObjectEntity, PropertyObjectEntity, CalcPropertyObjectEntity, OrderEntity, ObjectEntity, PropertyReaderEntity> {

    public StaticFormReportManager(final FormEntity<?> form, final ImMap<ObjectEntity, ObjectValue> mapObjects, final ExecutionContext<?> context) {
        super(new FormReportInterface<PropertyDrawEntity, GroupObjectEntity, PropertyObjectEntity, CalcPropertyObjectEntity, OrderEntity, ObjectEntity, PropertyReaderEntity>() {
            
            private ImMap<GroupObjectEntity, ImOrderMap<OrderEntity, Boolean>> ordersMap = BaseUtils.immutableCast(form.getDefaultOrdersList().mapOrderKeyValues(new GetValue<OrderEntity<?>, PropertyDrawEntity<?>>() {
                public OrderEntity<?> getMapValue(PropertyDrawEntity<?> value) {
                    return (CalcPropertyObjectEntity<?>) value.propertyObject;
                }
            }, new GetValue<Boolean, Boolean>() {
                public Boolean getMapValue(Boolean value) {
                    return !value;
                }}).mergeOrder(form.getFixedOrdersList()).groupOrder(new BaseUtils.Group<GroupObjectEntity, OrderEntity<?>>() {
                @Override
                public GroupObjectEntity group(OrderEntity<?> key) {
                    GroupObjectEntity groupObject = key.getApplyObject(form.getGroupsList());
                    if(groupObject == null)
                        return GroupObjectEntity.NULL;
                    return groupObject;
                }
            }));            
            private ImMap<GroupObjectEntity, ImSet<FilterEntity>> filtersMap = form.getFixedFilters().group(new BaseUtils.Group<GroupObjectEntity, FilterEntity>() {
                @Override
                public GroupObjectEntity group(FilterEntity key) {
                    GroupObjectEntity groupObject = key.getApplyObject(form);
                    if(groupObject == null)
                        return GroupObjectEntity.NULL;
                    return groupObject;
                }
            });            
            
            @Override
            public FormEntity getEntity() {
                return form;
            }

            @Override
            public ImSet<GroupObjectEntity> getGroups() {
                return form.getGroupsList().getSet();
            }

            @Override
            public int getGroupID(GroupObjectEntity groupObjectEntity) {
                return groupObjectEntity.getID();
            }

            @Override
            public int getObjectID(ObjectEntity o) {
                return o.getID();
            }

            @Override
            public String getObjectSID(ObjectEntity o) {
                return o.getSID();
            }

            @Override
            public ClassViewType getGroupViewType(GroupObjectEntity groupObjectEntity) {
                return groupObjectEntity.initClassView;
            }

            @Override
            public GroupObjectEntity getGroupByID(int id) {
                return form.getGroupObject(id);
            }

            @Override
            public FormInstance getFormInstance() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isPropertyShown(PropertyDrawEntity propertyDrawEntity) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BaseClass getBaseClass() {
                return context.getBL().LM.baseClass;
            }

            @Override
            public QueryEnvironment getQueryEnv() {
                return context.getQueryEnv();
            }

            @Override
            public DataSession getSession() {
                return context.getSession();
            }

            @Override
            public ImOrderSet<GroupObjectEntity> getOrderGroups() {
                return form.getGroupsList();
            }

            @Override
            public ImList<PropertyDrawEntity> getProperties() {
                return BaseUtils.immutableCast(form.getPropertyDrawsList());
            }

            @Override
            public PropertyObjectEntity getPropertyObject(PropertyDrawEntity propertyDrawEntity) {
                return propertyDrawEntity.propertyObject;
            }

            @Override
            public ImCol<ObjectEntity> getPObjects(PropertyObjectEntity propertyObjectEntity) {
                return ((PropertyObjectEntity<?, ?>)propertyObjectEntity).getColObjectInstances();
            }

            @Override
            public GroupObjectEntity getGroupTo(ObjectEntity o) {
                return o.groupTo;
            }

            @Override
            public GroupObjectEntity getToDraw(PropertyDrawEntity propertyDrawEntity) {
                GroupObjectEntity toDraw = propertyDrawEntity.toDraw;
                if(toDraw == null)
                    toDraw = propertyDrawEntity.getToDraw(form);
                return toDraw;
            }

            @Override
            public GroupObjectEntity getApplyObject(PropertyObjectEntity propertyObjectEntity) {
                return propertyObjectEntity.getApplyObject(form.getGroupsList());
            }

            @Override
            public ImSet<ObjectEntity> getObjects(GroupObjectEntity groupObjectEntity) {
                return groupObjectEntity.getObjects();
            }

            @Override
            public ImOrderSet<ObjectEntity> getOrderObjects(GroupObjectEntity groupObjectEntity) {
                return groupObjectEntity.getOrderObjects();
            }

            @Override
            public ImOrderSet<GroupObjectEntity> getOrderColumnGroupObjects(PropertyDrawEntity propertyDrawEntity) {
                return ((PropertyDrawEntity<?>)propertyDrawEntity).getColumnGroupObjects();
            }

            @Override
            public ImSet<GroupObjectEntity> getColumnGroupObjects(PropertyDrawEntity propertyDrawEntity) {
                return ((PropertyDrawEntity<?>)propertyDrawEntity).getColumnGroupObjects().getSet();
            }

            @Override
            public GroupObjectEntity getEntity(GroupObjectEntity groupObjectEntity) {
                return groupObjectEntity;
            }

            @Override
            public Property getProperty(PropertyObjectEntity propertyObjectEntity) {
                return propertyObjectEntity.property;
            }

            @Override
            public String getGroupSID(GroupObjectEntity cpo) {
                return cpo.getSID();
            }

            @Override
            public String getPSID(PropertyDrawEntity cpo) {
                return cpo.getSID();
            }

            @Override
            public ClassViewType getPViewType(PropertyDrawEntity propertyDrawEntity) {
                return propertyDrawEntity.forceViewType;
            }

            @Override
            public CalcPropertyObjectEntity getPropertyCaption(PropertyDrawEntity propertyDrawEntity) {
                return propertyDrawEntity.propertyCaption;
            }

            @Override
            public CalcPropertyObjectEntity getPropertyFooter(PropertyDrawEntity propertyDrawEntity) {
                return propertyDrawEntity.propertyFooter;
            }

            @Override
            public ImOrderMap<OrderEntity, Boolean> getOrders(GroupObjectEntity groupObjectEntity) {
                ImOrderMap<OrderEntity, Boolean> orders = ordersMap.get(groupObjectEntity);
                if(orders == null)
                    orders = MapFact.EMPTYORDER();
                return orders;
            }

            @Override
            public Type getType(OrderEntity o) {
                return o.getType();
            }

            @Override
            public CalcPropertyObjectEntity getDrawInstance(PropertyDrawEntity propertyDrawEntity) {
                return propertyDrawEntity.getDrawInstance();
            }

            @Override
            public ImSet<ObjectEntity> getObjects(ImSet<GroupObjectEntity> groups) {
                return GroupObjectEntity.getObjects(groups);
            }

            @Override
            public ImOrderSet<ObjectEntity> getOrderObjects(ImOrderSet<GroupObjectEntity> groups) {
                return GroupObjectEntity.getOrderObjects(groups);
            }

            @Override
            public byte getTypeID(PropertyReaderEntity propertyReaderEntity) {
                return propertyReaderEntity.getTypeID();
            }

            @Override
            public int getID(PropertyReaderEntity propertyReaderEntity) {
                return propertyReaderEntity.getID();
            }

            @Override
            public PropertyType getPropertyType(PropertyReaderEntity propertyReaderEntity) {
                return propertyReaderEntity.getPropertyType();
            }

            @Override
            public PropertyReaderEntity getCaptionReader(final PropertyDrawEntity propertyDrawEntity) {
                return propertyDrawEntity.captionReader;
            }

            @Override
            public PropertyReaderEntity getFooterReader(final PropertyDrawEntity propertyDrawEntity) {
                return propertyDrawEntity.footerReader;
            }

            @Override
            public Object read(CalcPropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException {
                return reportPathProp.read(context.getEnv(), mapObjects);
            }

            @Override
            public ObjectValue getObjectValue(ObjectEntity o) {
                ObjectValue objectValue = mapObjects.get(o);
                if(objectValue != null)
                    return objectValue;
                return NullValue.instance;
            }

            @Override
            public Where getWhere(GroupObjectEntity groupObjectEntity, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException {
                ImSet<FilterEntity> filters = filtersMap.get(groupObjectEntity);
                if(filters == null)
                    filters = SetFact.EMPTY();
                return groupObjectEntity.getWhere(mapExprs, context.getModifier(), mapObjects, filters);
            }

            @Override
            public Expr getExpr(OrderEntity o, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException {
                return o.getExpr(mapExprs, context.getModifier(), mapObjects);
            }
        });
    }


}
