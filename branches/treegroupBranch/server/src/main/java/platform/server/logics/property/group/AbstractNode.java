package platform.server.logics.property.group;

import platform.base.ImmutableObject;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ValueClass;
import platform.server.logics.property.Property;

import java.util.List;

public abstract class AbstractNode extends ImmutableObject {

    AbstractGroup parent;
    public AbstractGroup getParent() { return parent; }

    public abstract boolean hasChild(Property prop);

    public abstract List<ConcreteCustomClass> getClasses();

    public abstract List<Property> getProperties(ValueClass[] classes);
}
