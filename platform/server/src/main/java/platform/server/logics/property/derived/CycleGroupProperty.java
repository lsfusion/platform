package platform.server.logics.property.derived;

import platform.interop.Compare;
import platform.server.caches.GenericLazy;
import platform.server.caches.IdentityLazy;
import platform.server.classes.LogicalClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.Join;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CycleGroupProperty<T extends PropertyInterface,P extends PropertyInterface> extends MaxGroupProperty<T> {

    final Property<P> toChange;

    public CycleGroupProperty(String sID, String caption, Collection<PropertyInterfaceImplement<T>> interfaces, Property<T> property, Property<P> toChange) {
        super(sID, caption, interfaces, property);

        this.toChange = toChange;
    }

    @IdentityLazy
    public Property getConstrainedProperty(boolean checkChange) {
        // создает ограничение на "одинаковость" всех группировочных св-в
        // I1=I1' AND … In = In' AND G!=G' == false
        Property constraint = DerivedProperty.createPartition(groupProperty.interfaces, DerivedProperty.<T>createStatic(true, LogicalClass.instance),
                getMapInterfaces().values(), groupProperty.getImplement(), new HashMap<T, JoinProperty.Interface>(), Compare.GREATER);
        constraint.caption = "Нарушена уникальность свойства (" + toChange.caption + ") для объектов :";
        constraint.setConstraint(checkChange);
        return constraint;
    }

    @Override
    protected <U extends Changes<U>> U calculateUsedDataChanges(Modifier<U> modifier) {
        return MaxChangeProperty.getUsedChanges(this,toChange,modifier);
    }

    @Override
    protected MapDataChanges<Interface<T>> calculateDataChanges(PropertyChange<Interface<T>> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {

        Map<P,KeyExpr> toChangeKeys = toChange.getMapKeys();
        Expr resultExpr = getChangeExpr(change, modifier, toChangeKeys);
//        return toChange.getDataChanges(new PropertyChange<P>(toChangeKeys,resultExpr,resultExpr.getWhere()),changedWhere,modifier);
        DataChanges dataChanges = toChange.getDataChanges(new PropertyChange<P>(toChangeKeys,resultExpr,resultExpr.getWhere().or(getNullWhere(change, modifier, toChangeKeys))),null, modifier).changes;
        if(changedWhere!=null) {
            if(SIMPLE_SCHEME)
                changedWhere.add(change.where);
            else
                getExpr(change.mapKeys, new DataChangesModifier(modifier, dataChanges), changedWhere);
        }
        return new MapDataChanges<Interface<T>>(dataChanges);
    }

    private Expr getChangeExpr(PropertyChange<Interface<T>> change, Modifier<? extends Changes> modifier, Map<P,KeyExpr> toChangeKeys) {
        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys();
        
        // для G=newValue - изменением toChange
        // сделать чтобы все\хоть один I1=M1 AND I2=M2 AND … In=Mn AND G=newValue выполнялось - !FALSE - была хоть одна
        // берем I1=M1 AND I2=M2 AND … In=Mn, G=newValue and changed, "заменяя" DataProperty (C1=J1..CN=JN,D), группируем по C1,…,Cn,D ставим getWhere - отбираем
        Modifier<? extends Changes> changeModifier = toChange.getChangeModifier(modifier, false);
        WhereBuilder changedWhere = new WhereBuilder();
        Join<String> changeJoin = change.getQuery("value").join(getGroupImplements(mapKeys, changeModifier, changedWhere));
        // группируем по новому значению, интерфейсам, а также по изменению toChange
        Where compareWhere = groupProperty.getExpr(mapKeys, changeModifier, changedWhere).compare(changeJoin.getExpr("value"), Compare.EQUALS).and(changeJoin.getWhere());

        return GroupExpr.create(toChange.getChangeExprs(), toChange.changeExpr, changedWhere.toWhere().and(compareWhere), true, toChangeKeys, (PullExpr)toChange.changeExpr);
    }

    private Where getNullWhere(PropertyChange<Interface<T>> change, Modifier<? extends Changes> modifier, Map<P,KeyExpr> toChangeKeys) {
        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys();

        // для G!=newValue, изменением toChange на null
        // сделать чтобы I1=M1 AND I2=M2 … In=Mn не выполнялось == FALSE - не было вообще
        // берем I1=M1, I2=M2, …, In=Mn, G!=newValue and changed (and один из всех Ii - null), группируем по C1, …, Cn получаем те кого null'им в changeProperty
        // пока не будем проверять на G!=newValue пусть все зануляет
        Modifier<? extends Changes> changeModifier = toChange.getChangeModifier(modifier,true);
        WhereBuilder newOldChangedWhere = new WhereBuilder();

        Where newOldWhere = Where.FALSE;
        for(Interface<T> groupInterface : interfaces)
            newOldWhere = newOldWhere.or(groupInterface.implement.mapExpr(mapKeys,changeModifier,newOldChangedWhere).getWhere().not());
        newOldWhere = newOldWhere.and(groupProperty.getExpr(mapKeys,modifier,null).getWhere());

        return GroupExpr.create(toChange.getChangeExprs(), ValueExpr.TRUE, newOldChangedWhere.toWhere().and(change.getQuery("value").join(getGroupImplements(mapKeys, modifier, null)).getWhere().and(newOldWhere)), true, toChangeKeys).getWhere();
    }
}
