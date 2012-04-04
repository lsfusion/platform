package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.classes.LogicalClass;
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
import platform.server.session.*;

import java.util.*;

public class CycleGroupProperty<I extends PropertyInterface, P extends PropertyInterface> extends MaxGroupProperty<I> {

    @Override
    protected GroupType getGroupType() {
        return GroupType.ANY;
    }

    final Property<P> toChange;

    public CycleGroupProperty(String sID, String caption, Collection<I> innerInterfaces, Collection<? extends PropertyInterfaceImplement<I>> groupInterfaces, PropertyInterfaceImplement<I> property, Property<P> toChange) {
        super(sID, caption, innerInterfaces, groupInterfaces, property, false);
        this.toChange = toChange;
    }

    public CycleGroupProperty(String sID, String caption, Collection<PropertyInterfaceImplement<I>> interfaces, Property<I> property, Property<P> toChange) {
        super(sID, caption, interfaces, property, false);
        this.toChange = toChange;
    }

    @IdentityLazy
    public Property getConstrainedProperty(boolean checkChange) {
        // создает ограничение на "одинаковость" всех группировочных св-в
        // I1=I1' AND … In = In' AND G!=G' == false
        Property constraint = DerivedProperty.createPartition(innerInterfaces, DerivedProperty.<I>createStatic(true, LogicalClass.instance),
                getMapInterfaces().values(), groupProperty, new HashMap<I, JoinProperty.Interface>(), Compare.GREATER);
        constraint.caption = ServerResourceBundle.getString("logics.property.derived.violate.property.uniqueness.for.objects", groupProperty.toString());
        constraint.setConstraint(checkChange);
        return constraint;
    }

    @Override
    public Set<Property> getDataChangeProps() {
        if(toChange!=null)
            return Collections.<Property>singleton(toChange);
        return super.getDataChangeProps();
    }

    @Override
    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        if(toChange!=null)
            return MaxChangeProperty.getUsedChanges(this,toChange, propChanges);
        else
            return QuickSet.EMPTY();
    }

    @Override
    protected MapDataChanges<Interface<I>> calculateDataChanges(PropertyChange<Interface<I>> change, WhereBuilder changedWhere, PropertyChanges propChanges) {

        if(toChange!=null) {
            Map<P,KeyExpr> toChangeKeys = toChange.getMapKeys();
            Expr resultExpr = getChangeExpr(change, propChanges, toChangeKeys);
            DataChanges dataChanges = toChange.getDataChanges(new PropertyChange<P>(toChangeKeys,resultExpr,resultExpr.getWhere().or(getNullWhere(change, propChanges, toChangeKeys))), propChanges, null).changes;
            if(changedWhere!=null) {
                if (Settings.instance.isCalculateGroupDataChanged())
                    getExpr(change.mapKeys, dataChanges.add(propChanges), changedWhere);
                else
                    changedWhere.add(change.where);
            }
            return new MapDataChanges<Interface<I>>(dataChanges);
        } else
            return new MapDataChanges<Interface<I>>();

    }

    private Expr getChangeExpr(PropertyChange<Interface<I>> change, PropertyChanges propChanges, Map<P,KeyExpr> toChangeKeys) {
        Map<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);
        
        // для G=newValue - изменением toChange
        // сделать чтобы все\хоть один I1=M1 AND I2=M2 AND … In=Mn AND G=newValue выполнялось - !FALSE - была хоть одна
        // берем I1=M1 AND I2=M2 AND … In=Mn, G=newValue and changed, "заменяя" DataProperty (C1=J1..CN=JN,D), группируем по C1,…,Cn,D ставим getWhere - отбираем
        PropertyChanges changeModifier = toChange.getChangeModifier(propChanges, false);
        WhereBuilder changedWhere = new WhereBuilder();
        Join<String> changeJoin = change.join(getGroupImplements(mapKeys, changeModifier, changedWhere));
        // группируем по новому значению, интерфейсам, а также по изменению toChange
        Where compareWhere = groupProperty.mapExpr(mapKeys, changeModifier, changedWhere).compare(changeJoin.getExpr("value"), Compare.EQUALS).and(changeJoin.getWhere());

        return GroupExpr.create(toChange.getChangeExprs(), toChange.changeExpr, changedWhere.toWhere().and(compareWhere), getGroupType(), toChangeKeys, (PullExpr)toChange.changeExpr);
    }

    private Where getNullWhere(PropertyChange<Interface<I>> change, PropertyChanges propChanges, Map<P,KeyExpr> toChangeKeys) {
        Map<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);

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
}
