package platform.server.form.instance.filter;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyValueImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.Modifier;

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

    public NotNullFilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
        super(inStream, form);
        checkChange = false;
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return property.getExpr(mapKeys, modifier).getWhere();
    }

    @Override
    public <X extends PropertyInterface> Set<CalcPropertyValueImplement<?>> getResolveChangeProperties(CalcProperty<X> toChange) {
        if(checkChange && CalcProperty.depends((CalcProperty<?>) property.property, toChange))
            return BaseUtils.immutableCast(Collections.singleton(property.getValueImplement()));
        return super.getResolveChangeProperties(toChange);
    }

    @Override
    public void resolveAdd(ExecutionEnvironment env, CustomObjectInstance object, DataObject addObject) throws SQLException {

        if(!resolveAdd)
            return;

        if (!hasObjectInInterface(object))
            return;

        ImRevMap<P, KeyExpr> mapKeys = property.property.getMapKeys();
        ImRevMap<PropertyObjectInterfaceInstance, KeyExpr> mapObjects = property.mapping.toRevMap().crossJoin(mapKeys);
        property.property.setNotNull(mapKeys, getChangedWhere(object, mapObjects, addObject), env, true);
    }
}
