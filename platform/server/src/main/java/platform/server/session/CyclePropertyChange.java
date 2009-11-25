package platform.server.session;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.GroupPropertyInterface;
import platform.server.logics.property.CycleGroupProperty;
import platform.server.logics.ObjectValue;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.classes.CustomClass;

import java.sql.SQLException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;

public class CyclePropertyChange<T extends PropertyInterface> extends PropertyChange<GroupPropertyInterface<T>, CycleGroupProperty<T>> {

    public CyclePropertyChange(CycleGroupProperty<T> property, Map<GroupPropertyInterface<T>, DataObject> mapping) {
        super(property, mapping);
    }

    public void change(DataSession session, ObjectValue newValue, boolean externalID, TableModifier<? extends TableChanges> modifier) throws SQLException {
        property.change(mapping, session, modifier, newValue, externalID);
    }

    public Collection<FilterNavigator> getFilters(ObjectNavigator valueObject, BusinessLogics<?> BL) {
        return new ArrayList<FilterNavigator>();
    }

    public CustomClass getDialogClass() {
        return (CustomClass) property.getValueClass();
    }
}
