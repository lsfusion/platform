package platform.server.view.navigator;

import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.view.form.PropertyObjectInterface;

import java.util.Set;

public class ObjectNavigator extends CellViewNavigator implements PropertyInterfaceNavigator {

    public ObjectNavigator(int ID, ValueClass baseClass, String caption) {
        super(ID);
        this.caption = caption;
        this.baseClass = baseClass;

        if (baseClass instanceof CustomClass && !((CustomClass)baseClass).children.isEmpty()) {
            showTree = true;
            showClass = true;
        }
    }

    public GroupObjectNavigator groupTo;

    public final String caption;
    public boolean show = true;
    public boolean showTree = false;
    public boolean showClass = false;

    public boolean addOnTransaction = false;

    public final ValueClass baseClass;

    public PropertyObjectInterface doMapping(Mapper mapper) {
        return mapper.mapObject(this);
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
        objects.add(this);
    }    
}
