package lsfusion.server.logics.property.set;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.WindowExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.AggrExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.PartitionExpr;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.SimpleIncrementProperty;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class PartitionProperty<T extends PropertyInterface> extends SimpleIncrementProperty<PartitionProperty.Interface<T>> {

    protected final PartitionType partitionType;

    protected final ImSet<T> innerInterfaces;
    protected final ImList<PropertyInterfaceImplement<T>> props;
    protected final ImOrderMap<PropertyInterfaceImplement<T>,Boolean> orders;
    protected final boolean ordersNotNull;
    protected final ImSet<PropertyInterfaceImplement<T>> partitions;

    private final SelectTop<T> selectTop; // partitions should (and only them) contain window interfaces

    public PartitionProperty(LocalizedString caption, PartitionType partitionType, ImSet<T> innerInterfaces, ImList<PropertyInterfaceImplement<T>> props, ImSet<PropertyInterfaceImplement<T>> partitions, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull, SelectTop<T> selectTop) {
        super(caption, getInterfaces(innerInterfaces));
        this.innerInterfaces = innerInterfaces;
        this.props = props;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
        this.partitionType = partitionType;
        this.partitions = partitions;

        this.selectTop = selectTop;

        finalizeInit();
   }

    @Override
    protected void fillDepends(MSet<Property> depends, boolean events) {
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
        return innerInterfaces.mapColSetValues(Interface::new).toOrderSet();
    }

    public ImRevMap<Interface<T>,T> getMapInterfaces() {
        return interfaces.mapRevValues((Interface<T> value) -> value.propertyInterface);
    }

    // кривовать как и в GroupProperty, перетягивание на себя функций компилятора (то есть с третьего ограничивается второй), но достаточно хороший case оптимизации
    protected ImMap<T, Expr> getGroupKeys(ImMap<Interface<T>, ? extends Expr> joinImplement, Result<ImMap<KeyExpr, Expr>> mapExprs) {
        ImRevMap<T, KeyExpr> mapKeys = OrderGroupProperty.getMapKeys(innerInterfaces, selectTop);

        ImMap<T, ? extends Expr> innerJoinImplement = getMapInterfaces().crossJoin(joinImplement);
        ImValueMap<T, Expr> mvResult = innerJoinImplement.mapItValues(); // есть последействие
        MExclMap<KeyExpr,Expr> mMapExprs = MapFact.mExclMapMax(innerJoinImplement.size());
        // читаем value из joinImplement, затем фильтруем partitions'ами
        for(int i=0,size=innerJoinImplement.size();i<size;i++) {
            T key = innerJoinImplement.getKey(i);
            Expr expr = innerJoinImplement.getValue(i);
            if(expr.isValue() && partitions.contains(key) && !selectTop.contains(key)) {
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

    protected ImMap<PropertyInterfaceImplement<T>,Expr> getPartitionImplements(final ImMap<T, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return partitions.mapItValues(value -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere));
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(final ImMap<T, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return orders.mapMergeItOrderKeys(value -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere));
    }

    protected ImList<Expr> getExprImplements(final ImMap<T, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return props.mapItListValues(value -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere));
    }

    private boolean checkPrereadNull(ImMap<T, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, boolean checkChange) {
        return JoinProperty.checkPrereadNull(joinImplement, true, props.getCol(), calcType, propChanges, checkChange) ||
                JoinProperty.checkPrereadNull(joinImplement, true, partitions, calcType, propChanges, checkChange) ||
                JoinProperty.checkPrereadNull(joinImplement, ordersNotNull, orders.keys(), calcType, propChanges, checkChange);
    }
    
    protected Expr calculateExpr(ImMap<Interface<T>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {

        Result<ImMap<KeyExpr, Expr>> mapExprs = new Result<>();
        ImMap<T, ? extends Expr> mapKeys = getGroupKeys(joinImplement, mapExprs);
        
        if(checkPrereadNull(mapKeys, calcType, propChanges, changedWhere != null))
            return Expr.NULL();

        WhereBuilder orderWhere = cascadeWhere(changedWhere);
        ImMap<PropertyInterfaceImplement<T>,Expr> partitionImplements = getPartitionImplements(mapKeys, calcType, propChanges, orderWhere);
        ImOrderMap<Expr, Boolean> orderExprs = getOrderImplements(mapKeys, calcType, propChanges, orderWhere);
        ImList<Expr> exprs = getExprImplements(mapKeys, calcType, propChanges, orderWhere);

        if(changedWhere!=null) { // изменившиеся ряды (orderWhere) -> ряды с изменившимися partition'ами -> изменившиеся записи
            changedWhere.add(getPartitionWhere(orderWhere.toWhere(), partitionImplements, exprs, orderExprs, mapExprs.result));
            PropertyChanges prevPropChanges = getPrevPropChanges(calcType, propChanges);
            changedWhere.add(getPartitionWhere(orderWhere.toWhere(), getPartitionImplements(mapKeys, calcType, prevPropChanges, null),
                    getExprImplements(mapKeys, calcType, prevPropChanges, null), getOrderImplements(mapKeys, calcType, prevPropChanges, null), mapExprs.result));
        }

        return PartitionExpr.create(partitionType, exprs, orderExprs, ordersNotNull, partitionImplements.values().toSet(), mapExprs.result, null, calcType instanceof CalcClassType);
    }

    private Where getPartitionWhere(Where where, ImMap<PropertyInterfaceImplement<T>,Expr> partitionImplements, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, ImMap<KeyExpr, Expr> mapExprs) {
        if(!selectTop.isEmpty()) {
            partitionImplements = partitionImplements.remove((ImSet<? extends PropertyInterfaceImplement<T>>) selectTop.getParamsSet());
            mapExprs = mapExprs.removeFn(WindowExpr::is);
        }
        return GroupExpr.create(partitionImplements, where.and(Expr.getWhere(exprs)).and(AggrExpr.getOrderWhere(orders, ordersNotNull)), partitionImplements).getWhere().mapWhere(mapExprs);
    }

    @Override
    public Inferred<Interface<T>> calcInferInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        return inferInnerInterfaceClasses(commonValue, inferType).map(getMapInterfaces().reverse());
    }
    public ExClassSet calcInferValueClass(ImMap<Interface<T>, ExClassSet> inferred, InferType inferType) {
        return inferInnerValueClass(getMapInterfaces().crossJoin(inferred), inferType);
    }

    private Inferred<T> inferInnerInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        return inferInnerInterfaceClasses(props.addList(partitions.toList()), partitionType.isSelect(), commonValue, orders, ordersNotNull, -1, inferType);
    }
    public ExClassSet inferInnerValueClass(final ImMap<T, ExClassSet> commonClasses, InferType inferType) {
        return inferInnerValueClass(props, orders, commonClasses, partitionType, inferType);
    }
}
