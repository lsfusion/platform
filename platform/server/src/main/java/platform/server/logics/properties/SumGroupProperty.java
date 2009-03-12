package platform.server.logics.properties;

import platform.server.data.PropertyField;
import platform.server.data.query.OperationQuery;
import platform.server.data.Union;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public SumGroupProperty(String iSID, Collection<GroupPropertyInterface<T>> iInterfaces, TableFactory iTableFactory, Property<T> iProperty) {
        super(iSID, iInterfaces, iTableFactory, iProperty, 1);
    }

    public void fillRequiredChanges(Integer incrementType, Map<Property, Integer> requiredTypes) {
        // Group на ->1, Interface на ->2 - как было - возвр. 1 (на подчищение если (0 или 2) LEFT JOIN'им старые)
        groupProperty.setChangeType(requiredTypes,1);

        for(GroupPropertyInterface<T> propertyInterface : interfaces)
            if(propertyInterface.implement instanceof PropertyMapImplement)
                (((PropertyMapImplement)propertyInterface.implement).property).setChangeType(requiredTypes,2);
    }

    public Change incrementChanges(DataSession session, int changeType) {

        // конечный результат, с ключами и выражением
        OperationQuery<GroupPropertyInterface<T>, PropertyField> resultQuery = new OperationQuery<GroupPropertyInterface<T>,PropertyField>(interfaces, Union.SUM);
        ValueClassSet<GroupPropertyInterface<T>> resultClass = new ValueClassSet<GroupPropertyInterface<T>>();

        resultQuery.add(getGroupQuery(getChangeMap(session,1), changeTable.value,resultClass),1);
        resultQuery.add(getGroupQuery(getChangeImplements(session,0), changeTable.value,resultClass),1);
        resultQuery.add(getGroupQuery(getPreviousImplements(session), changeTable.value,resultClass),-1);

        return new Change(1,resultQuery,resultClass);
     }

    public Integer getIncrementType(Collection<Property> changedProps, Set<Property> toWait) {
        return 1;
    }
}
