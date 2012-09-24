package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.util.*;

// связь один к одному
public class AggregateGroupProperty<T extends PropertyInterface> extends CycleGroupProperty<T ,PropertyInterface> {

    private final CalcPropertyInterfaceImplement<T> whereProp;
    private final T aggrInterface;
    private final Collection<CalcPropertyInterfaceImplement<T>> groupProps;

    // чисто из-за ограничения конструктора
    public static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(String sID, String caption, Collection<T> innerInterfaces, CalcPropertyInterfaceImplement<T> property, T aggrInterface, Collection<CalcPropertyInterfaceImplement<T>> groupProps) {
        CalcPropertyMapImplement<?, T> and = DerivedProperty.createAnd(innerInterfaces, aggrInterface, property);
        and.property.caption = caption + "(аггр.)";
        assert groupProps.containsAll(BaseUtils.remove(innerInterfaces, aggrInterface));
        return create(sID, caption, and, groupProps, innerInterfaces, property, aggrInterface, groupProps);
    }

    // чисто для generics
    private static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(String sID, String caption, CalcPropertyInterfaceImplement<T> and, Collection<CalcPropertyInterfaceImplement<T>> groupInterfaces, Collection<T> innerInterfaces, CalcPropertyInterfaceImplement<T> whereProp, T aggrInterface, Collection<CalcPropertyInterfaceImplement<T>> groupProps) {
        return new AggregateGroupProperty<T>(sID, caption, and, groupInterfaces, innerInterfaces, whereProp, aggrInterface, groupProps);
    }

    private AggregateGroupProperty(String sID, String caption, CalcPropertyInterfaceImplement<T> and, Collection<CalcPropertyInterfaceImplement<T>> groupInterfaces, Collection<T> innerInterfaces, CalcPropertyInterfaceImplement<T> whereProp, T aggrInterface, Collection<CalcPropertyInterfaceImplement<T>> groupProps) {
        super(sID, caption, innerInterfaces, groupInterfaces, and, null);

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
    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, Map<Interface<T>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return changedExpr.ifElse(changedExpr.getWhere(), prevExpr.and(changedPrevExpr.getWhere().not()));
    }

    @Override
    public ActionPropertyMapImplement<?, Interface<T>> getSetNotNullAction(boolean notNull) {
        if(notNull) {
            PropertyInterface addedObject = new PropertyInterface();
            Map<CalcPropertyInterfaceImplement<T>, Interface<T>> aggrInterfaces = BaseUtils.reverse(getMapInterfaces());

            Map<T, PropertyInterface> propValues = BaseUtils.merge(Collections.singletonMap(aggrInterface, addedObject), // aggrInterface = aggrObject, остальные из row'а читаем
                    BaseUtils.filterKeys(aggrInterfaces, BaseUtils.remove(innerInterfaces, aggrInterface))); // assert что будут все в aggrInterfaces

            List<ActionPropertyMapImplement<?, PropertyInterface>> actions = new ArrayList<ActionPropertyMapImplement<?, PropertyInterface>>();
            if(whereProp instanceof CalcPropertyMapImplement)
                actions.add(((CalcPropertyMapImplement<?, T>) whereProp).getSetNotNullAction(true).map(propValues));
            for(Map.Entry<CalcPropertyInterfaceImplement<T>, Interface<T>> propertyInterface : BaseUtils.filterKeys(aggrInterfaces, groupProps).entrySet())
                if(propertyInterface.getKey() instanceof CalcPropertyMapImplement) {
                    CalcPropertyMapImplement<?, PropertyInterface> change = ((CalcPropertyMapImplement<?, T>) propertyInterface.getKey()).map(propValues);
                    Collection<PropertyInterface> usedInterfaces = BaseUtils.add(change.mapping.values(), propertyInterface.getValue()); // assert что не будет
                    actions.add(DerivedProperty.createSetAction(usedInterfaces, change, (PropertyInterface)propertyInterface.getValue()));
                }


            Collection<PropertyInterface> setInnerInterfaces = BaseUtils.add(interfaces, addedObject);
            return BaseUtils.<ActionPropertyMapImplement<?, Interface<T>>>immutableCast(DerivedProperty.createForAction(setInnerInterfaces, new ArrayList<PropertyInterface>(interfaces),
                    DerivedProperty.createListAction(setInnerInterfaces, actions), addedObject, null, false));
        } else
            return super.getSetNotNullAction(notNull);
    }

}



