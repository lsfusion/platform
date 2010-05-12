package platform.server.view.navigator;

import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.view.form.filter.CompareValue;
import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.navigator.filter.CompareValueNavigator;

import java.util.Map;

public class PropertyObjectNavigator<P extends PropertyInterface> extends ControlObjectNavigator<P,Property<P>,PropertyObjectImplement<P>> implements CompareValueNavigator {

    public PropertyObjectNavigator(LP<P> property, ControlInterfaceNavigator... objects) {
        super(property, objects);
    }

    public PropertyObjectNavigator(Property<P> property, Map<P, ControlInterfaceNavigator> mapping) {
        super(property, mapping);
    }

    public ControlViewNavigator createView(int ID, GroupObjectNavigator groupObject) {
        return new PropertyViewNavigator<P>(ID, this, groupObject);
    }

    public PropertyObjectImplement<P> createImplement(Map<P, PropertyObjectInterface> mapping) {
        return new PropertyObjectImplement<P>(property, mapping);
    }

    public CompareValue doMapping(Mapper mapper) {
        return mapper.mapControl(this);
    }
}
