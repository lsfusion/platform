package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.sql.SQLException;
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
    protected void proceedNotNull(PropertySet<Interface<T>> set, ExecutionEnvironment env, boolean notNull) throws SQLException {
        if(notNull) {
            Map<CalcPropertyInterfaceImplement<T>, Interface<T>> aggrInterfaces = BaseUtils.reverse(getMapInterfaces());

            for(Map<Interface<T>, DataObject> row : set.executeClasses(env)) {
                DataObject aggrObject = env.getSession().addObject();

                Map<CalcPropertyInterfaceImplement<T>, DataObject> interfaceValues = BaseUtils.join(aggrInterfaces, row);
                Map<T, DataObject> propValues = BaseUtils.merge(Collections.singletonMap(aggrInterface, aggrObject), // aggrInterface = aggrObject, остальные из row'а читаем
                        BaseUtils.filterKeys(interfaceValues, BaseUtils.remove(innerInterfaces, aggrInterface)));

                if(whereProp instanceof CalcPropertyMapImplement)
                    ((CalcPropertyMapImplement<?,T>)whereProp).mapNotNull(propValues, env, true, false); // потому как только что добавился объект
                for(Map.Entry<CalcPropertyInterfaceImplement<T>, DataObject> propertyInterface : BaseUtils.filterKeys(interfaceValues, groupProps).entrySet())
                    if(propertyInterface.getKey() instanceof CalcPropertyMapImplement)
                        ((CalcPropertyMapImplement<?,T>)propertyInterface.getKey()).change(propValues, env, propertyInterface.getValue());
            }
        } else
            super.proceedNotNull(set, env, notNull);
    }

    @Override
    public Set<CalcProperty> getSetChangeProps(boolean notNull, boolean add) {
        if(notNull) {
            Set<CalcProperty> result = new HashSet<CalcProperty>();
            if(whereProp instanceof CalcPropertyMapImplement)
                result.addAll(((CalcProperty)((CalcPropertyMapImplement) whereProp).property).getSetChangeProps(true, true));
            for(CalcPropertyInterfaceImplement<T> groupProp : groupProps)
                if(groupProp instanceof CalcPropertyMapImplement)
                    result.addAll(((CalcProperty)(((CalcPropertyMapImplement)groupProp).property)).getChangeProps());
            return result;
        } else
            return super.getSetChangeProps(notNull, add);
    }
}



