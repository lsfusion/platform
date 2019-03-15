package lsfusion.server.logics.property.set;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// связь один к одному
public class AggregateGroupProperty<T extends PropertyInterface> extends CycleGroupProperty<T ,PropertyInterface> {

    private final PropertyInterfaceImplement<T> whereProp;
    private final T aggrInterface;
    private final ImSet<PropertyInterfaceImplement<T>> groupProps;

    // чисто из-за ограничения конструктора
    public static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(LocalizedString caption, ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> property, T aggrInterface, ImSet<PropertyInterfaceImplement<T>> groupProps) {
        PropertyMapImplement<?, T> and = PropertyFact.createAnd(innerInterfaces, aggrInterface, property);
        if(caption.isEmpty()) {
            ImCol<PropertyMapImplement<?, T>> groupMapProps = PropertyMapImplement.filter(groupProps);
            for(PropertyMapImplement<?, T> groupProp : groupMapProps)
                caption = LocalizedString.concat(caption, (caption.isEmpty() ? "" : ", ") + groupProp.property.toString());
            if(groupMapProps.size() > 1)
                caption = LocalizedString.concatList("(", caption, ")"); 
        } else
            caption = LocalizedString.concat(caption, "(агр.)");
        and.property.caption = caption;
        assert groupProps.toSet().containsAll(innerInterfaces.removeIncl(aggrInterface));
        return create(caption, and, groupProps, innerInterfaces, property, aggrInterface, groupProps);
    }

    // чисто для generics
    private static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(LocalizedString caption, PropertyInterfaceImplement<T> and, ImCol<PropertyInterfaceImplement<T>> groupInterfaces, ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> whereProp, T aggrInterface, ImSet<PropertyInterfaceImplement<T>> groupProps) {
        return new AggregateGroupProperty<>(caption, and, groupInterfaces, innerInterfaces, whereProp, aggrInterface, groupProps);
    }

    private AggregateGroupProperty(LocalizedString caption, PropertyInterfaceImplement<T> and, ImCol<PropertyInterfaceImplement<T>> groupInterfaces, ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> whereProp, T aggrInterface, ImSet<PropertyInterfaceImplement<T>> groupProps) {
        super(caption, innerInterfaces, groupInterfaces, and, null);

        this.whereProp = whereProp;
        this.aggrInterface = aggrInterface;
        this.groupProps = groupProps;
    }

    // для этого во многом и делалось
    @Override
    protected boolean noIncrement() {
        return false;
    }

    @Override
    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, ImMap<Interface<T>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return changedExpr.ifElse(changedExpr.getWhere(), prevExpr.and(changedPrevExpr.getWhere().not()));
    }

    @Override
    public ActionMapImplement<?, Interface<T>> getSetNotNullAction(boolean notNull) {
        if(notNull) {
            PropertyInterface addedObject = new PropertyInterface();
            ImRevMap<PropertyInterfaceImplement<T>, Interface<T>> aggrInterfaces = getMapInterfaces().toRevExclMap().reverse();

            ImRevMap<T, PropertyInterface> propValues = MapFact.addRevExcl(MapFact.singletonRev(aggrInterface, addedObject), // aggrInterface = aggrObject, остальные из row'а читаем
                    aggrInterfaces.filterInclRev(innerInterfaces.removeIncl(aggrInterface))); // assert что будут все в aggrInterfaces

            MList<ActionMapImplement<?, PropertyInterface>> mActions = ListFact.mList();
            if(whereProp instanceof PropertyMapImplement)
                mActions.add(((PropertyMapImplement<?, T>) whereProp).getSetNotNullAction(true).map(propValues));
            for(int i=0,size= aggrInterfaces.size();i<size;i++) {
                PropertyInterfaceImplement<T> keyImplement = aggrInterfaces.getKey(i);
                if(keyImplement instanceof PropertyMapImplement) {
                    PropertyMapImplement<?, PropertyInterface> change = ((PropertyMapImplement<?, T>) keyImplement).map(propValues);
                    Interface<T> valueInterface = aggrInterfaces.getValue(i);
                    ImSet<PropertyInterface> usedInterfaces = change.mapping.valuesSet().addExcl(valueInterface); // assert что не будет
                    mActions.add(PropertyFact.createSetAction(usedInterfaces, change, (PropertyInterface) valueInterface));
                }
            }

            ImSet<PropertyInterface> setInnerInterfaces = SetFact.addExcl(interfaces, addedObject);
            return BaseUtils.immutableCast(PropertyFact.createForAction(setInnerInterfaces, BaseUtils.<ImSet<PropertyInterface>>immutableCast(interfaces),
                    PropertyFact.createListAction(setInnerInterfaces, mActions.immutableList()), addedObject, null, false));
        } else
            return super.getSetNotNullAction(notNull);
    }

    public boolean isFullAggr;
    public ImSet<StoredDataProperty> getFullAggrProps() {
        if(isFullAggr)
            return interfaces.mapSetValues(new GetValue<StoredDataProperty, Interface<T>>() {
                @Override
                public StoredDataProperty getMapValue(Interface<T> value) {
                    return (StoredDataProperty) ((PropertyMapImplement<?, T>)value.implement).property;
                }
            });
        return null;
    } 
}



