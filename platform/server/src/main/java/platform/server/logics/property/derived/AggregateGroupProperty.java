package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

// связь один к одному
public class AggregateGroupProperty<T extends PropertyInterface, J extends PropertyInterface> extends CycleGroupProperty<J, PropertyInterface> {

    private final Map<J, T> mapping;

    public Map<T, J> getMapping() {
        return BaseUtils.reverse(mapping);
    }

    private final Property<T> property;
    private final T aggrInterface;
    private final Collection<PropertyMapImplement<?, T>> groupProps;

    // чисто из-за ограничения конструктора
    public static <T extends PropertyInterface<T>> AggregateGroupProperty<T, ?> create(String sID, String caption, Property<T> property, T aggrInterface, Collection<PropertyMapImplement<?, T>> groupProps) {
        PropertyMapImplement<?, T> and = DerivedProperty.createAnd(property.interfaces, aggrInterface, property.getImplement());
        return create(sID, caption, and, BaseUtils.merge(groupProps, BaseUtils.remove(property.interfaces, aggrInterface)), property, aggrInterface, groupProps);
    }

    // чисто для generics
    private static <T extends PropertyInterface<T>, J extends PropertyInterface<J>> AggregateGroupProperty<T, J> create(String sID, String caption, PropertyMapImplement<J, T> and, Collection<PropertyInterfaceImplement<T>> groupInterfaces, Property<T> property, T aggrInterface, Collection<PropertyMapImplement<?, T>> groupProps) {
        return new AggregateGroupProperty<T, J>(sID, caption, and, DerivedProperty.mapImplements(groupInterfaces, BaseUtils.reverse(and.mapping)), property, aggrInterface, groupProps);
    }

    private AggregateGroupProperty(String sID, String caption, PropertyMapImplement<J, T> and, Collection<PropertyInterfaceImplement<J>> groupInterfaces, Property<T> property, T aggrInterface, Collection<PropertyMapImplement<?, T>> groupProps) {
        super(sID, caption, groupInterfaces, and.property, null);

        mapping = and.mapping;
        this.property = property;
        this.aggrInterface = aggrInterface;
        this.groupProps = groupProps;
    }

    // для этого во многом и делалось
    @Override
    protected boolean noIncrement() {
        return false;
    }

    @Override
    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Map<Interface<J>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        return changedExpr.ifElse(changedExpr.getWhere(), getExpr(joinImplement).and(changedPrevExpr.getWhere().not()));
    }

    @Override
    public void setNotNull(Map<Interface<J>, KeyExpr> mapKeys, Where where, DataSession session, BusinessLogics<?> BL) throws SQLException {
        Map<PropertyInterfaceImplement<T>, Interface<J>> aggrInterfaces = BaseUtils.reverse(DerivedProperty.mapImplements(getMapInterfaces(), mapping));

        for(Map<Interface<J>, DataObject> row : new Query<Interface<J>, Object>(mapKeys, where).executeClasses(session.sql, session.env, session.baseClass).keySet()) {
            DataObject aggrObject = session.addObject();

            Map<PropertyInterfaceImplement<T>, DataObject> interfaceValues = BaseUtils.join(aggrInterfaces, row);
            Map<T, DataObject> propValues = BaseUtils.merge(Collections.singletonMap(aggrInterface, aggrObject), // aggrInterface = aggrObject, остальные из row'а читаем
                    BaseUtils.filterKeys(interfaceValues, BaseUtils.remove(property.interfaces, aggrInterface)));

            Map<T, KeyExpr> mapPropKeys = property.getMapKeys();
            property.setNotNull(mapPropKeys, EqualsWhere.compareValues(mapPropKeys, propValues), session, BL);

            for(Map.Entry<PropertyMapImplement<?, T>, DataObject> propertyInterface : BaseUtils.filterKeys(interfaceValues, groupProps).entrySet())
                propertyInterface.getKey().execute(propValues, session, propertyInterface.getValue().object, session.modifier);
        }
    }
}



