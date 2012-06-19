package platform.server.logics.property;

import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.session.*;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.Settings;
import platform.base.OrderedMap;
import platform.base.BaseUtils;

import java.util.*;

public class SumGroupProperty<I extends PropertyInterface> extends AddGroupProperty<I> {

    @Override
    protected GroupType getGroupType() {
        return GroupType.SUM;
    }

    public SumGroupProperty(String sID, String caption, Collection<I> innerInterfaces, Collection<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, CalcPropertyInterfaceImplement<I> property) {
        super(sID, caption, innerInterfaces, groupInterfaces, property);

        finalizeInit();
    }

    public SumGroupProperty(String sID, String caption, Collection<? extends CalcPropertyInterfaceImplement<I>> interfaces, CalcProperty<I> property) {
        super(sID, caption, interfaces, property);

        finalizeInit();
    }

    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, Map<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return changedExpr.diff(changedPrevExpr).sum(getExpr(joinImplement));
    }

    private CalcPropertyMapImplement<ClassPropertyInterface, Interface<I>> nullImplement;
    public CalcPropertyMapImplement<?, I> distribute;

    public <L extends PropertyInterface> void setDataChanges(OrderedMap<CalcPropertyInterfaceImplement<I>, Boolean> mapOrders, CalcPropertyMapImplement<L, I> restriction, boolean over) {
        OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean> orders = new OrderedMap<CalcPropertyInterfaceImplement<L>, Boolean>();
        for(Map.Entry<CalcPropertyInterfaceImplement<I>,Boolean> order : mapOrders.entrySet())
            orders.put(order.getKey().map(BaseUtils.reverse(restriction.mapping)),order.getValue());

        nullImplement = DerivedProperty.createDataProp(true, this);

        distribute = DerivedProperty.createUGProp(new CalcPropertyImplement<ClassPropertyInterface, CalcPropertyInterfaceImplement<L>>(nullImplement.property,
                BaseUtils.join(nullImplement.mapping, DerivedProperty.mapImplements(getMapInterfaces(), BaseUtils.reverse(restriction.mapping)))),
                orders, restriction.property, over).map(restriction.mapping);
    }

    public Set<CalcProperty> getDataChangeProps() {
        if(distribute!=null)
            return BaseUtils.<CalcProperty>toSet(distribute.property, nullImplement.property);
        return super.getDataChangeProps();
    }

    @Override
    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        if(distribute != null) {
            Set<CalcProperty> implementDepends = new HashSet<CalcProperty>();
            groupProperty.mapFillDepends(implementDepends);
            return QuickSet.add(distribute.property.getUsedChanges(propChanges), propChanges.getUsedDataChanges(implementDepends), propChanges.getUsedChanges(implementDepends));
        } else
            return super.calculateUsedDataChanges(propChanges);
    }

    // такая же помошь компилятору как и при getExpr в GroupProperty
    private Where getGroupKeys(PropertyChange<Interface<I>> propertyChange, Map<I, KeyExpr> mapKeys, Map<I, Expr> mapValueKeys) {
        Map<CalcPropertyInterfaceImplement<I>, Expr> changeValues = new HashMap<CalcPropertyInterfaceImplement<I>, Expr>();
        for(Map.Entry<Interface<I>, Expr> keyValue : propertyChange.getMapExprs().entrySet())
            changeValues.put(keyValue.getKey().implement, keyValue.getValue());

        Where valueWhere = Where.TRUE;
        for(Map.Entry<I, KeyExpr> mapKey : KeyExpr.getMapKeys(innerInterfaces).entrySet()) {
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
    public Collection<DataProperty> getChangeProps() {
        if(distribute!=null)
            return groupProperty.mapChangeProps();
        return super.getChangeProps();
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface<I>> propertyChange, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(distribute != null) {
            // создаем распределяющее свойство от этого, moidfier который меняет это свойство на PropertyChange, получаем значение распределяющего и условие на изменение
            // зацепит лишние changed'ы как и в MaxChangeExpr и иже с ними но пока забьем

            Map<I, KeyExpr> mapKeys = new HashMap<I, KeyExpr>(); Map<I, Expr> mapValueKeys = new HashMap<I, Expr>();
            Where valueWhere = getGroupKeys(propertyChange, mapKeys, mapValueKeys);

            PropertyChanges mapChanges = new PropertyChanges(nullImplement.property, propertyChange.map(nullImplement.mapping));

            Where nullWhere = propertyChange.getWhere(getGroupImplements(mapValueKeys, propChanges)).and(groupProperty.mapExpr(mapValueKeys, propChanges).getWhere()); // where чтобы за null'ить
            if(!nullWhere.isFalse())
                mapChanges = groupProperty.mapJoinDataChanges(mapKeys, CaseExpr.NULL, nullWhere.and(valueWhere), null, propChanges).add(mapChanges);

            Expr distributeExpr = distribute.mapExpr(mapValueKeys, mapChanges.add(propChanges));
            DataChanges dataChanges = groupProperty.mapJoinDataChanges(mapKeys, distributeExpr, distributeExpr.getWhere().or(nullWhere).and(valueWhere), null, propChanges);
            if(changedWhere!=null) {
                if (Settings.instance.isCalculateGroupDataChanged())
                    getExpr(propertyChange.getMapExprs(), dataChanges.add(propChanges), changedWhere);
                else
                    changedWhere.add(propertyChange.where);
            }
            return dataChanges;
        } else
            return super.calculateDataChanges(propertyChange, changedWhere, propChanges);
    }
}
