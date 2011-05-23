package platform.server.logics.property.group;

import platform.base.ImmutableObject;
import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyClassImplement;
import platform.server.logics.property.ValueClassWrapper;

import java.util.Collection;
import java.util.List;

public abstract class AbstractNode extends ImmutableObject {

    AbstractGroup parent;
    public AbstractGroup getParent() { return parent; }

    public abstract boolean hasChild(Property prop);

    public abstract List<ConcreteCustomClass> getClasses();

    public abstract List<Property> getProperties();

    public abstract Property getProperty(String sid);

    public abstract List<PropertyClassImplement> getProperties(Collection<List<ValueClassWrapper>> classLists, boolean anyInInterface);
}
