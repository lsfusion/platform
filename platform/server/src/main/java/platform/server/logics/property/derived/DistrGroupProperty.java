package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.util.*;

public class DistrGroupProperty<T extends PropertyInterface, L extends PropertyInterface> extends SumGroupProperty<T> {

    // чисто чтобы быстрее (не себя подставлять)
    private static class NullProperty<T extends PropertyInterface> extends FunctionProperty<NullProperty.Interface<T>> {

        private static <T extends PropertyInterface> List<Interface<T>> getInterfaces(Property<T> property) {
            List<Interface<T>> interfaces = new ArrayList<Interface<T>>();
            for(T propertyInterface : property.interfaces)
                interfaces.add(new Interface<T>(interfaces.size(),propertyInterface));
            return interfaces;
        }

        public NullProperty(Property<T> property) {
            super("pc"+property.hashCode(), property.toString(), getInterfaces(property));
        }

        public static class Interface<T extends PropertyInterface> extends PropertyInterface<Interface<T>> {
            final T mapInterface;

            public Interface(int ID, T mapInterface) {
                super(ID);
                this.mapInterface = mapInterface;
            }
        }

        public Map<Interface<T>,T> getMapInterfaces() {
            Map<Interface<T>,T> result = new HashMap<Interface<T>,T>();
            for(Interface<T> propertyInterface : interfaces)
                result.put(propertyInterface,propertyInterface.mapInterface);
            return result;
        }

        protected Expr calculateExpr(Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
            return CaseExpr.NULL;
        }

    }

    private final NullProperty<Interface<T>> nullProperty;
    private final PropertyMapImplement<? extends PropertyInterface, T> distribute;

    public DistrGroupProperty(String sID, String caption, Collection<PropertyInterfaceImplement<T>> interfaces, Property<T> property, OrderedMap<PropertyInterfaceImplement<T>,Boolean> mapOrders, PropertyMapImplement<L,T> restriction) {
        super(sID, caption, interfaces, property);
        OrderedMap<PropertyInterfaceImplement<L>, Boolean> orders = new OrderedMap<PropertyInterfaceImplement<L>, Boolean>();
        for(Map.Entry<PropertyInterfaceImplement<T>,Boolean> order : mapOrders.entrySet())
            orders.put(order.getKey().map(BaseUtils.reverse(restriction.mapping)),order.getValue());

        nullProperty = new NullProperty<Interface<T>>(this);

        distribute = DerivedProperty.createUGProp(new PropertyImplement<PropertyInterfaceImplement<L>, NullProperty.Interface<Interface<T>>>(nullProperty,
                BaseUtils.join(nullProperty.getMapInterfaces(), DerivedProperty.mapImplements(getMapInterfaces(), BaseUtils.reverse(restriction.mapping)))),
                orders, restriction.property).map(restriction.mapping);
    }

    @Override
    protected <U extends Changes<U>> U calculateUsedDataChanges(Modifier<U> modifier) {
        return distribute.property.getUsedChanges(modifier).add(groupProperty.getUsedChanges(modifier)).add(groupProperty.getUsedDataChanges(modifier));
    }

    @Override
    protected MapDataChanges<Interface<T>> calculateDataChanges(PropertyChange<Interface<T>> propertyChange, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        // создаем распределяющее свойство от этого, moidfier который меняет это свойство на PropertyChange, получаем значение распределяющего и условие на изменение
        // зацепит лишние changed'ы как и в MaxChangeExpr и иже с ними но пока забьем

        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys();

        PropertyChanges propertyChanges = new PropertyChanges(nullProperty, propertyChange.map(nullProperty.getMapInterfaces()));

        Where nullWhere = propertyChange.getQuery("value").join(getGroupImplements(mapKeys, modifier, null)).getWhere().and(
                groupProperty.getExpr(mapKeys, modifier, null).getWhere());
        if(!nullWhere.isFalse())
            propertyChanges = new PropertyChanges(propertyChanges, groupProperty.getDataChanges(new PropertyChange<T>(mapKeys, CaseExpr.NULL, nullWhere), null, modifier).changes);

        Expr distributeExpr = distribute.mapExpr(mapKeys, new PropertyChangesModifier(modifier, propertyChanges), null);
        DataChanges dataChanges = groupProperty.getDataChanges(new PropertyChange<T>(mapKeys, distributeExpr, distributeExpr.getWhere().or(nullWhere)), null, modifier).changes;
        if(changedWhere!=null) {
            if(SIMPLE_SCHEME)
                changedWhere.add(propertyChange.where);
            else
                getExpr(propertyChange.mapKeys, new DataChangesModifier(modifier, dataChanges), changedWhere);
        }
        return new MapDataChanges<Interface<T>>(dataChanges);
    }
}