package lsfusion.server.form.instance.filter;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.Compare;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChange;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class CompareFilterInstance<P extends PropertyInterface> extends PropertyFilterInstance<P> {

    boolean negate;
    public Compare compare;
    public CompareValue value;

    public CompareFilterInstance(CalcPropertyObjectInstance<P> property,Compare compare, CompareValue value) {
        this(property, compare, value, false);
    }

    // не можем хранить ссылку на Entity, так как этот Instance может создаваться на стороне клиента и не иметь Entity
    public CompareFilterInstance(CalcPropertyObjectInstance<P> property,Compare compare, CompareValue value, boolean resolve) {
        super(property, resolve);
        this.compare = compare;
        this.value = value;
    }

    public CompareFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        super(inStream,form);
        negate = inStream.readBoolean();
        compare = Compare.deserialize(inStream);
        value = deserializeCompare(inStream, form, property.getValueClass());
        junction = inStream.readBoolean();
    }

    private static CompareValue deserializeCompare(DataInputStream inStream, FormInstance form, ValueClass valueClass) throws IOException, SQLException, SQLHandledException {
        byte type = inStream.readByte();
        switch(type) {
            case 0:
                return form.session.getObjectValue(valueClass, BaseUtils.deserializeObject(inStream));
            case 1:
                return form.getObjectInstance(inStream.readInt());
            case 2:
                return (CalcPropertyObjectInstance)((PropertyDrawInstance<?>)form.getPropertyDraw(inStream.readInt())).propertyObject;
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

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        Where where = property.getExpr(mapKeys, modifier, reallyChanged).compare(value.getExpr(mapKeys, modifier, reallyChanged), compare);
        return negate ? where.not() : where;
    }

    @Override
    public void fillProperties(MSet<CalcProperty> properties) {
        super.fillProperties(properties);
        value.fillProperties(properties);
    }

    @Override
    public boolean isInInterface(GroupObjectInstance classGroup) {
        return super.isInInterface(classGroup) && value.isInInterface(classGroup);
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
        ImMap<PropertyObjectInterfaceInstance, KeyExpr> mapObjects = property.mapping.toRevMap().crossJoin(mapKeys);
        env.change(property.property, new PropertyChange<P>(mapKeys,
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
}
