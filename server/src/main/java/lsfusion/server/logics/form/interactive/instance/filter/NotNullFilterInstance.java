package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.form.interactive.change.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.CalcPropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyValueImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.action.session.ExecutionEnvironment;
import lsfusion.server.logics.action.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

public class NotNullFilterInstance<P extends PropertyInterface> extends PropertyFilterInstance<P> {

    private final boolean checkChange;

    public NotNullFilterInstance(CalcPropertyObjectInstance<P> property) {
        this(property, false);
    }

    public NotNullFilterInstance(CalcPropertyObjectInstance<P> property, boolean checkChange) {
        this(property, checkChange, false);
    }

    public NotNullFilterInstance(CalcPropertyObjectInstance<P> property, boolean checkChange, boolean resolveAdd) {
        super(property, resolveAdd);
        this.checkChange = checkChange;
    }

    public NotNullFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        super(inStream, form);
        checkChange = false;
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, MSet<CalcProperty> mUsedProps) throws SQLException, SQLHandledException {
        return property.getExpr(mapKeys, modifier, reallyChanged, mUsedProps).getWhere();
    }

    @Override
    public <X extends PropertyInterface> Set<CalcPropertyValueImplement<?>> getResolveChangeProperties(CalcProperty<X> toChange) {
        if(checkChange && CalcProperty.depends((CalcProperty<?>) property.property, toChange))
            return BaseUtils.immutableCast(Collections.singleton(property.getValueImplement()));
        return super.getResolveChangeProperties(toChange);
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
}
