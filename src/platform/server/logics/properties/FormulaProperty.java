package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataChanges;
import platform.server.logics.session.DataSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract public class FormulaProperty<T extends FormulaPropertyInterface> extends AggregateProperty<T> {

    protected FormulaProperty(TableFactory iTableFactory) {
        super(iTableFactory);
    }

    public void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
    }

    // не может быть изменений в принципе
    public boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        return false;
    }

    public Change incrementChanges(DataSession session, int changeType) {
        return null;
    }

    public Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return null;
    }

}
