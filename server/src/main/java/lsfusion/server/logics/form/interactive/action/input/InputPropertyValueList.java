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
import lsfusion.server.data.stat.Cost;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;

public class InputPropertyValueList<P extends PropertyInterface> extends InputValueList<P, Property<P>> {

    protected final ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders;

    public InputPropertyValueList(Property<P> property, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, ImMap<P, ObjectValue> mapValues) {
        super(property, mapValues);

        this.orders = orders;
    }

    public InputListExpr<P> getListExpr(Modifier modifier, AsyncMode asyncMode) throws SQLException, SQLHandledException {
        ImRevMap<P, KeyExpr> innerKeys = KeyExpr.getMapKeys(property.interfaces.removeIncl(mapValues.keys()));
        return new InputListExpr<>(innerKeys, property.getExpr(MapFact.addExcl(innerKeys, DataObject.getMapExprs(mapValues)), modifier), getOrderExprs(modifier, innerKeys, asyncMode));
    }

    private ImOrderMap<Expr, Boolean> getOrderExprs(Modifier modifier, ImRevMap<P, KeyExpr> innerKeys, AsyncMode asyncMode) throws SQLException, SQLHandledException {
        if(asyncMode == null)
            return MapFact.EMPTYORDER();

        // the check is that when we have too much rows, we remove the order for the optimization purposes
        if (!orders.isEmpty()) {
            if (isTooMayRows())
                return MapFact.EMPTYORDER();
        } else {
            if (asyncMode.isObjects() && !isTooMayRows()) // maybe OBJECTVALUES also can be used
                return MapFact.singletonOrder(innerKeys.get(singleInterface()), false);
        }

        return orders.mapMergeOrderKeysEx((ThrowingFunction<PropertyInterfaceImplement<P>, Expr, SQLException, SQLHandledException>) value -> value.mapExpr(innerKeys, modifier));
    }

    private boolean isTooMayRows() {
        return property.getInterfaceStat(mapValues.keys()).getCount() > Settings.get().getAsyncValuesMaxReadOrderCount();
    }

    public P singleInterface() {
        return property.interfaces.removeIncl(mapValues.keys()).single();
    }

    public Stat getSelectStat() {
        return property.getSelectStat(mapValues.keys());
    }

    private ImSet<P> getInterfaceParams() { // maybe classes from ObjectValue should be used with the proper caching
        return mapValues.keys();
    }
    public Stat getInterfaceStat() {
        return property.getInterfaceStat(getInterfaceParams());
    }

    public Cost getInterfaceCost() {
        return property.getInterfaceCost(getInterfaceParams());
    }

    public boolean isHighlight() {
        Type type = property.getType();
        return !(type instanceof DataClass && ((DataClass<?>) type).markupHtml()); // ts_headline breaks html : https://stackoverflow.com/questions/40263956/why-is-postgresql-stripping-html-entities-in-ts-headline
    }

    @Override
    public ImSet<Property> getChangeProps() {
        if(orders.isEmpty()) // optimization
            return SetFact.singleton(property);

        MSet<Property> mProps = SetFact.mSetMax(orders.size() + 1);
        mProps.add(property);
        for(PropertyInterfaceImplement<P> order : orders.keyIt())
            order.mapFillDepends(mProps);
        return mProps.immutable();
    }

    @Override
    protected ImOrderMap<PropertyInterfaceImplement<P>, Boolean> getCacheOrders() {
        return orders;
    }
}
