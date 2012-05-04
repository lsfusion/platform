package platform.server.logics.property;

import platform.server.session.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NoUpdate extends BaseMutableModifier {

    private Set<Property> props = new HashSet<Property>();

    public void add(Property property) {
        props.add(property);

        addChange(property);
    }
    public void addAll(Collection<Property> props) {
        for(Property prop :  props)
            add(prop);
    }
    public void clear() {
        addChanges(props);

        props.clear();
    }

    public NoUpdate() {
    }

    protected boolean isFinal() {
        return true;
    }

    protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property) {
        if(props.contains(property))
            return property.getNoChange();
        else
            return null;
    }

    protected Collection<Property> calculateProperties() {
        return props;
    }
}
