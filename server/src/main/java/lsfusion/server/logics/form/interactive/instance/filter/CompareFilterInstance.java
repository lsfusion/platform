package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.changed.ChangedData;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class CompareFilterInstance<P extends PropertyInterface> extends PropertyFilterInstance<P> {

    boolean negate;
    public Compare compare;
    public CompareInstance value;

    public CompareFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        super(inStream,form);
        negate = inStream.readBoolean();
        compare = Compare.deserialize(inStream);
        value = deserializeCompare(inStream, form, property.getFilterValueClass(compare));
        junction = inStream.readBoolean();
    }

    private static CompareInstance deserializeCompare(DataInputStream inStream, FormInstance form, ValueClass valueClass) throws IOException, SQLException, SQLHandledException {
        byte type = inStream.readByte();
        switch(type) {
            case 0:
                return form.session.getObjectValue(valueClass, BaseUtils.deserializeObject(inStream));
            case 1:
                return form.getObjectInstance(inStream.readInt());
            case 2:
                return (PropertyObjectInstance)((PropertyDrawInstance<?>)form.getPropertyDraw(inStream.readInt())).getValueProperty();
        }

        throw new IOException();
    }

    @Override
    public boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier, boolean hidden, ImSet<GroupObjectInstance> groupObjects) throws SQLException, SQLHandledException {
        return super.dataUpdated(changedProps, reallyChanged, modifier, hidden, groupObjects) || value.dataUpdated(changedProps, reallyChanged, modifier, hidden, groupObjects);
    }

    @Override
    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return super.classUpdated(gridGroups) || value.classUpdated(gridGroups);
    }

    @Override
    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return super.objectUpdated(gridGroups) || value.objectUpdated(gridGroups);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        Where where = property.getExpr(mapKeys, modifier, reallyChanged, mUsedProps).compare(value.getExpr(mapKeys, modifier, reallyChanged, mUsedProps), compare);
        return negate ? where.not() : where;
    }

    @Override
    public void fillProperties(MSet<Property> properties) {
        super.fillProperties(properties);
        value.fillProperties(properties);
    }

    @Override
    public void resolveAdd(ExecutionEnvironment env, CustomObjectInstance object, DataObject addObject, ExecutionStack stack) throws SQLException, SQLHandledException {

        if(!resolveAdd)
            return;

        if(compare!=Compare.EQUALS)
            return;

        if (!hasObjectInInterface(object))
            return;

        ImRevMap<P, KeyExpr> mapKeys = property.property.getMapKeys();
        ImMap<PropertyObjectInterfaceInstance, KeyExpr> mapObjects = property.mapping.toRevMap(property.property.getFriendlyOrderInterfaces()).crossJoin(mapKeys);
        env.change(property.property, new PropertyChange<>(mapKeys,
                value.getExpr(mapObjects.filter(object.groupTo.objects), env.getModifier()),
                getChangedWhere(object, mapObjects, addObject)));
    }

    @Override
    public GroupObjectInstance getApplyObject() {

        GroupObjectInstance applyObject = super.getApplyObject();

        for(ObjectInstance intObject : value.getObjectInstances())
            if(applyObject == null || intObject.groupTo.order > applyObject.order)
                applyObject = intObject.getApplyObject();

        return applyObject;
    }

    @Override
    protected void fillObjects(MSet<ObjectInstance> objects) {
        super.fillObjects(objects);
        objects.addAll(value.getObjectInstances().toSet());
    }

    @Override
    public NotNullFilterInstance notNullCached() {
        PropertyImplement<?, PropertyObjectInterfaceInstance> implement = PropertyFact.createCompareCached(getPropertyImplement(property), compare,
                value instanceof PropertyObjectInstance ? getPropertyImplement((PropertyObjectInstance) value) : PropertyFact.createValueCached((PropertyObjectInterfaceInstance) value));
        if(negate)
            implement = PropertyFact.createNotCached(implement);
        return getFilterInstance(implement);
    }
}
