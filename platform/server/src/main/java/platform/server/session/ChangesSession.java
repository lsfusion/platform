package platform.server.session;

import platform.server.data.classes.ConcreteCustomClass;
import platform.server.data.classes.ConcreteObjectClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.types.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.Property;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ChangesSession {

    public void restart(boolean cancel) throws SQLException;

    public DataObject addObject(ConcreteCustomClass customClass) throws SQLException;

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException;
    public void changeProperty(DataProperty property, Map<DataPropertyInterface, DataObject> keys, Object newValue, boolean externalID) throws SQLException;
    public void changeProperty(DataProperty property, Map<DataPropertyInterface, DataObject> keys, ObjectValue newValue, boolean externalID) throws SQLException;

    public ConcreteObjectClass getCurrentClass(DataObject value) throws SQLException;

    public DataObject getDataObject(Object value, Type type) throws SQLException;
    public ObjectValue getObjectValue(Object value, Type type) throws SQLException;

    // узнает список изменений произошедших без него
    public List<Property> update(PropertyUpdateView toUpdate, Collection<CustomClass> updateClasses) throws SQLException;

    public String apply(BusinessLogics<?> BL) throws SQLException;
}
