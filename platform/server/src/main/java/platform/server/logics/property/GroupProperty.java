package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.drilldown.DrillDownFormEntity;
import platform.server.form.entity.drilldown.GroupDrillDownFormEntity;
import platform.server.logics.BusinessLogics;
import platform.server.session.PropertyChanges;

import static platform.base.BaseUtils.capitalize;
import static platform.server.logics.ServerResourceBundle.getString;

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
        ImRevMap<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);
        Where w = Expr.getWhere(getGroupImplements(mapKeys, PropertyChanges.EMPTY))
                .and(Expr.getWhere(getExprImplements(mapKeys, PropertyChanges.EMPTY)))
                .and(Expr.getWhere(getOrderImplements(mapKeys, PropertyChanges.EMPTY).keys()))
                ;

        ClassWhere<I> classWhere = w.getClassWhere().get(mapKeys, true);

        return classWhere.getCommonParent(innerInterfaces);
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
