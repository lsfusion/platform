package platform.server.session;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IncrementChangeProps extends IncrementProps {

    public IncrementChangeProps() {
    }

    public <P extends PropertyInterface> IncrementChangeProps(CalcProperty<P> property, PropertyChange<P> table) {
        add(property, table);
    }

    private Map<CalcProperty, PropertyChange<PropertyInterface>> changes = MapFact.mAddRemoveMap(); // mutable поведение

    public ImSet<CalcProperty> getProperties() {
        return SetFact.fromJavaSet(changes.keySet());
    }

    protected boolean isFinal() {
        return true;
    }

    public <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        return (PropertyChange<P>) changes.get(property);
    }

    public <P extends PropertyInterface> void clear() {
        eventChanges(changes.keySet());

        changes.clear();
    }

    public <P extends PropertyInterface> void addNoChanges(Collection<CalcProperty> properties) {
        for(CalcProperty property : properties)
            addNoChange(property);
    }

    public <P extends PropertyInterface> void addNoChange(CalcProperty<P> property) {
        add(property, property.getNoChange());
    }

    public <P extends PropertyInterface> void add(CalcProperty<P> property, PropertyChange<P> change) {
        changes.put(property, (PropertyChange<PropertyInterface>) change);

        eventChange(property);
    }
}
