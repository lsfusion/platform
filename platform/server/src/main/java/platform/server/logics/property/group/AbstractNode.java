package platform.server.logics.property.group;

import platform.base.ImmutableObject;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyClassImplement;
import platform.server.logics.property.ValueClassWrapper;

import java.util.List;

public abstract class AbstractNode extends ImmutableObject {

    AbstractGroup parent;
    public AbstractGroup getParent() { return parent; }

    public abstract boolean hasChild(Property prop);

    public abstract ImOrderSet<Property> getProperties();

    public abstract Property getProperty(String sid);

    public abstract ImList<PropertyClassImplement> getProperties(ImCol<ImSet<ValueClassWrapper>> classLists, boolean anyInInterface);

    public abstract List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList);
}
