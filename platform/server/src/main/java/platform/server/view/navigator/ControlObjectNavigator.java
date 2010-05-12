package platform.server.view.navigator;

import platform.server.logics.linear.LC;
import platform.server.logics.control.ControlImplement;
import platform.server.logics.control.ControlInterface;
import platform.server.logics.control.Control;
import platform.server.view.form.ControlObjectImplement;
import platform.server.view.form.PropertyObjectInterface;

import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Set;

public abstract class ControlObjectNavigator<P extends ControlInterface, C extends Control<P>, O extends ControlObjectImplement<P,C>> extends ControlImplement<ControlInterfaceNavigator, P, C> {

    public ControlObjectNavigator(LC<P,C> property, ControlInterfaceNavigator... objects) {
        super(property.property);

        for(int i=0;i<property.listInterfaces.size();i++)
            mapping.put(property.listInterfaces.get(i),objects[i]);
    }

    protected ControlObjectNavigator(C property, Map<P, ControlInterfaceNavigator> mapping) {
        super(property, mapping);
    }

    public Collection<ObjectNavigator> getObjectImplements() {
        Collection<ObjectNavigator> result = new ArrayList<ObjectNavigator>();
        for(ControlInterfaceNavigator object : mapping.values())
            if(object instanceof ObjectNavigator)
                result.add((ObjectNavigator) object);
        return result;
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
        objects.addAll(getObjectImplements());
    }

    public abstract ControlViewNavigator createView(int ID, GroupObjectNavigator groupObject);
    public abstract O createImplement(Map<P, PropertyObjectInterface> mapping);
}
