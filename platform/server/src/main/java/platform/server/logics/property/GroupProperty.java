package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.*;

abstract public class GroupProperty<I extends PropertyInterface> extends ComplexIncrementProperty<GroupProperty.Interface<I>> {

    public static class Interface<I extends PropertyInterface> extends PropertyInterface<Interface<I>> {
        public PropertyInterfaceImplement<I> implement;

        public Interface(int ID,PropertyInterfaceImplement<I> implement) {
            super(ID);
            this.implement = implement;
        }
    }

    private static <I extends PropertyInterface> List<Interface<I>> getInterfaces(Collection<? extends PropertyInterfaceImplement<I>> interfaceImplements) {
        List<Interface<I>> interfaces = new ArrayList<Interface<I>>();
        for(PropertyInterfaceImplement<I> implement : interfaceImplements)
            interfaces.add(new Interface<I>(interfaces.size(),implement));
        return interfaces;
    }

    protected abstract GroupType getGroupType();

    protected GroupProperty(String sID, String caption, Collection<I> innerInterfaces, Collection<? extends PropertyInterfaceImplement<I>> groupInterfaces) {
        super(sID, caption, getInterfaces(groupInterfaces));
        this.innerInterfaces = innerInterfaces;
    }

    public Map<Interface<I>,PropertyInterfaceImplement<I>> getMapInterfaces() {
        Map<Interface<I>,PropertyInterfaceImplement<I>> mapInterfaces = new HashMap<Interface<I>, PropertyInterfaceImplement<I>>();
        for(GroupProperty.Interface<I> propertyInterface : interfaces)
            mapInterfaces.put(propertyInterface,propertyInterface.implement);
        return mapInterfaces;
    }

    protected abstract List<PropertyInterfaceImplement<I>> getProps();
    protected abstract OrderedMap<PropertyInterfaceImplement<I>, Boolean> getOrders();
    protected final Collection<I> innerInterfaces;

    protected Map<Interface<I>, Expr> getGroupImplements(Map<I, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) {
        return getGroupImplements(mapKeys, modifier, null);
    }

    protected Map<Interface<I>, Expr> getGroupImplements(Map<I, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<Interface<I>, Expr> group = new HashMap<Interface<I>, Expr>();
        for(Interface<I> propertyInterface : interfaces)
            group.put(propertyInterface,propertyInterface.implement.mapExpr(mapKeys, modifier, changedWhere));
        return group;
    }

    protected OrderedMap<Expr, Boolean> getOrderImplements(Map<I, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        return getOrderImplements(joinImplement, modifier, null);
    }

    protected OrderedMap<Expr, Boolean> getOrderImplements(Map<I, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        OrderedMap<Expr, Boolean> result = new OrderedMap<Expr, Boolean>();
        for(Map.Entry<PropertyInterfaceImplement<I>, Boolean> order : getOrders().entrySet())
            result.put(order.getKey().mapExpr(joinImplement, modifier, changedWhere), order.getValue());
        return result;
    }

    protected List<Expr> getExprImplements(Map<I, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        return getExprImplements(joinImplement, modifier, null);
    }

    protected List<Expr> getExprImplements(Map<I, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        List<Expr> exprs = new ArrayList<Expr>();
        for(PropertyInterfaceImplement<I> expr : getProps())
            exprs.add(expr.mapExpr(joinImplement, modifier, changedWhere));
        return exprs;
    }

    // не очень хорошо, так как берет на себя часть функций компилятора (проталкивание значений), но достаточно неплохо должна помогать оптимизации
    protected Map<I, Expr> getGroupKeys(Map<Interface<I>, ? extends Expr> joinImplement) {
        Map<PropertyInterfaceImplement<I>, Expr> interfaceValues = new HashMap<PropertyInterfaceImplement<I>, Expr>();
        for(Map.Entry<Interface<I>, ? extends Expr> entry : joinImplement.entrySet())
            if(entry.getValue().isValue())
                interfaceValues.put(entry.getKey().implement, entry.getValue());
        return BaseUtils.replace(KeyExpr.getMapKeys(innerInterfaces), interfaceValues);
    }

    @Override
    public void fillDepends(Set<Property> depends, boolean derived) {
        for(Interface interfaceImplement : interfaces)
            interfaceImplement.implement.mapFillDepends(depends);
        fillDepends(depends, getProps());
        fillDepends(depends, getOrders().keySet());
    }
}
