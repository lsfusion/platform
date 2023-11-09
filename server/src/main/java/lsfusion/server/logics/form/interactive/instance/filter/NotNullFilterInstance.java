package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class NotNullFilterInstance<P extends PropertyInterface> extends PropertyFilterInstance<P> {

    public NotNullFilterInstance(PropertyObjectInstance<P> property) {
        this(property, false);
    }

    public NotNullFilterInstance(PropertyObjectInstance<P> property, boolean resolveAdd) {
        this(property, resolveAdd, null, null);
    }

    public NotNullFilterInstance(PropertyObjectInstance<P> property, GroupObjectInstance toDraw, PropertyDrawInstance<P> propertyDraw) {
        this(property, false, toDraw, propertyDraw);
    }

    public NotNullFilterInstance(PropertyObjectInstance<P> property, boolean resolveAdd, GroupObjectInstance toDraw, PropertyDrawInstance<P> propertyDraw) {
        super(property, resolveAdd, toDraw, propertyDraw);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        return property.getExpr(mapKeys, modifier, reallyChanged, mUsedProps).getWhere();
    }

    @Override
    public void resolveAdd(ExecutionEnvironment env, CustomObjectInstance object, DataObject addObject, ExecutionStack stack) throws SQLException, SQLHandledException {

        if(!resolveAdd)
            return;

        if (!hasObjectInInterface(object))
            return;

        ImRevMap<P, KeyExpr> mapKeys = property.property.getMapKeys();
        ImRevMap<PropertyObjectInterfaceInstance, KeyExpr> mapObjects = property.mapping.toRevMap(property.property.getFriendlyOrderInterfaces()).crossJoin(mapKeys);
        property.property.setNotNull(mapKeys, getChangedWhere(object, mapObjects, addObject), env, true, stack);
    }

    @Override
    public NotNullFilterInstance notNullCached() {
        return this;
    }
}
