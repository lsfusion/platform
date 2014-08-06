package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.GroupDrillDownFormEntity;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;
import lsfusion.server.session.PropertyChanges;

import static lsfusion.server.logics.ServerResourceBundle.getString;

abstract public class GroupProperty<I extends PropertyInterface> extends ComplexIncrementProperty<GroupProperty.Interface<I>> {

    public static class Interface<I extends PropertyInterface> extends PropertyInterface<Interface<I>> {
        public CalcPropertyInterfaceImplement<I> implement;

        public Interface(int ID,CalcPropertyInterfaceImplement<I> implement) {
            super(ID);
            this.implement = implement;
        }
    }

    protected final ImSet<I> innerInterfaces;

    protected GroupProperty(String caption, ImSet<I> innerInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces) {
        super(caption, getInterfaces(groupInterfaces));
        this.innerInterfaces = innerInterfaces;
    }

    protected GroupProperty(String caption, ImSet<I> innerInterfaces, ImList<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces) {
        super(caption, getTempInterfaces(groupInterfaces));
        this.innerInterfaces = innerInterfaces;
    }
    
    public ImMap<Interface<I>,CalcPropertyInterfaceImplement<I>> getMapInterfaces() {
        return interfaces.mapValues(new GetValue<CalcPropertyInterfaceImplement<I>, Interface<I>>() {
            public CalcPropertyInterfaceImplement<I> getMapValue(Interface<I> value) {
                return value.implement;
            }});
    }

    public abstract GroupType getGroupType();

    public abstract ImList<CalcPropertyInterfaceImplement<I>> getProps();

    public abstract ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> getOrders();
    public abstract boolean getOrdersNotNull();

