package platform.server.view.navigator;

import platform.server.data.classes.ValueClass;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.navigator.filter.CompareValueNavigator;
import platform.server.view.form.filter.CompareValue;

import java.util.Set;

public class ObjectNavigator extends CellViewNavigator implements CompareValueNavigator {

    public ObjectNavigator(int iID, ValueClass iBaseClass, String iCaption) {
        super(iID);
        caption = iCaption;
        baseClass = iBaseClass;
    }

    public GroupObjectNavigator groupTo;

    public final String caption;

    public final ValueClass baseClass;

    public CompareValue doMapping(FilterNavigator.Mapper mapper) {
        return mapper.mapObject(this);
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
        objects.add(this);
    }    
}
