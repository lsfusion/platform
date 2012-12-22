package platform.server.form.instance.filter;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.interop.Compare;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

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

    public CompareFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        super(inStream,form);
        negate = inStream.readBoolean();
        compare = Compare.deserialize(inStream);
        value = deserializeCompare(inStream, form, property.getType());
        junction = inStream.readBoolean();
    }

    private static CompareValue deserializeCompare(DataInputStream inStream, FormInstance form, Type DBType) throws IOException, SQLException {
        byte type = inStream.readByte();
        switch(type) {
            case 0:
                return form.session.getObjectValue(BaseUtils.deserializeObject(inStream),DBType);
            case 1:
                return form.getObjectInstance(inStream.readInt());
            case 2:
                return (CalcPropertyObjectInstance)((PropertyDrawInstance<?>)form.getPropertyDraw(inStream.readInt())).propertyObject;
        }

        throw new IOException();
    }

    @Override
    public boolean dataUpdated(FunctionSet<CalcProperty> changedProps) {
        return super.dataUpdated(changedProps) || value.dataUpdated(changedProps);
    }

    @Override
    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return super.classUpdated(gridGroups) || value.classUpdated(gridGroups);
    }

    @Override
    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return super.objectUpdated(gridGroups) || value.objectUpdated(gridGroups);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        Where where = property.getExpr(mapKeys, modifier).compare(value.getExpr(mapKeys, modifier), compare);
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
    public void resolveAdd(ExecutionEnvironment env, CustomObjectInstance object, DataObject addObject) throws SQLException {

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
}
