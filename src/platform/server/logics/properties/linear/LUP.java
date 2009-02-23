package platform.server.logics.properties.linear;

import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.UnionProperty;
import platform.server.logics.properties.JoinPropertyInterface;

public class LUP extends LP<PropertyInterface, UnionProperty> {

    public LUP(UnionProperty iProperty,int objects) {
        super(iProperty);
        for(int i=0;i<objects;i++) {
            JoinPropertyInterface propertyInterface = new JoinPropertyInterface(i);
            listInterfaces.add(propertyInterface);
            property.interfaces.add(propertyInterface);
        }
    }
}
