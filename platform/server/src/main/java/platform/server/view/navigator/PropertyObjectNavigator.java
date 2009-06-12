package platform.server.view.navigator;

import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyImplement;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.linear.LP;

public class PropertyObjectNavigator<P extends PropertyInterface> extends PropertyImplement<ObjectNavigator,P> {

    public PropertyObjectNavigator(LP<P, Property<P>> property,ObjectNavigator... objects) {
        super(property.property);
        
        for(int i=0;i<property.listInterfaces.size();i++)
            mapping.put(property.listInterfaces.get(i),objects[i]);
    }
}
