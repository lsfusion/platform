package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.*;

abstract public class GroupProperty<I extends PropertyInterface> extends ComplexIncrementProperty<GroupProperty.Interface<I>> {

    public static class Interface<I extends PropertyInterface> extends PropertyInterface<Interface<I>> {
        public CalcPropertyInterfaceImplement<I> implement;

        public Interface(int ID,CalcPropertyInterfaceImplement<I> implement) {
            super(ID);
            this.implement = implement;
        }
    }

    private static <I extends PropertyInterface> List<Interface<I>> getInterfaces(Collection<? extends CalcPropertyInterfaceImplement<I>> interfaceImplements) {
        List<Interface<I>> interfaces = new ArrayList<Interface<I>>();
        for(CalcPropertyInterfaceImplement<I> implement : interfaceImplements)
            interfaces.add(new Interface<I>(interfaces.size(),implement));
        return interfaces;
    }

    protected abstract GroupType getGroupType();

    protected GroupProperty(String sID, String caption, Collection<I> innerInterfaces, Collection<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces) {
        super(sID, caption, getInterfaces(groupInterfaces));
        this.innerInterfaces = innerInterfaces;
    }

    public Map<Interface<I>,CalcPropertyInterfaceImplement<I>> getMapInterfaces() {
        Map<Interface<I>,CalcPropertyInterfaceImplement<I>> mapInterfaces = new HashMap<Interface<I>, CalcPropertyInterfaceImplement<I>>();
        for(GroupProperty.Interface<I> propertyInterface : interfaces)
            mapInterfaces.put(propertyInterface,propertyInterface.implement);
        return mapInterfaces;
    }

    protected abstract List<CalcPropertyInterfaceImplement<I>> getProps();
    protected abstract OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> getOrders();
    protected final Collection<I> innerInterfaces;

    protected Map<Interface<I>, Expr> getGroupImplements(Map<I, ? extends Expr> mapKeys, PropertyChanges changes) {
        return getGroupImplements(mapKeys, false, changes);
    }

    protected Map<Interface<I>, Expr> getGroupImplements(Map<I, ? extends Expr> mapKeys, boolean propClasses, PropertyChanges changes) {
        return getGroupImplements(mapKeys, propClasses, changes, null);
    }

    protected Map<Interface<I>, Expr> getGroupImplements(Map<I, ? extends Expr> mapKeys, PropertyChanges changes, WhereBuilder changedWhere) {
        return getGroupImplements(mapKeys, false, changes, changedWhere);
    }

    protected Map<Interface<I>, Expr> getGroupImplements(Map<I, ? extends Expr> mapKeys, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        Map<Interface<I>, Expr> group = new HashMap<Interface<I>, Expr>();
        for(Interface<I> propertyInterface : interfaces)
            group.put(propertyInterface,propertyInterface.implement.mapExpr(mapKeys, propClasses, changes, changedWhere));
        return group;
    }

    protected OrderedMap<Expr, Boolean> getOrderImplements(Map<I, ? extends Expr> joinImplement, PropertyChanges changes) {
        return getOrderImplements(joinImplement, false, changes);
    }

    protected OrderedMap<Expr, Boolean> getOrderImplements(Map<I, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes) {
        return getOrderImplements(joinImplement, propClasses, changes, null);
    }

    protected OrderedMap<Expr, Boolean> getOrderImplements(Map<I, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        OrderedMap<Expr, Boolean> result = new OrderedMap<Expr, Boolean>();
        for(Map.Entry<CalcPropertyInterfaceImplement<I>, Boolean> order : getOrders().entrySet())
            result.put(order.getKey().mapExpr(joinImplement, propClasses, changes, changedWhere), order.getValue());
        return result;
    }

    protected List<Expr> getExprImplements(Map<I, ? extends Expr> joinImplement, PropertyChanges changes) {
        return getExprImplements(joinImplement, false, changes, null);
    }

    protected List<Expr> getExprImplements(Map<I, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes) {
        return getExprImplements(joinImplement, propClasses, changes, null);
    }

    protected List<Expr> getExprImplements(Map<I, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        List<Expr> exprs = new ArrayList<Expr>();
        for(CalcPropertyInterfaceImplement<I> expr : getProps())
            exprs.add(expr.mapExpr(joinImplement, propClasses, changes, changedWhere));
        return exprs;
    }

    // не очень хорошо, так как берет на себя часть функций компилятора (проталкивание значений), но достаточно неплохо должна помогать оптимизации
    protected Map<I, Expr> getGroupKeys(Map<Interface<I>, ? extends Expr> joinImplement) {
        Map<CalcPropertyInterfaceImplement<I>, Expr> interfaceValues = new HashMap<CalcPropertyInterfaceImplement<I>, Expr>();
        for(Map.Entry<Interface<I>, ? extends Expr> entry : joinImplement.entrySet())
            if(entry.getValue().isValue())
                interfaceValues.put(entry.getKey().implement, entry.getValue());
        return BaseUtils.replace(KeyExpr.getMapKeys(innerInterfaces), interfaceValues);
    }

    @Override
    public void fillDepends(Set<CalcProperty> depends, boolean events) {
        for(Interface interfaceImplement : interfaces)
            interfaceImplement.implement.mapFillDepends(depends);
        fillDepends(depends, getProps());
        fillDepends(depends, getOrders().keySet());
    }
}
