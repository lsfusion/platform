package platform.server.session;

import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.CustomClass;
import platform.server.data.type.Type;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.view.form.RemoteForm;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public interface ChangesSession {

    public void restart(boolean cancel) throws SQLException;

    public DataObject addObject(ConcreteCustomClass customClass, Modifier<? extends Changes> modifier) throws SQLException;

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException;

    public void changeProperty(DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue) throws SQLException;

    public ConcreteClass getCurrentClass(DataObject value);
    public <T> Map<T,ConcreteClass> getCurrentClasses(Map<T,DataObject> map);

    public DataObject getDataObject(Object value, Type type) throws SQLException;
    public ObjectValue getObjectValue(Object value, Type type) throws SQLException;

    // узнает список изменений произошедших без него
    public Collection<Property> update(RemoteForm<?> toUpdate, Collection<CustomClass> updateClasses) throws SQLException;

    public String apply(BusinessLogics<?> BL) throws SQLException;
}
