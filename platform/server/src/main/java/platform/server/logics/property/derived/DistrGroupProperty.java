package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.*;

import java.util.Collection;
import java.util.Map;

import net.jcip.annotations.Immutable;

@Immutable
public class DistrGroupProperty<T extends PropertyInterface, L extends PropertyInterface> extends SumGroupProperty<T> {

    OrderedMap<PropertyInterfaceImplement<L>,Boolean> orders;
    PropertyMapImplement<L,T> restriction;

    public DistrGroupProperty(String sID, String caption, Collection<PropertyInterfaceImplement<T>> interfaces, Property<T> property, OrderedMap<PropertyInterfaceImplement<T>,Boolean> mapOrders, PropertyMapImplement<L,T> restriction) {
        super(sID, caption, interfaces, property);
        orders = new OrderedMap<PropertyInterfaceImplement<L>, Boolean>();
        for(Map.Entry<PropertyInterfaceImplement<T>,Boolean> order : mapOrders.entrySet())
            orders.put(order.getKey().map(BaseUtils.reverse(restriction.mapping)),order.getValue());
        this.restriction = restriction;
    }

    @Override
    public DataChanges getDataChanges(PropertyChange<Interface<T>> propertyChange, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        // создаем распределяющее свойство от этого, moidfier который меняет это свойство на PropertyChange, получаем значение распределяющего и условие на изменение
        // зацепит лишние changed'ы как и в MaxChangeExpr и иже с ними но пока забьем

        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys();
        Where nullWhere = propertyChange.getQuery("value").join(getGroupImplements(mapKeys, modifier, null)).getWhere().and(
                groupProperty.getExpr(mapKeys, modifier, null).getWhere());
        Modifier<? extends Changes> changeModifier = new DataChangesModifier(modifier, groupProperty.getDataChanges(new PropertyChange<T>(mapKeys, CaseExpr.NULL, nullWhere), null, modifier));

        ExprProperty<Interface<T>> exprProperty = new ExprProperty<Interface<T>>(propertyChange);
        Expr distributeExpr = DerivedProperty.createUGProp(new PropertyImplement<PropertyInterfaceImplement<L>, ExprProperty.Interface<Interface<T>>>(exprProperty,
                BaseUtils.crossJoin(exprProperty.getMapInterfaces(),DerivedProperty.mapImplements(getMapInterfaces(), BaseUtils.reverse(restriction.mapping)))),
                orders, restriction.property).map(restriction.mapping).mapExpr(mapKeys, changeModifier, null);
        return groupProperty.getDataChanges(new PropertyChange<T>(mapKeys, distributeExpr, distributeExpr.getWhere().or(nullWhere)), changedWhere, modifier);
    }
}