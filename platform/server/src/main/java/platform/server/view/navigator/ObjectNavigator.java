package platform.server.view.navigator;

import platform.server.data.classes.ValueClass;
import platform.server.view.form.filter.CompareValue;
import platform.server.view.form.PropertyObjectInterface;

import java.util.Set;

public class ObjectNavigator extends CellViewNavigator implements PropertyInterfaceNavigator {

    public ObjectNavigator(int iID, ValueClass iBaseClass, String iCaption) {
        super(iID);
        caption = iCaption;
        baseClass = iBaseClass;
    }

    public GroupObjectNavigator groupTo;

    public final String caption;
    public boolean show = true;

    public final ValueClass baseClass;

    public PropertyObjectInterface doMapping(Mapper mapper) {
        return mapper.mapObject(this);
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
        objects.add(this);
    }    
}
