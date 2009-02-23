package platform.server.logics.properties.groups;

import platform.server.logics.classes.*;
import platform.server.logics.properties.Property;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public class AbstractGroup extends AbstractNode {

    public String caption;

    public AbstractGroup(String icaption) {
        caption = icaption;
    }

    Collection<AbstractNode> children = new ArrayList<AbstractNode>();
    public void add(AbstractNode prop) {
        children.add(prop);
        prop.parent = this;
    }

    public boolean hasChild(AbstractNode prop) {
        for (AbstractNode child : children) {
            if (child == prop) return true;
            if (child instanceof AbstractGroup && ((AbstractGroup)child).hasChild(prop)) return true;
        }
        return false;
    }

    public List<DataClass> getClasses() {
        List<DataClass> result = new ArrayList();
        fillClasses(result);
        return result;
    }

    private void fillClasses(List<DataClass> classes) {
        for (AbstractNode child : children) {
            if (child instanceof AbstractGroup)
                ((AbstractGroup)child).fillClasses(classes);
            if (child instanceof DataClass)
                classes.add((DataClass)child);
        }
    }

    public List<Property> getProperties() {
        List<Property> result = new ArrayList();
        fillProperties(result);
        return result;
    }

    private void fillProperties(List<Property> properties) {
        for (AbstractNode child : children) {
            if (child instanceof AbstractGroup)
                ((AbstractGroup)child).fillProperties(properties);
            if (child instanceof Property)
                properties.add((Property)child);
        }
    }

}
