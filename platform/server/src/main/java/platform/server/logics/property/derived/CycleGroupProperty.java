package platform.server.logics.property.derived;

import platform.base.Result;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Join;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.*;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

public class CycleGroupProperty<I extends PropertyInterface, P extends PropertyInterface> extends MaxGroupProperty<I> {

    @Override
    public GroupType getGroupType() {
        return GroupType.ANY;
    }

    final CalcProperty<P> toChange;

    public CycleGroupProperty(String sID, String caption, ImSet<I> innerInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> groupInterfaces, CalcPropertyInterfaceImplement<I> property, CalcProperty<P> toChange) {
        super(sID, caption, innerInterfaces, groupInterfaces, property, false);
        this.toChange = toChange;
    }

    public CycleGroupProperty(String sID, String caption, ImCol<CalcPropertyInterfaceImplement<I>> interfaces, CalcProperty<I> property, CalcProperty<P> toChange) {
        super(sID, caption, interfaces, property, false);
        this.toChange = toChange;
    }

    @IdentityInstanceLazy
    public CalcProperty getConstrainedProperty() {
        // создает ограничение на "одинаковость" всех группировочных св-в
        // I1=I1' AND … In = In' AND G!=G' == false
        CalcProperty constraint = DerivedProperty.createPartition(innerInterfaces, DerivedProperty.<I>createTrue(),
                getMapInterfaces().values(), groupProperty, new Result<ImRevMap<I, JoinProperty.Interface>>(), Compare.GREATER);
        constraint.caption = ServerResourceBundle.getString("logics.property.derived.violate.property.uniqueness.for.objects", groupProperty.toString());
        return constraint;
    }

    @Override
    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        if(toChange!=null)
            return MaxChangeProperty.getUsedChanges(this,toChange, propChanges);
        else
            return SetFact.EMPTY();
    }

    @Override
    public ImSet<DataProperty> getChangeProps() {
        if(toChange!=null)
            return toChange.getChangeProps();
        return super.getChangeProps();
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface<I>> change, WhereBuilder changedWhere, PropertyChanges propChanges) {

        if(toChange!=null) {
            ImRevMap<P,KeyExpr> toChangeKeys = toChange.getMapKeys();
            Expr resultExpr = getChangeExpr(change, propChanges, toChangeKeys);
            DataChanges dataChanges = toChange.getDataChanges(new PropertyChange<P>(toChangeKeys,resultExpr,resultExpr.getWhere().or(getNullWhere(change, propChanges, toChangeKeys))), propChanges);
            if(changedWhere!=null) {
                if (Settings.get().isCalculateGroupDataChanged())
                    getExpr(change.getMapExprs(), dataChanges.add(propChanges), changedWhere);
                else
                    changedWhere.add(change.where);
            }
            return dataChanges;
        } else
            return DataChanges.EMPTY;

    }

    private Expr getChangeExpr(PropertyChange<Interface<I>> change, PropertyChanges propChanges, ImRevMap<P,KeyExpr> toChangeKeys) {
        ImRevMap<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);
        
        // для G=newValue - изменением toChange
        // сделать чтобы все\хоть один I1=M1 AND I2=M2 AND … In=Mn AND G=newValue выполнялось - !FALSE - была хоть одна
        // берем I1=M1 AND I2=M2 AND … In=Mn, G=newValue and changed, "заменяя" DataProperty (C1=J1..CN=JN,D), группируем по C1,…,Cn,D ставим getWhere - отбираем
        PropertyChanges changeModifier = toChange.getChangeModifier(propChanges, false);
        WhereBuilder changedWhere = new WhereBuilder();
        Join<String> changeJoin = change.join(getGroupImplements(mapKeys, changeModifier, changedWhere));
        // группируем по новому значению, интерфейсам, а также по изменению toChange
        Where compareWhere = groupProperty.mapExpr(mapKeys, changeModifier, changedWhere).compare(changeJoin.getExpr("value"), Compare.EQUALS).and(changeJoin.getWhere());

        return GroupExpr.create(toChange.getChangeExprs(), toChange.getChangeExpr(), changedWhere.toWhere().and(compareWhere), getGroupType(), toChangeKeys, (PullExpr)toChange.getChangeExpr());
    }

    private Where getNullWhere(PropertyChange<Interface<I>> change, PropertyChanges propChanges, ImRevMap<P,KeyExpr> toChangeKeys) {
        ImRevMap<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);

        // для G!=newValue, изменением toChange на null
        // сделать чтобы I1=M1 AND I2=M2 … In=Mn не выполнялось == FALSE - не было вообще
        // берем I1=M1, I2=M2, …, In=Mn, G!=newValue and changed (and один из всех Ii - null), группируем по C1, …, Cn получаем те кого null'им в changeProperty
        // пока не будем проверять на G!=newValue пусть все зануляет
        PropertyChanges changeModifier = toChange.getChangeModifier(propChanges, true);
        WhereBuilder newOldChangedWhere = new WhereBuilder();

        Where newOldWhere = Where.FALSE;
        for(Interface<I> groupInterface : interfaces)
            newOldWhere = newOldWhere.or(groupInterface.implement.mapExpr(mapKeys,changeModifier,newOldChangedWhere).getWhere().not());
        newOldWhere = newOldWhere.and(groupProperty.mapExpr(mapKeys, propChanges).getWhere());

        return GroupExpr.create(toChange.getChangeExprs(), newOldChangedWhere.toWhere().and(change.getWhere(getGroupImplements(mapKeys, propChanges)).and(newOldWhere)), toChangeKeys).getWhere();
    }

    @Override
    public boolean supportsDrillDown() {
        return false;
    }
}
