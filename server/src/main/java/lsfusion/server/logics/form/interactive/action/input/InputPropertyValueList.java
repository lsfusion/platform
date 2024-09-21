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
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Cost;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;

public class InputPropertyValueList<P extends PropertyInterface> extends InputValueList<P> {

    protected final ImSet<P> interfaces;
    protected final PropertyMapImplement<?, P> property;
    protected final ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders;
    private final ImMap<P, PropertyObjectInterfaceInstance> mapObjects;

    public InputPropertyValueList(ImSet<P> interfaces, PropertyMapImplement<?, P> property, ImOrderMap<PropertyInterfaceImplement<P>, Boolean> orders, ImMap<P, ObjectValue> mapValues, ImMap<P, PropertyObjectInterfaceInstance> mapObjects) {
        super(mapValues);

        this.interfaces = interfaces;
        this.property = property;
        this.orders = orders;
        this.mapObjects = mapObjects;
        assert mapValues.keys().equals(mapObjects.keys());
    }

    public InputListExpr<P> getListExpr(Modifier modifier, AsyncMode asyncMode) throws SQLException, SQLHandledException {
        ImRevMap<P, KeyExpr> innerKeys = KeyExpr.getMapKeys(getInterfaces());
        ImMap<P, Expr> innerExprs = MapFact.addExcl(innerKeys, DataObject.getMapExprs(mapValues));

        return new InputListExpr<>(innerKeys, property.mapExpr(innerExprs, modifier), getOrderExprs(modifier, innerExprs, asyncMode));
    }

    private ImSet<P> getInterfaces() {
        return interfaces.removeIncl(mapValues.keys());
    }

    private ImOrderMap<Expr, Boolean> getOrderExprs(Modifier modifier, ImMap<P, Expr> innerExprs, AsyncMode asyncMode) throws SQLException, SQLHandledException {
        if(asyncMode == null)
            return MapFact.EMPTYORDER();

        // the check is that when we have too much rows, we remove the order for the optimization purposes
        if (!orders.isEmpty()) {
            if (isTooMayRows())
                return MapFact.EMPTYORDER();
        } else {
            if (asyncMode.isObjects() && !isTooMayRows()) // maybe OBJECTVALUES also can be used
                return MapFact.singletonOrder(innerExprs.get(singleInterface()), false);
        }

        return orders.mapMergeOrderKeysEx((ThrowingFunction<PropertyInterfaceImplement<P>, Expr, SQLException, SQLHandledException>) value -> value.mapExpr(innerExprs, modifier));
    }

    private boolean isTooMayRows() {
        return getInterfaceStat().getCount() > Settings.get().getAsyncValuesMaxReadOrderCount();
    }

    public P singleInterface() {
        return getInterfaces().single();
    }

    // maybe classes from ObjectValue should be used with the proper caching
    public Stat getSelectStat() {
        return property.mapSelectStat(mapObjects);
    }

    public Stat getInterfaceStat() {
        return property.mapInterfaceStat(mapObjects);
    }

    public Cost getInterfaceCost() {
        return property.mapInterfaceCost(mapObjects);
    }

    public boolean isHighlight() {
        Type type = property.property.getType();
        return !(type instanceof DataClass && ((DataClass<?>) type).markupHtml()); // ts_headline breaks html : https://stackoverflow.com/questions/40263956/why-is-postgresql-stripping-html-entities-in-ts-headline
    }

    @Override
    public ImSet<Property> getChangeProps() {
        MSet<Property> mProps = SetFact.mSetMax(orders.size() + 1);
        property.mapFillDepends(mProps);
        for(PropertyInterfaceImplement<P> order : orders.keyIt())
            order.mapFillDepends(mProps);
        return mProps.immutable();
    }

    @Override
    public ActionOrProperty<?> getCacheKey() {
        return property.property;
    }

    @Override
    protected ImOrderMap<PropertyInterfaceImplement<P>, Boolean> getCacheOrders() {
        return orders;
    }
}
