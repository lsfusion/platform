package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.GroupDrillDownFormEntity;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.session.PropertyChanges;

import static lsfusion.base.BaseUtils.capitalize;
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

    protected GroupProperty(String sID, String caption, ImSet<I> innerInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces) {
        super(sID, caption, getInterfaces(groupInterfaces));
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

    public ImMap<I, ValueClass> getInnerInterfaceCommonClasses(final ValueClass commonValue) {
        final boolean isSelect = getGroupType().isSelect();
        ImList<CalcPropertyInterfaceImplement<I>> props = getProps().addList(getMapInterfaces().values().toList()).addList(getOrders().keyOrderSet());
        return or(innerInterfaces, props, ListFact.toList(props.size(), new GetIndex<ValueClass>() {
                    public ValueClass getMapValue(int i) {
                        return isSelect && i==0 ? commonValue : null;
                    }}));
    }

    @Override
    public ImMap<Interface<I>, ValueClass> getInterfaceCommonClasses(final ValueClass commonValue) {
        return or(interfaces, super.getInterfaceCommonClasses(commonValue),
                MapFact.innerJoin(getMapInterfaces(), getInnerInterfaceCommonClasses(commonValue)));
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, PropertyChanges changes) {
        return getGroupImplements(mapKeys, false, changes);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, boolean propClasses, PropertyChanges changes) {
        return getGroupImplements(mapKeys, propClasses, changes, null);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, PropertyChanges changes, WhereBuilder changedWhere) {
        return getGroupImplements(mapKeys, false, changes, changedWhere);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(final ImMap<I, ? extends Expr> mapKeys, final boolean propClasses, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return interfaces.mapItValues(new GetValue<Expr, Interface<I>>() {
            public Expr getMapValue(Interface<I> value) {
                return value.implement.mapExpr(mapKeys, propClasses, changes, changedWhere);
            }});
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(ImMap<I, ? extends Expr> joinImplement, PropertyChanges changes) {
        return getOrderImplements(joinImplement, false, changes);
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(ImMap<I, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes) {
        return getOrderImplements(joinImplement, propClasses, changes, null);
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(final ImMap<I, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return getOrders().mapMergeItOrderKeys(new GetValue<Expr, CalcPropertyInterfaceImplement<I>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<I> value) {
                return value.mapExpr(joinImplement, propClasses, changes, changedWhere);
            }
        });
    }

    protected ImList<Expr> getExprImplements(ImMap<I, ? extends Expr> joinImplement, PropertyChanges changes) {
        return getExprImplements(joinImplement, false, changes, null);
    }

    protected ImList<Expr> getExprImplements(ImMap<I, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes) {
        return getExprImplements(joinImplement, propClasses, changes, null);
    }

    protected ImList<Expr> getExprImplements(final ImMap<I, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return getProps().mapItListValues(new GetValue<Expr, CalcPropertyInterfaceImplement<I>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<I> value) {
                return value.mapExpr(joinImplement, propClasses, changes, changedWhere);
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
        return getInnerInterfaceCommonClasses(null);
/*        ImRevMap<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);
        Where w = Expr.getWhere(getGroupImplements(mapKeys, PropertyChanges.EMPTY))
                .and(Expr.getWhere(getExprImplements(mapKeys, PropertyChanges.EMPTY)))
                .and(Expr.getWhere(getOrderImplements(mapKeys, PropertyChanges.EMPTY).keys()))
                ;

        ClassWhere<I> classWhere = w.getClassWhere().get(mapKeys);

        return classWhere.getCommonParent(innerInterfaces);*/
    }

    @Override
    public boolean supportsDrillDown() {
        return isFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new GroupDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.group." + getGroupType().name().toLowerCase()), this, BL
        );
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
