package platform.server.logics.property.group;

import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.property.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AbstractGroup extends AbstractNode {

    public String caption;
    public boolean createContainer = true;

    public AbstractGroup(String icaption) {
        caption = icaption;
    }

    Collection<AbstractNode> children = new ArrayList<AbstractNode>();
    public void add(AbstractNode prop) {
        children.add(prop);
        prop.parent = this;
    }

    public boolean hasChild(AbstractNode prop) {
        if (this == prop) return true;
        for (AbstractNode child : children) {
            if (child == prop) return true;
            if (child instanceof AbstractGroup && ((AbstractGroup)child).hasChild(prop)) return true;
        }
        return false;
    }

    public List<ConcreteCustomClass> getClasses() {
        List<ConcreteCustomClass> result = new ArrayList<ConcreteCustomClass>();
        fillClasses(result);
        return result;
    }

    private void fillClasses(List<ConcreteCustomClass> classes) {
        for (AbstractNode child : children) {
            if (child instanceof AbstractGroup)
                ((AbstractGroup)child).fillClasses(classes);
            if (child instanceof ConcreteCustomClass)
                classes.add((ConcreteCustomClass)child);
        }
    }

    public List<Property> getProperties() {
        List<Property> result = new ArrayList<Property>();
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
