package platform.server.logics.property;

import platform.interop.Compare;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.session.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.Settings;
import platform.base.OrderedMap;
import platform.base.BaseUtils;

import java.util.*;

public class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public SumGroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property, 1);
    }

    Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        return changedExpr.sum(changedPrevExpr.scale(-1)).sum(getExpr(joinImplement));
    }

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

    private NullProperty<Interface<T>> nullProperty;
    private PropertyMapImplement<? extends PropertyInterface, T> distribute;

    public <L extends PropertyInterface> void setDataChanges(OrderedMap<PropertyInterfaceImplement<T>,Boolean> mapOrders, PropertyMapImplement<L,T> restriction) {
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
        if(distribute != null)
            return distribute.property.getUsedChanges(modifier).add(groupProperty.getUsedChanges(modifier)).add(groupProperty.getUsedDataChanges(modifier));
        else
            return super.calculateUsedDataChanges(modifier);
    }

    // такая же помошь компилятору как и при getExpr в GroupProperty
    private Where getGroupKeys(PropertyChange<Interface<T>> propertyChange, Map<T, KeyExpr> mapKeys, Map<T, Expr> mapValueKeys) {
        Map<BaseExpr, BaseExpr> exprValues = propertyChange.where.getExprValues();
        Map<PropertyInterfaceImplement<T>, Expr> changeValues = new HashMap<PropertyInterfaceImplement<T>, Expr>();
        for(Map.Entry<Interface<T>, KeyExpr> mapKey : propertyChange.mapKeys.entrySet()) {
            BaseExpr exprValue = exprValues.get(mapKey.getValue());
            if(exprValue!=null)
                changeValues.put(mapKey.getKey().implement, exprValue);
        }
        Where valueWhere = Where.TRUE;
        for(Map.Entry<T, KeyExpr> mapKey : groupProperty.getMapKeys().entrySet()) {
            Expr expr = changeValues.get(mapKey.getKey());
            if(expr!=null) {
                mapValueKeys.put(mapKey.getKey(), expr);
                valueWhere = valueWhere.and(mapKey.getValue().compare(expr, Compare.EQUALS));
            } else
                mapValueKeys.put(mapKey.getKey(), mapKey.getValue());
            mapKeys.put(mapKey.getKey(), mapKey.getValue());
        }
        return valueWhere;
    }

    @Override
    protected MapDataChanges<Interface<T>> calculateDataChanges(PropertyChange<Interface<T>> propertyChange, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        if(distribute != null) {
            // создаем распределяющее свойство от этого, moidfier который меняет это свойство на PropertyChange, получаем значение распределяющего и условие на изменение
            // зацепит лишние changed'ы как и в MaxChangeExpr и иже с ними но пока забьем

            Map<T, KeyExpr> mapKeys = new HashMap<T, KeyExpr>(); Map<T, Expr> mapValueKeys = new HashMap<T, Expr>();
            Where valueWhere = getGroupKeys(propertyChange, mapKeys, mapValueKeys);

            PropertyChanges propertyChanges = new PropertyChanges(nullProperty, propertyChange.map(nullProperty.getMapInterfaces()));

            Where nullWhere = propertyChange.getWhere(getGroupImplements(mapValueKeys, modifier)).and(groupProperty.getExpr(mapValueKeys, modifier).getWhere()); // where чтобы за null'ить
            if(!nullWhere.isFalse())
                propertyChanges = new PropertyChanges(propertyChanges, groupProperty.getDataChanges(new PropertyChange<T>(mapKeys, CaseExpr.NULL, nullWhere.and(valueWhere)), null, modifier).changes);

            Expr distributeExpr = distribute.mapExpr(mapValueKeys, new PropertyChangesModifier(modifier, propertyChanges));
            DataChanges dataChanges = groupProperty.getDataChanges(new PropertyChange<T>(mapKeys, distributeExpr, distributeExpr.getWhere().or(nullWhere).and(valueWhere)), null, modifier).changes;
            if(changedWhere!=null) {
                if (Settings.instance.isCalculateGroupDataChanged())
                    getExpr(propertyChange.mapKeys, new DataChangesModifier(modifier, dataChanges), changedWhere);
                else
                    changedWhere.add(propertyChange.where);
            }
            return new MapDataChanges<Interface<T>>(dataChanges);
        } else
            return super.calculateDataChanges(propertyChange, changedWhere, modifier);
    }
}
