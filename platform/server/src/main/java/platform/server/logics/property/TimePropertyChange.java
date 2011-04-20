package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.data.Time;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimePropertyChange<T extends PropertyInterface> {

    public final Map<ClassPropertyInterface, T> mapInterfaces = new HashMap<ClassPropertyInterface, T>();

    public DataProperty property;

    public TimePropertyChange(boolean isStored, Time time, String sID, String caption, ValueClass[] classes, List<T> propInterfaces) {
        ConcreteValueClass valueClass = time.getConcreteValueClass();
        property = isStored
            ? new StoredDataProperty(sID, caption, classes, valueClass)
            : new SessionDataProperty(sID, caption, classes, valueClass);

        int i=0;
        for(ClassPropertyInterface propertyInterface : property.interfaces)
            mapInterfaces.put(propertyInterface, propInterfaces.get(i++));
    }
}