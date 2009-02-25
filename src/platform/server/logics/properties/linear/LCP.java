package platform.server.logics.properties.linear;

import platform.server.logics.classes.RemoteClass;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.ClassProperty;

public class LCP extends LP<DataPropertyInterface, ClassProperty> {

    public LCP(ClassProperty iProperty) {super(iProperty);}

    public void addInterface(RemoteClass inClass) {
        DataPropertyInterface propertyInterface = new DataPropertyInterface(listInterfaces.size(),inClass);
        listInterfaces.add(propertyInterface);
        property.interfaces.add(propertyInterface);
    }
}
