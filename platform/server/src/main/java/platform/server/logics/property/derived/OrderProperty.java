package platform.server.logics.property.derived;

import platform.server.logics.property.FunctionProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyInterfaceImplement;
import platform.server.logics.property.Property;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.OrderExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.base.OrderedMap;
import platform.base.BaseUtils;

import java.util.*;

public class OrderProperty<T extends PropertyInterface> extends FunctionProperty<OrderProperty.Interface<T>> {

    public OrderProperty(String sID, String caption, Property<T> property, Collection<PropertyInterfaceImplement<T>> partitions, OrderedMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean includeLast) {
        super(sID, caption, getInterfaces(property));
        this.property = property;
        this.partitions = partitions;
        this.orders = orders;
        this.includeLast = includeLast;
    }

    Property<T> property;
    Collection<PropertyInterfaceImplement<T>> partitions;
    OrderedMap<PropertyInterfaceImplement<T>,Boolean> orders;
    boolean includeLast;

    public static class Interface<T extends PropertyInterface> extends PropertyInterface<Interface<T>> {
        public T propertyInterface;

        public Interface(int ID,T propertyInterface) {
            super(ID);
            this.propertyInterface = propertyInterface;
        }
    }

    private static <T extends PropertyInterface> List<Interface<T>> getInterfaces(Property<T> property) {
        List<Interface<T>> interfaces = new ArrayList<Interface<T>>();
        for(T propertyInterface : property.interfaces)
            interfaces.add(new Interface<T>(interfaces.size(),propertyInterface));
        return interfaces;
    }

    @Override
    protected void fillDepends(Set<Property> depends) {
        depends.add(property);
        fillDepends(depends,partitions);
        fillDepends(depends,orders.keySet());
    }

    public Map<Interface<T>,T> getMapInterfaces() {
        Map<Interface<T>,T> mapInterfaces = new HashMap<Interface<T>, T>();
        for(Interface<T> propertyInterface : interfaces)
            mapInterfaces.put(propertyInterface,propertyInterface.propertyInterface);
        return mapInterfaces;
    }

    private Map<PropertyInterfaceImplement<T>,Expr> getPartitionImplements(Map<T, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Map<PropertyInterfaceImplement<T>,Expr> result = new HashMap<PropertyInterfaceImplement<T>,Expr>();
        for(PropertyInterfaceImplement<T> partition : partitions)
            result.put(partition,partition.mapExpr(joinImplement, modifier, changedWhere));
        return result;
    }

    protected Expr calculateExpr(Map<Interface<T>, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        Map<T, KeyExpr> mapKeys = property.getMapKeys();

        WhereBuilder orderWhere = cascadeWhere(changedWhere);
        Map<PropertyInterfaceImplement<T>,Expr> partitionImplements = getPartitionImplements(mapKeys, modifier, orderWhere);
        List<Expr> orderExprs = new ArrayList<Expr>();
        for(int i=0;i<orders.size();i++)
            orderExprs.add(orders.getKey(i).mapExpr(mapKeys, modifier, orderWhere));
        Expr propertyExpr = property.getExpr(mapKeys, modifier, orderWhere);

        Map<KeyExpr, ? extends Expr> mapExprs = BaseUtils.crossJoin(BaseUtils.join(getMapInterfaces(),mapKeys),joinImplement);        

        if(changedWhere!=null) { // изменившиеся ряды (orderWhere) -> ряды с изменившимися partition'ами -> изменившиеся записи
            changedWhere.add(GroupExpr.create(partitionImplements, ValueExpr.TRUE, orderWhere.toWhere(), true, partitionImplements).getWhere().map(mapExprs));
            changedWhere.add(GroupExpr.create(getPartitionImplements(mapKeys, defaultModifier, null), ValueExpr.TRUE, orderWhere.toWhere(), true, partitionImplements).getWhere().map(mapExprs));
        }

        return OrderExpr.create(propertyExpr, orderExprs, new HashSet<Expr>(partitionImplements.values()), mapExprs);
    }
}
