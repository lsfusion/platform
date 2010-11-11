package platform.server.logics.property.group;

import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// set'ы свойств, нужен в общем то когда входы динамической длины
public abstract class PropertySet extends AbstractNode {

    protected abstract Class<?> getPropertyClass();

    public boolean hasChild(Property prop) {
        return prop.getClass() == getPropertyClass();
    }

    public List<ConcreteCustomClass> getClasses() {
        return new ArrayList<ConcreteCustomClass>();
    }

    public List<Property> getProperties(ValueClass[] classes) {
        if(classes!=null && isInInterface(classes))
            return Collections.singletonList(getProperty(classes));
        else
            return new ArrayList<Property>();
    }

    protected boolean isInInterface(ValueClass[] classes) {
        return true;
    }

    protected abstract Property getProperty(ValueClass[] classes);
}
