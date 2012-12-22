package platform.server.logics.property;

import platform.base.Result;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

public class SumGroupProperty<I extends PropertyInterface> extends AddGroupProperty<I> {

    @Override
    protected GroupType getGroupType() {
        return GroupType.SUM;
    }

    public SumGroupProperty(String sID, String caption, ImSet<I> innerInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, CalcPropertyInterfaceImplement<I> property) {
        super(sID, caption, innerInterfaces, groupInterfaces, property);

        finalizeInit();
    }

    public SumGroupProperty(String sID, String caption, ImCol<? extends CalcPropertyInterfaceImplement<I>> interfaces, CalcProperty<I> property) {
        super(sID, caption, interfaces, property);

        finalizeInit();
    }

    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, ImMap<Interface<I>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return changedExpr.diff(changedPrevExpr).sum(getExpr(joinImplement));
    }

    private CalcPropertyMapImplement<ClassPropertyInterface, Interface<I>> nullImplement;
    public CalcPropertyMapImplement<?, I> distribute;

    public <L extends PropertyInterface> void setDataChanges(ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> mapOrders, CalcPropertyMapImplement<L, I> restriction, boolean over) {
        ImRevMap<I,L> revMapping = restriction.mapping.reverse();

        ImOrderMap<CalcPropertyInterfaceImplement<L>, Boolean> orders = DerivedProperty.mapImplements(mapOrders, revMapping);

        nullImplement = DerivedProperty.createDataProp(true, this);

        distribute = DerivedProperty.createUGProp(new CalcPropertyImplement<ClassPropertyInterface, CalcPropertyInterfaceImplement<L>>(nullImplement.property,
                nullImplement.mapping.join(DerivedProperty.mapImplements(getMapInterfaces(), revMapping))),
                orders, restriction.property, over).map(restriction.mapping);
    }

    @Override
    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        if(distribute != null) {
            MSet<CalcProperty> mImplementDepends = SetFact.mSet();
            groupProperty.mapFillDepends(mImplementDepends);
            ImSet<CalcProperty> implementDepends = mImplementDepends.immutable();
            return SetFact.add(distribute.property.getUsedChanges(propChanges), propChanges.getUsedDataChanges(implementDepends), propChanges.getUsedChanges(implementDepends));
        } else
            return super.calculateUsedDataChanges(propChanges);
    }

    // такая же помошь компилятору как и при getExpr в GroupProperty
    private Where getGroupKeys(PropertyChange<Interface<I>> propertyChange, Result<ImRevMap<I, KeyExpr>> mapKeys, Result<ImMap<I, Expr>> mapValueKeys) {
        ImMap<CalcPropertyInterfaceImplement<I>, Expr> changeValues = propertyChange.getMapExprs().mapKeys(new GetValue<CalcPropertyInterfaceImplement<I>, Interface<I>>() {
            public CalcPropertyInterfaceImplement<I> getMapValue(Interface<I> value) {
                return value.implement;
            }});

        ImRevMap<I, KeyExpr> innerKeys = KeyExpr.getMapKeys(innerInterfaces);

        Where valueWhere = Where.TRUE;
        ImValueMap<I,Expr> mvMapValueKeys = innerKeys.mapItValues();// есть совместная обработка
        for(int i=0,size=innerKeys.size();i<size;i++) {
            Expr expr = changeValues.get(innerKeys.getKey(i));
            if(expr!=null) {
                mvMapValueKeys.mapValue(i, expr);
                valueWhere = valueWhere.and(innerKeys.getValue(i).compare(expr, Compare.EQUALS));
            } else
                mvMapValueKeys.mapValue(i, innerKeys.getValue(i));
        }

        mapKeys.set(innerKeys);
        mapValueKeys.set(mvMapValueKeys.immutableValue());
        return valueWhere;
    }

    @Override
    public ImSet<DataProperty> getChangeProps() {
        if(distribute!=null)
            return groupProperty.mapChangeProps();
        return super.getChangeProps();
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface<I>> propertyChange, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(distribute != null) {
            // создаем распределяющее свойство от этого, moidfier который меняет это свойство на PropertyChange, получаем значение распределяющего и условие на изменение
            // зацепит лишние changed'ы как и в MaxChangeExpr и иже с ними но пока забьем

            Result<ImRevMap<I, KeyExpr>> mapKeys = new Result<ImRevMap<I, KeyExpr>>(); Result<ImMap<I, Expr>> mapValueKeys = new Result<ImMap<I, Expr>>();
            Where valueWhere = getGroupKeys(propertyChange, mapKeys, mapValueKeys);

            PropertyChanges mapChanges = new PropertyChanges(nullImplement.property, propertyChange.map(nullImplement.mapping));

            Where nullWhere = propertyChange.getWhere(getGroupImplements(mapValueKeys.result, propChanges)).and(groupProperty.mapExpr(mapValueKeys.result, propChanges).getWhere()); // where чтобы за null'ить
            if(!nullWhere.isFalse())
                mapChanges = groupProperty.mapJoinDataChanges(mapKeys.result, CaseExpr.NULL, nullWhere.and(valueWhere), null, propChanges).add(mapChanges);

            Expr distributeExpr = distribute.mapExpr(mapValueKeys.result, mapChanges.add(propChanges));
            DataChanges dataChanges = groupProperty.mapJoinDataChanges(mapKeys.result, distributeExpr, distributeExpr.getWhere().or(nullWhere).and(valueWhere), null, propChanges);
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
