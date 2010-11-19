package platform.server.logics.property.group;

import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyClassImplement;
import platform.server.logics.property.ValueClassWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// set'ы свойств, нужен в общем то когда входы динамической длины
public abstract class PropertySet extends AbstractNode {
    protected abstract Class<?> getPropertyClass();

    @Override
    public boolean hasChild(Property prop) {
        return getPropertyClass().isInstance(prop);
    }

    @Override
    public List<ConcreteCustomClass> getClasses() {
        return new ArrayList<ConcreteCustomClass>();
    }
                       
    @Override
    public List<PropertyClassImplement> getProperties(Collection<List<ValueClassWrapper>> classLists, boolean anyInInterface) {
        List<PropertyClassImplement> resultList = new ArrayList<PropertyClassImplement>();
        for (List<ValueClassWrapper> classes : classLists) {
            if (isInInterface(classes)) {
                resultList.addAll(getProperties(classes));
            }
        }
        return resultList;
    }

    protected abstract List<PropertyClassImplement> getProperties(List<ValueClassWrapper> classes);

    protected abstract boolean isInInterface(List<ValueClassWrapper> classes);

    protected void setParent(AbstractNode node) {
        node.parent = parent;
    }
}
