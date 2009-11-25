package platform.server.session;

import platform.server.logics.property.DataProperty;
import platform.server.logics.property.DataPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.ObjectValue;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.navigator.filter.NotFilterNavigator;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.navigator.PropertyInterfaceNavigator;
import platform.server.view.navigator.PropertyObjectNavigator;
import platform.server.classes.CustomClass;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class DataPropertyChange extends PropertyChange<DataPropertyInterface, DataProperty> {

    public DataPropertyChange(DataProperty property, Map<DataPropertyInterface, DataObject> mapping) {
        super(property, mapping);
    }

    public void change(DataSession session, ObjectValue newValue, boolean externalID, TableModifier<? extends TableChanges> modifier) throws SQLException {
        session.changeProperty(property, mapping, newValue, externalID);
    }

    public CustomClass getDialogClass() {
        return (CustomClass) property.value;
    }
    
    public Collection<FilterNavigator> getFilters(ObjectNavigator valueObject, BusinessLogics<?> BL) {
        Map<DataPropertyInterface, PropertyInterfaceNavigator> mapObjects = new HashMap<DataPropertyInterface, PropertyInterfaceNavigator>(mapping);
        mapObjects.put(property.valueInterface,valueObject);

        Collection<FilterNavigator> filters = new ArrayList<FilterNavigator>();
        for(Property constrainedProperty : BL.getChangeConstrainedProperties(property)) // добавляем все констрейнты
            filters.add(new NotFilterNavigator(new NotNullFilterNavigator<DataPropertyInterface>(
                    new PropertyObjectNavigator<DataPropertyInterface>(constrainedProperty,mapObjects))));
        return filters;
    }
}
