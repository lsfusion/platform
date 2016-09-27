package lsfusion.server.logics.property.derived;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.PropertyChanges;

// связь один к одному
public class AggregateGroupProperty<T extends PropertyInterface> extends CycleGroupProperty<T ,PropertyInterface> {

    private final CalcPropertyInterfaceImplement<T> whereProp;
    private final T aggrInterface;
    private final ImSet<CalcPropertyInterfaceImplement<T>> groupProps;

    // чисто из-за ограничения конструктора
    public static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(LocalizedString caption, ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> property, T aggrInterface, ImSet<CalcPropertyInterfaceImplement<T>> groupProps) {
        CalcPropertyMapImplement<?, T> and = DerivedProperty.createAnd(innerInterfaces, aggrInterface, property);
        if(caption.isEmpty()) {
            ImCol<CalcPropertyMapImplement<?, T>> groupMapProps = CalcPropertyMapImplement.filter(groupProps);
            for(CalcPropertyMapImplement<?, T> groupProp : groupMapProps)
                caption = LocalizedString.create(caption.getSourceString() + "," + groupProp.property.toString());
            if(groupMapProps.size() > 1)
                caption = LocalizedString.create("(" + caption.getSourceString() + ")"); 
        } else
            caption = LocalizedString.create(caption.getSourceString() + "(агр.)");
        and.property.caption = caption;
        assert groupProps.toSet().containsAll(innerInterfaces.removeIncl(aggrInterface));
        return create(caption, and, groupProps, innerInterfaces, property, aggrInterface, groupProps);
    }

    // чисто для generics
    private static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(LocalizedString caption, CalcPropertyInterfaceImplement<T> and, ImCol<CalcPropertyInterfaceImplement<T>> groupInterfaces, ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> whereProp, T aggrInterface, ImSet<CalcPropertyInterfaceImplement<T>> groupProps) {
        return new AggregateGroupProperty<>(caption, and, groupInterfaces, innerInterfaces, whereProp, aggrInterface, groupProps);
    }

    private AggregateGroupProperty(LocalizedString caption, CalcPropertyInterfaceImplement<T> and, ImCol<CalcPropertyInterfaceImplement<T>> groupInterfaces, ImSet<T> innerInterfaces, CalcPropertyInterfaceImplement<T> whereProp, T aggrInterface, ImSet<CalcPropertyInterfaceImplement<T>> groupProps) {
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
    public ActionPropertyMapImplement<?, Interface<T>> getSetNotNullAction(boolean notNull) {
        if(notNull) {
            PropertyInterface addedObject = new PropertyInterface();
            ImRevMap<CalcPropertyInterfaceImplement<T>, Interface<T>> aggrInterfaces = getMapInterfaces().toRevExclMap().reverse();

            ImRevMap<T, PropertyInterface> propValues = MapFact.addRevExcl(MapFact.singletonRev(aggrInterface, addedObject), // aggrInterface = aggrObject, остальные из row'а читаем
                    aggrInterfaces.filterInclRev(innerInterfaces.removeIncl(aggrInterface))); // assert что будут все в aggrInterfaces

            MList<ActionPropertyMapImplement<?, PropertyInterface>> mActions = ListFact.mList();
            if(whereProp instanceof CalcPropertyMapImplement)
                mActions.add(((CalcPropertyMapImplement<?, T>) whereProp).getSetNotNullAction(true).map(propValues));
            for(int i=0,size= aggrInterfaces.size();i<size;i++) {
                CalcPropertyInterfaceImplement<T> keyImplement = aggrInterfaces.getKey(i);
                if(keyImplement instanceof CalcPropertyMapImplement) {
                    CalcPropertyMapImplement<?, PropertyInterface> change = ((CalcPropertyMapImplement<?, T>) keyImplement).map(propValues);
                    Interface<T> valueInterface = aggrInterfaces.getValue(i);
                    ImSet<PropertyInterface> usedInterfaces = change.mapping.valuesSet().addExcl(valueInterface); // assert что не будет
                    mActions.add(DerivedProperty.createSetAction(usedInterfaces, change, (PropertyInterface) valueInterface));
                }
            }

            ImSet<PropertyInterface> setInnerInterfaces = SetFact.addExcl(interfaces, addedObject);
            return BaseUtils.immutableCast(DerivedProperty.createForAction(setInnerInterfaces, BaseUtils.<ImSet<PropertyInterface>>immutableCast(interfaces),
                    DerivedProperty.createListAction(setInnerInterfaces, mActions.immutableList()), addedObject, null, false));
        } else
            return super.getSetNotNullAction(notNull);
    }

}



