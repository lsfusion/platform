package lsfusion.server.remote;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.Compare;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.Property;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class InteractiveFormReportManager extends FormReportManager<PropertyDrawInstance, GroupObjectInstance, PropertyObjectInstance, CalcPropertyObjectInstance, OrderInstance, ObjectInstance, PropertyReaderInstance> {
    
    public InteractiveFormReportManager(final FormInstance form) {
        super(new FormReportInterface<PropertyDrawInstance, GroupObjectInstance, PropertyObjectInstance, CalcPropertyObjectInstance, OrderInstance, ObjectInstance, PropertyReaderInstance>() {
            @Override
            public BusinessLogics getBL() {
                return form.BL;
            }

            @Override
            public FormEntity getEntity() {
                return form.entity;
            }

            @Override
            public ImSet<GroupObjectInstance> getGroups() {
                return form.getGroups();
            }

            @Override
            public int getGroupID(GroupObjectInstance groupObjectInstance) {
                return groupObjectInstance.getID();
            }

            @Override
            public int getObjectID(ObjectInstance o) {
                return o.getID();
            }

            @Override
            public GroupObjectInstance getGroupByID(int id) {
                return form.getGroupObjectInstance(id);
            }

            @Override
            public Object read(CalcPropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException {
                CalcPropertyObjectInstance propInstance = form.instanceFactory.getInstance(reportPathProp);
                return propInstance.read(form);
            }

            @Override
            public FormInstance getFormInstance() {
                return form;
            }

            @Override
            public BaseClass getBaseClass() {
                return form.BL.LM.baseClass;
            }

            @Override
            public QueryEnvironment getQueryEnv() {
                return form.getQueryEnv();
            }

            @Override
            public DataSession getSession() {
                return form.getSession();
            }

            @Override
            public ImOrderSet<GroupObjectInstance> getOrderGroups() {
                return form.getOrderGroups();
            }

            @Override
            public ImList<PropertyDrawInstance> getProperties() {
                return form.properties;
            }

            @Override
            public ImCol<ObjectInstance> getPObjects(PropertyObjectInstance po) {
                return ((PropertyObjectInstance<?, ?>)po).getObjectInstances();
            }

            @Override
            public GroupObjectInstance getGroupTo(ObjectInstance o) {
                return o.groupTo;
            }

            @Override
            public GroupObjectInstance getToDraw(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.toDraw;
            }

            @Override
            public GroupObjectInstance getApplyObject(PropertyObjectInstance po) {
                return po.getApplyObject();
            }

            @Override
            public ImSet<ObjectInstance> getObjects(ImSet<GroupObjectInstance> groups) {
                return GroupObjectInstance.getObjects(groups);
            }

            @Override
            public ImSet<ObjectInstance> getObjects(GroupObjectInstance groupObjectInstance) {
                return groupObjectInstance.objects;
            }

            @Override
            public ImOrderSet<ObjectInstance> getOrderObjects(GroupObjectInstance groupObjectInstance) {
                return groupObjectInstance.getOrderObjects();
            }

            @Override
            public ImOrderSet<ObjectInstance> getOrderObjects(ImOrderSet<GroupObjectInstance> go) {
                return GroupObjectInstance.getOrderObjects(go);
            }

            @Override
            public Type getType(OrderInstance o) {
                return o.getType();
            }

            @Override
            public ImOrderSet<GroupObjectInstance> getOrderColumnGroupObjects(PropertyDrawInstance pd) {
                return ((PropertyDrawInstance<?>)pd).getOrderColumnGroupObjects();
            }

            @Override
            public ImSet<GroupObjectInstance> getColumnGroupObjects(PropertyDrawInstance pd) {
                return ((PropertyDrawInstance<?>)pd).getColumnGroupObjects();
            }

            @Override
            public GroupObjectEntity getEntity(GroupObjectInstance groupObjectInstance) {
                return groupObjectInstance.entity;
            }

            @Override
            public Property getProperty(PropertyObjectInstance po) {
                return po.property;
            }

            @Override
            public String getGroupSID(GroupObjectInstance cpo) {
                return cpo.getSID();
            }

            @Override
            public String getPSID(PropertyDrawInstance cpo) {
                return cpo.getsID();
            }

            @Override
            public ClassViewType getPViewType(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.getForceViewType();
            }

            @Override
            public ImOrderMap<OrderInstance, Boolean> getOrders(GroupObjectInstance groupObjectInstance) {
                return groupObjectInstance.orders;
            }

            @Override
            public ObjectValue getObjectValue(ObjectInstance o) {
                return o.getObjectValue();
            }

            @Override
            public GroupObjectInstance getDrawApplyObject(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.getApplyObject();
            }

            @Override
            public ImSet<ObjectInstance> getDrawObjects(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.getObjectInstances();
            }

            @Override
            public boolean isNoParamCalcProperty(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.isNoParamCalcProperty();
            }

            @Override
            public CalcPropertyObjectInstance getDrawInstance(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.getDrawInstance();
            }

            @Override
            public CalcPropertyObjectInstance getPropertyCaption(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.propertyCaption;
            }

            @Override
            public CalcPropertyObjectInstance getPropertyFooter(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.propertyFooter;
            }

            @Override
            public CalcPropertyObjectInstance getPropertyShowIf(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.propertyShowIf;
            }
            
            @Override
            public PropertyReaderInstance getCaptionReader(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.captionReader;
            }

            @Override
            public PropertyReaderInstance getFooterReader(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.footerReader;
            }

            @Override
            public PropertyReaderInstance getShowIfReader(PropertyDrawInstance propertyDrawInstance) {
                return propertyDrawInstance.showIfReader;
            }

            @Override
            public Where getWhere(GroupObjectInstance group, ImMap<ObjectInstance, Expr> mapExprs) throws SQLException, SQLHandledException {
                return group.getWhere(mapExprs, form.getModifier());
            }

            @Override
            public Where getFixedObjectsWhere(GroupObjectInstance group, Integer groupId, ImMap<ObjectInstance, Expr> mapExprs) throws SQLException, SQLHandledException {
                Where where = Where.TRUE;
                if (!group.curClassView.isGrid())
                    for (ObjectInstance object : group.objects) {
                        where = where.and(getExpr(object, mapExprs).compare(getObjectValue(object).getExpr(), Compare.EQUALS));
                    }
                return where;
            }

            @Override
            public Expr getExpr(OrderInstance o, ImMap<ObjectInstance, Expr> mapExprs) throws SQLException, SQLHandledException {
                return o.getExpr(mapExprs, form.getModifier());
            }

            @Override
            public boolean isPropertyShown(PropertyDrawInstance propertyDrawInstance) {
                return form.isPropertyShown(propertyDrawInstance);
            }

            @Override
            public String getObjectSID(ObjectInstance o) {
                return o.getsID();
            }

            @Override
            public byte getTypeID(PropertyReaderInstance propertyReaderInstance) {
                return propertyReaderInstance.getTypeID();
            }

            @Override
            public int getID(PropertyReaderInstance propertyReaderInstance) {
                return propertyReaderInstance.getID();
            }

            @Override
            public PropertyType getPropertyType(PropertyReaderInstance propertyReaderInstance) {
                return propertyReaderInstance.getPropertyType(getEntity());
            }
        });
    }
}
