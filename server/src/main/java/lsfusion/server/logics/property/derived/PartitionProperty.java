package lsfusion.server.logics.property.derived;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.AggrExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.PartitionExpr;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.SimpleIncrementProperty;
import lsfusion.server.session.PropertyChanges;

public class PartitionProperty<T extends PropertyInterface> extends SimpleIncrementProperty<PartitionProperty.Interface<T>> {

    protected final PartitionType partitionType;

    protected final ImSet<T> innerInterfaces;
    protected final ImList<CalcPropertyInterfaceImplement<T>> props;
    protected final ImOrderMap<CalcPropertyInterfaceImplement<T>,Boolean> orders;
    protected final boolean ordersNotNull;
    protected final ImSet<CalcPropertyInterfaceImplement<T>> partitions;
    protected boolean includeLast;

    public PartitionProperty(String sID, String caption, PartitionType partitionType, ImSet<T> innerInterfaces, ImList<CalcPropertyInterfaceImplement<T>> props, ImSet<CalcPropertyInterfaceImplement<T>> partitions, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, boolean includeLast) {
        super(sID, caption, getInterfaces(innerInterfaces));
        this.innerInterfaces = innerInterfaces;
        this.props = props;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
        this.partitionType = partitionType;
        this.partitions = partitions;
        this.includeLast = includeLast;

        finalizeInit();
   }

    @Override
    protected void fillDepends(MSet<CalcProperty> depends, boolean events) {
        fillDepends(depends, orders.keys());
        fillDepends(depends, partitions);
        fillDepends(depends, props.getCol());
    }

    public static class Interface<T extends PropertyInterface> extends PropertyInterface<Interface<T>> {
        public T propertyInterface;

        public Interface(int ID,T propertyInterface) {
            super(ID);
            this.propertyInterface = propertyInterface;
        }
    }

    private static <T extends PropertyInterface> ImOrderSet<Interface<T>> getInterfaces(ImSet<T> innerInterfaces) {
        return innerInterfaces.mapColSetValues(new GetIndexValue<Interface<T>, T>() {
            public Interface<T> getMapValue(int i, T value) {
                return new Interface<T>(i, value);
            }}).toOrderSet();
    }

    public ImRevMap<Interface<T>,T> getMapInterfaces() {
        return interfaces.mapRevValues(new GetValue<T, Interface<T>>() {
            public T getMapValue(Interface<T> value) {
                return value.propertyInterface;
            }});
    }

    // кривовать как и в GroupProperty, перетягивание на себя функций компилятора (то есть с третьего ограничивается второй), но достаточно хороший case оптимизации
    protected ImMap<T, Expr> getGroupKeys(ImMap<Interface<T>, ? extends Expr> joinImplement, Result<ImMap<KeyExpr, Expr>> mapExprs) {
        ImRevMap<T, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);

        ImMap<T, ? extends Expr> innerJoinImplement = getMapInterfaces().crossJoin(joinImplement);
        ImValueMap<T, Expr> mvResult = innerJoinImplement.mapItValues(); // есть последействие
        MExclMap<KeyExpr,Expr> mMapExprs = MapFact.mExclMapMax(innerJoinImplement.size());
        // читаем value из joinImplement, затем фильтруем partitions'ами
        for(int i=0,size=innerJoinImplement.size();i<size;i++) {
            T key = innerJoinImplement.getKey(i);
            Expr expr = innerJoinImplement.getValue(i);
            if(expr.isValue() && partitions.contains(key)) {
                mvResult.mapValue(i, expr);
            } else {
                KeyExpr keyExpr = mapKeys.get(key);
                mvResult.mapValue(i, keyExpr);
                mMapExprs.exclAdd(keyExpr, expr);
            }
        }
        mapExprs.set(mMapExprs.immutable());
        return mvResult.immutableValue();
    }

    protected ImMap<CalcPropertyInterfaceImplement<T>,Expr> getPartitionImplements(final ImMap<T, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return partitions.mapItValues(new GetValue<Expr, CalcPropertyInterfaceImplement<T>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<T> value) {
                return value.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            }});
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(final ImMap<T, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return orders.mapMergeItOrderKeys(new GetValue<Expr, CalcPropertyInterfaceImplement<T>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<T> value) {
                return value.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            }});
    }

    protected ImList<Expr> getExprImplements(final ImMap<T, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return props.mapItListValues(new GetValue<Expr, CalcPropertyInterfaceImplement<T>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<T> value) {
                return value.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            }});
    }

    protected Expr calculateExpr(ImMap<Interface<T>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {

        Result<ImMap<KeyExpr, Expr>> mapExprs = new Result<ImMap<KeyExpr, Expr>>();
        ImMap<T, ? extends Expr> mapKeys = getGroupKeys(joinImplement, mapExprs);

        WhereBuilder orderWhere = cascadeWhere(changedWhere);
        ImMap<CalcPropertyInterfaceImplement<T>,Expr> partitionImplements = getPartitionImplements(mapKeys, propClasses, propChanges, orderWhere);
        ImOrderMap<Expr, Boolean> orderExprs = getOrderImplements(mapKeys, propClasses, propChanges, orderWhere);
        ImList<Expr> exprs = getExprImplements(mapKeys, propClasses, propChanges, orderWhere);

        if(changedWhere!=null) { // изменившиеся ряды (orderWhere) -> ряды с изменившимися partition'ами -> изменившиеся записи
            changedWhere.add(getPartitionWhere(orderWhere.toWhere(), partitionImplements, exprs, orderExprs, mapExprs.result));
            changedWhere.add(getPartitionWhere(orderWhere.toWhere(), getPartitionImplements(mapKeys, propClasses, PropertyChanges.EMPTY, null),
                    getExprImplements(mapKeys, propClasses, PropertyChanges.EMPTY, null), getOrderImplements(mapKeys, propClasses, PropertyChanges.EMPTY, null), mapExprs.result));
        }

        return PartitionExpr.create(partitionType, exprs, orderExprs, ordersNotNull, partitionImplements.values().toSet(), mapExprs.result, null);
    }

    private Where getPartitionWhere(Where where, ImMap<CalcPropertyInterfaceImplement<T>,Expr> partitionImplements, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, ImMap<KeyExpr, Expr> mapExprs) {
        return GroupExpr.create(partitionImplements, where.and(Expr.getWhere(exprs)).and(AggrExpr.getOrderWhere(orders, ordersNotNull)), partitionImplements).getWhere().map(mapExprs);
    }
}
