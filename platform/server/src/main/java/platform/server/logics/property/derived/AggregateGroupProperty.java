package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.session.DataSession;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class AggregateGroupProperty extends CycleGroupProperty<ClassPropertyInterface, PropertyInterface> {

    private final ObjectValueProperty objectProperty;
    private final ConcreteCustomClass aggregateClass;

    public AggregateGroupProperty(String sID, String caption, ObjectValueProperty objectProperty, ConcreteCustomClass aggregateClass, Collection<PropertyInterfaceImplement<ClassPropertyInterface>> interfaces) {
        super(sID, caption, interfaces, objectProperty, null);

        this.objectProperty = objectProperty;
        this.aggregateClass = aggregateClass;
    }

    @Override
    public void setNotNull(Map<Interface<ClassPropertyInterface>, KeyExpr> mapKeys, Where where, DataSession session, BusinessLogics<?> BL) throws SQLException {
        for(Map<Interface<ClassPropertyInterface>, DataObject> row : new Query<Interface<ClassPropertyInterface>, Object>(mapKeys, where).executeClasses(session.sql, session.env, session.baseClass).keySet()) {
            DataObject aggrObject = session.addObject(aggregateClass, session.modifier);
            for(Map.Entry<Interface<ClassPropertyInterface>, DataObject> propertyInterface : row.entrySet())
                ((PropertyMapImplement<PropertyInterface, ClassPropertyInterface>) propertyInterface.getKey().implement).
                        execute(Collections.singletonMap(objectProperty.objectInterface, aggrObject), session, propertyInterface.getValue().object, session.modifier);
        }
    }
}



