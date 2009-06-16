package platform.server.logics.properties.linear;

import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LP<T extends PropertyInterface,P extends Property<T>> {

    public LP(P iProperty) {
        property =iProperty;
        listInterfaces = new ArrayList<T>(property.interfaces);
    }

    public LP(P iProperty, List<T> iListInterfaces) {
        property =iProperty;
        listInterfaces = iListInterfaces;
    }

    public P property;
    public List<T> listInterfaces;

    public <IT extends PropertyInterface, IP extends Property<IT>> boolean intersect(LP<IT,IP> lp) {
        assert listInterfaces.size()==lp.listInterfaces.size();
        Map<IT,T> map = new HashMap<IT,T>();
        for(int i=0;i<listInterfaces.size();i++)
            map.put(lp.listInterfaces.get(i),listInterfaces.get(i));
        return property.intersect(lp.property,map);
    }
}
