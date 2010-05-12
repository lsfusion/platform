package platform.server.logics.linear;

import platform.server.logics.control.ControlInterface;
import platform.server.logics.control.Control;
import platform.server.view.navigator.ControlObjectNavigator;
import platform.server.view.navigator.ObjectNavigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public abstract class LC<T extends ControlInterface, C extends Control<T>> {

    public LC(C property) {
        this.property = property;
        listInterfaces = new ArrayList<T>(property.interfaces);
    }

    public LC(C property, List<T> listInterfaces) {
        this.property = property;
        this.listInterfaces = listInterfaces;
    }

    public C property;
    public List<T> listInterfaces;

    public <IT extends ControlInterface,IF extends Control<IT>> boolean intersect(LC<IT,IF> lp) {
        assert listInterfaces.size()==lp.listInterfaces.size();
        Map<IT,T> map = new HashMap<IT,T>();
        for(int i=0;i<listInterfaces.size();i++)
            map.put(lp.listInterfaces.get(i),listInterfaces.get(i));
        return property.intersect(lp.property,map);
    }

    public abstract ControlObjectNavigator createNavigator(ObjectNavigator... objects);
}
