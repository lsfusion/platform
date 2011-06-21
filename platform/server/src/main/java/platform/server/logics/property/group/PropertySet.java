package platform.server.logics.property.group;

import platform.server.classes.ValueClass;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyClassImplement;
import platform.server.logics.property.ValueClassWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// set'ы свойств, нужен в общем то когда входы динамической длины
public abstract class PropertySet extends AbstractNode {
    protected abstract Class<?> getPropertyClass();

    @Override
    public boolean hasChild(Property prop) {
        return getPropertyClass().isInstance(prop);
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

    public List<Property> getProperties(ValueClass... classes) {
        List<ValueClassWrapper> classList = new ArrayList<ValueClassWrapper>();
        for (ValueClass cls : classes) {
            classList.add(new ValueClassWrapper(cls));
        }

        List<PropertyClassImplement> clsImplements = getProperties(Collections.singleton(classList), true);
        List<Property> result = new ArrayList<Property>();
        for (PropertyClassImplement clsImplement : clsImplements) {
            result.add(clsImplement.property);
        }

        return result;
    }

    protected abstract List<PropertyClassImplement> getProperties(List<ValueClassWrapper> classes);

    protected abstract boolean isInInterface(List<ValueClassWrapper> classes);

    protected void setParent(AbstractNode node) {
        node.parent = parent;
    }
}
