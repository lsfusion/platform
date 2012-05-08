package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

// связь один к одному
public class AggregateGroupProperty<T extends PropertyInterface> extends CycleGroupProperty<T ,PropertyInterface> {

    private final PropertyInterfaceImplement<T> whereProp;
    private final T aggrInterface;
    private final Collection<PropertyInterfaceImplement<T>> groupProps;

    // чисто из-за ограничения конструктора
    public static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(String sID, String caption, Collection<T> innerInterfaces, PropertyInterfaceImplement<T> property, T aggrInterface, Collection<PropertyInterfaceImplement<T>> groupProps) {
        PropertyMapImplement<?, T> and = DerivedProperty.createAnd(innerInterfaces, aggrInterface, property);
        and.property.caption = caption + "(аггр.)";
        assert groupProps.containsAll(BaseUtils.remove(innerInterfaces, aggrInterface));
        return create(sID, caption, and, groupProps, innerInterfaces, property, aggrInterface, groupProps);
    }

    // чисто для generics
    private static <T extends PropertyInterface<T>> AggregateGroupProperty<T> create(String sID, String caption, PropertyInterfaceImplement<T> and, Collection<PropertyInterfaceImplement<T>> groupInterfaces, Collection<T> innerInterfaces, PropertyInterfaceImplement<T> whereProp, T aggrInterface, Collection<PropertyInterfaceImplement<T>> groupProps) {
        return new AggregateGroupProperty<T>(sID, caption, and, groupInterfaces, innerInterfaces, whereProp, aggrInterface, groupProps);
    }

    private AggregateGroupProperty(String sID, String caption, PropertyInterfaceImplement<T> and, Collection<PropertyInterfaceImplement<T>> groupInterfaces, Collection<T> innerInterfaces, PropertyInterfaceImplement<T> whereProp, T aggrInterface, Collection<PropertyInterfaceImplement<T>> groupProps) {
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
            Map<PropertyInterfaceImplement<T>, Interface<T>> aggrInterfaces = BaseUtils.reverse(getMapInterfaces());

            DataSession session = env.getSession();
            for(Map<Interface<T>, DataObject> row : set.executeClasses(session)) {
                DataObject aggrObject = session.addObject();

                Map<PropertyInterfaceImplement<T>, DataObject> interfaceValues = BaseUtils.join(aggrInterfaces, row);
                Map<T, DataObject> propValues = BaseUtils.merge(Collections.singletonMap(aggrInterface, aggrObject), // aggrInterface = aggrObject, остальные из row'а читаем
                        BaseUtils.filterKeys(interfaceValues, BaseUtils.remove(innerInterfaces, aggrInterface)));

                if(whereProp instanceof PropertyMapImplement)
                    ((PropertyMapImplement<?,T>)whereProp).mapNotNull(propValues, env, true, false); // потому как только что добавился объект
                for(Map.Entry<PropertyInterfaceImplement<T>, DataObject> propertyInterface : BaseUtils.filterKeys(interfaceValues, groupProps).entrySet())
                    if(propertyInterface.getKey() instanceof PropertyMapImplement)
                        ((PropertyMapImplement<?,T>)propertyInterface.getKey()).execute(propValues, env, propertyInterface.getValue(), null);
            }
        } else
            super.proceedNotNull(set, env, notNull);
    }

    @Override
    public Set<Property> getSetChangeProps(boolean notNull, boolean add) {
        if(notNull) {
            Set<Property> result = new HashSet<Property>();
            if(whereProp instanceof PropertyMapImplement)
                result.addAll(((PropertyMapImplement) whereProp).property.getSetChangeProps(true, true));
            for(PropertyInterfaceImplement<T> groupProp : groupProps)
                if(groupProp instanceof PropertyMapImplement)
                    result.addAll((((PropertyMapImplement)groupProp).property).getDataChanges());
            return result;
        } else
            return super.getSetChangeProps(notNull, add);
    }
}



