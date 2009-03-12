package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataChanges;
import platform.server.logics.session.DataSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract public class FormulaProperty<T extends FormulaPropertyInterface> extends AggregateProperty<T> {

    protected FormulaProperty(String iSID, Collection<T> iInterfaces, TableFactory iTableFactory) {
        super(iSID, iInterfaces, iTableFactory);
    }

    public void fillRequiredChanges(Integer incrementType, Map<Property, Integer> requiredTypes) {
    }

    // не может быть изменений в принципе
    public boolean fillChangedList(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate) {
        return false;
    }

    public Change incrementChanges(DataSession session, int changeType) {
        return null;
    }

    public Integer getIncrementType(Collection<Property> changedProps, Set<Property> toWait) {
        return null;
    }

}
