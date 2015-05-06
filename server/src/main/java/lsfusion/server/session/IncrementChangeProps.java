package lsfusion.server.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;

import java.util.Map;

public class IncrementChangeProps extends IncrementProps {

    public IncrementChangeProps() {
    }

    public <P extends PropertyInterface> IncrementChangeProps(CalcProperty<P> property, PropertyChange<P> table) {
        add(property, table);
    }

    // noUpdate конструктор
    public <P extends PropertyInterface> IncrementChangeProps(ImSet<? extends CalcProperty> noUpdates) {
        for(CalcProperty noUpdate : noUpdates)
            addNoChange(noUpdate);
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

    public <P extends PropertyInterface> void addNoChange(CalcProperty<P> property) {
        add(property, property.getNoChange());
    }

    public <P extends PropertyInterface> void add(CalcProperty<P> property, PropertyChange<P> change) {
        add(property, change, true);
    }

    public <P extends PropertyInterface> void add(CalcProperty<P> property, PropertyChange<P> change, boolean dataChanged) {
        PropertyChange<PropertyInterface> previous = changes.put(property, (PropertyChange<PropertyInterface>) change);

        eventChange(property, dataChanged, previous==null || !BaseUtils.hashEquals(previous, change));
    }

    public int getMaxCount(CalcProperty property) {
        return 0;
    }
}
