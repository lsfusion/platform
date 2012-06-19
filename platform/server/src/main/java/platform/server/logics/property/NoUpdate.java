package platform.server.logics.property;

import platform.server.session.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NoUpdate extends BaseMutableModifier {

    private Set<CalcProperty> props = new HashSet<CalcProperty>();

    public void add(CalcProperty property) {
        props.add(property);

        addChange(property);
    }
    public void addAll(Collection<CalcProperty> props) {
        for(CalcProperty prop :  props)
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

    protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        if(props.contains(property))
            return property.getNoChange();
        else
            return null;
    }

    protected Collection<CalcProperty> calculateProperties() {
        return props;
    }
}
