package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;

public class InputValueList<P extends PropertyInterface> {

    private final Property<P> property;

    private final ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders;

    private final ImMap<P, ObjectValue> mapValues; // external context

    public InputValueList(Property<P> property, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, ImMap<P, ObjectValue> mapValues) {
        this.property = property;

        this.orders = orders;

        this.mapValues = mapValues;
    }

    public InputListExpr<P> getListExpr(Modifier modifier) throws SQLException, SQLHandledException {
        ImRevMap<P, KeyExpr> innerKeys = KeyExpr.getMapKeys(property.interfaces.removeIncl(mapValues.keys()));
        ImMap<P, Expr> innerExprs = MapFact.addExcl(innerKeys, DataObject.getMapExprs(mapValues));

        ImOrderMap<Expr, Boolean> orderExprs = orders.mapMergeOrderKeysEx((ThrowingFunction<PropertyInterfaceImplement<P>, Expr, SQLException, SQLHandledException>) value -> value.mapExpr(innerKeys, modifier));

        return new InputListExpr<>(innerKeys, property.getExpr(innerExprs, modifier), orderExprs);
    }

    public P singleInterface() {
        return property.interfaces.removeIncl(mapValues.keys()).single();
    }

    public boolean hasValues() {
        return !mapValues.isEmpty();
    }

    public Stat getSelectStat() {
        return property.getSelectStat(mapValues.keys());
    }

    public ImSet<Property> getChangeProps() {
        if(orders.isEmpty()) // optimization
            return SetFact.singleton(property);

        MSet<Property> mProps = SetFact.mSet();
        mProps.add(property);
        for(PropertyInterfaceImplement<P> order : orders.keyIt())
            order.mapFillDepends(mProps);
        return mProps.immutable();
    }

    public Property<?> getCacheKey() {
        return property;
    }
    public DBManager.Param<?> getCacheParam(String value, AsyncMode mode) {
        return new DBManager.Param<P>(mapValues, orders, value, mode.getCacheMode());
    }
}