    public Inferred<I> inferInnerInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        GroupType groupType = getGroupType();
        return inferInnerInterfaceClasses(getProps().addList(getMapInterfaces().values().toList()),
                groupType.isSelect(), commonValue, getOrders(), getOrdersNotNull(), groupType.getSkipWhereIndex(), inferType);
    }
    public Inferred<I> inferInnerInterfaceClasses(final ImMap<Interface<I>, ExClassSet> inferred, InferType inferType) {
        ImList<CalcPropertyInterfaceImplement<I>> props = getProps();
        ImOrderSet<Interface<I>> orderInterfaces = getOrderInterfaces();
        return inferInnerInterfaceClasses(props.addList(orderInterfaces.mapList(getMapInterfaces())), getOrders(), getOrdersNotNull(), getGroupType().getSkipWhereIndex(), ListFact.toList((ExClassSet) null, props.size()).addList(orderInterfaces.mapList(inferred)), inferType);
    }
    public ExClassSet inferInnerValueClass(final ImMap<I, ExClassSet> commonClasses, InferType inferType) {
        return inferInnerValueClass(getProps(), commonClasses, getGroupType(), inferType);
    }

    @Override
    public Inferred<Interface<I>> calcInferInterfaceClasses(final ExClassSet commonValue, final InferType inferType) {
        final ImMap<I, ExClassSet> innerInferred = inferInnerInterfaceClasses(commonValue, inferType).finishEx(inferType);
        if(innerInferred == null)
            return Inferred.FALSE();

        ImMap<Interface<I>, CalcPropertyInterfaceImplement<I>> mapInterfaces = getMapInterfaces();
        return new Inferred<Interface<I>>(mapInterfaces.mapValues(new GetValue<ExClassSet, CalcPropertyInterfaceImplement<I>>() {
            public ExClassSet getMapValue(CalcPropertyInterfaceImplement<I> value) {
                return ExClassSet.toNotNull(value.mapInferValueClass(innerInferred, inferType));
            }
        }));
    }
    public ExClassSet calcInferValueClass(ImMap<Interface<I>, ExClassSet> inferred, InferType inferType) {
        ImMap<I, ExClassSet> innerInferred = inferInnerInterfaceClasses(inferred, inferType).finishEx(inferType);
        if(innerInferred == null)
            return ExClassSet.FALSE;
        return inferInnerValueClass(innerInferred, inferType);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, PropertyChanges changes) {
        return getGroupImplements(mapKeys, CalcType.EXPR, changes);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, CalcType calcType, PropertyChanges changes) {
        return getGroupImplements(mapKeys, calcType, changes, null);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, PropertyChanges changes, WhereBuilder changedWhere) {
        return getGroupImplements(mapKeys, CalcType.EXPR, changes, changedWhere);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(final ImMap<I, ? extends Expr> mapKeys, final CalcType calcType, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return interfaces.mapItValues(new GetValue<Expr, Interface<I>>() {
            public Expr getMapValue(Interface<I> value) {
                return value.implement.mapExpr(mapKeys, calcType, changes, changedWhere);
            }});
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(ImMap<I, ? extends Expr> joinImplement, PropertyChanges changes) {
        return getOrderImplements(joinImplement, CalcType.EXPR, changes);
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(ImMap<I, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges changes) {
        return getOrderImplements(joinImplement, calcType, changes, null);
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(final ImMap<I, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return getOrders().mapMergeItOrderKeys(new GetValue<Expr, CalcPropertyInterfaceImplement<I>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<I> value) {
                return value.mapExpr(joinImplement, calcType, changes, changedWhere);
            }
        });
    }

    protected ImList<Expr> getExprImplements(ImMap<I, ? extends Expr> joinImplement, PropertyChanges changes) {
        return getExprImplements(joinImplement, CalcType.EXPR, changes, null);
    }

    protected ImList<Expr> getExprImplements(ImMap<I, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges changes) {
        return getExprImplements(joinImplement, calcType, changes, null);
    }

    protected ImList<Expr> getExprImplements(final ImMap<I, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return getProps().mapItListValues(new GetValue<Expr, CalcPropertyInterfaceImplement<I>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<I> value) {
                return value.mapExpr(joinImplement, calcType, changes, changedWhere);
            }});
    }

    // не очень хорошо, так как берет на себя часть функций компилятора (проталкивание значений), но достаточно неплохо должна помогать оптимизации
    protected ImMap<I, Expr> getGroupKeys(ImMap<Interface<I>, ? extends Expr> joinImplement) {
        ImMap<I, ? extends Expr> interfaceValues = BaseUtils.immutableCast(getMapInterfaces().toRevExclMap().reverse().join((ImMap<Interface<I>, Expr>)joinImplement).filterFn(new GetKeyValue<Boolean, CalcPropertyInterfaceImplement<I>, Expr>() {
            public Boolean getMapValue(CalcPropertyInterfaceImplement<I> key, Expr value) {
                return value.isValue() && key instanceof PropertyInterface;
            }
        }));
        return MapFact.override(KeyExpr.getMapKeys(innerInterfaces), interfaceValues);
    }

    @Override
    public void fillDepends(MSet<CalcProperty> depends, boolean events) {
        for(Interface interfaceImplement : interfaces)
            interfaceImplement.implement.mapFillDepends(depends);
        fillDepends(depends, getProps().getCol());
        fillDepends(depends, getOrders().keys());
    }

    public ImMap<I, ValueClass> getInnerInterfaceClasses() {
        InferType inferType = InferType.PREVSAME;
        return ExClassSet.fromExValue(inferInnerInterfaceClasses((ExClassSet) null, inferType).finishEx(inferType));
/*        ImRevMap<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);
        Where w = Expr.getWhere(getGroupImplements(mapKeys, PropertyChanges.EMPTY))
                .and(Expr.getWhere(getExprImplements(mapKeys, PropertyChanges.EMPTY)))
                .and(Expr.getWhere(getOrderImplements(mapKeys, PropertyChanges.EMPTY).keys()))
                ;

        ClassWhere<I> classWhere = w.getClassWhere().get(mapKeys);

        return classWhere.getCommonParent(innerInterfaces);*/
    }

    protected boolean checkPrereadNull(ImMap<I, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges) {
        return JoinProperty.checkPrereadNull(joinImplement, true, getProps().getCol(), calcType, propChanges) ||
                JoinProperty.checkPrereadNull(joinImplement, true, interfaces.mapSetValues(new GetValue<CalcPropertyInterfaceImplement<I>, Interface<I>>() {
                    public CalcPropertyInterfaceImplement<I> getMapValue(Interface<I> value) {
                        return value.implement;
                    }}), calcType, propChanges) ||
                JoinProperty.checkPrereadNull(joinImplement, getOrdersNotNull(), getOrders().keys(), calcType, propChanges);
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM, String canonicalName) {
        return new GroupDrillDownFormEntity(
                canonicalName, getString("logics.property.drilldown.form.group." + getGroupType().name().toLowerCase()), this, LM
        );
    }

    private static <I extends PropertyInterface> ImOrderSet<Interface<I>> getTempInterfaces(ImList<? extends CalcPropertyInterfaceImplement<I>> interfaceImplements) {
        MOrderExclSet<Interface<I>> mResult = SetFact.mOrderExclSet(interfaceImplements.size());
        for (int i = 0, size = interfaceImplements.size(); i < size; i++)
            mResult.exclAdd(new Interface<I>(i, interfaceImplements.get(i)));
        return mResult.immutableOrder();
    }
    
    private static <I extends PropertyInterface> ImOrderSet<Interface<I>> getInterfaces(ImCol<? extends CalcPropertyInterfaceImplement<I>> interfaceImplements) {
        return ((ImCol<CalcPropertyInterfaceImplement<I>>) interfaceImplements).mapColSetValues(
                new GetIndexValue<Interface<I>, CalcPropertyInterfaceImplement<I>>() {
                    public Interface<I> getMapValue(int i, CalcPropertyInterfaceImplement<I> value) {
                        return new Interface<I>(i, value);
                    }
                }).toOrderSet();
    }
}
