package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.session.DataSession;
import platform.server.data.PropertyField;
import platform.server.data.query.Union;
import platform.server.data.query.OperationQuery;

import java.util.Map;
import java.util.Collection;
import java.util.Set;

public class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public SumGroupProperty(TableFactory iTableFactory, Property<T> iProperty) {super(iTableFactory,iProperty,1);}

    public void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        // Group на ->1, Interface на ->2 - как было - возвр. 1 (на подчищение если (0 или 2) LEFT JOIN'им старые)
        groupProperty.setChangeType(RequiredTypes,1);

        for(GroupPropertyInterface<T> Interface : interfaces)
            if(Interface.implement instanceof PropertyMapImplement)
                (((PropertyMapImplement)Interface.implement).property).setChangeType(RequiredTypes,2);
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

    public Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return 1;
    }
}
