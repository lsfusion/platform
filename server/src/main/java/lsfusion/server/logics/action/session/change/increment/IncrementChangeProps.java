package lsfusion.server.logics.action.session.change.increment;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.Map;

public class IncrementChangeProps extends IncrementProps {

    public IncrementChangeProps() {
    }

    // noUpdate конструктор
    public <P extends PropertyInterface> IncrementChangeProps(ImSet<? extends CalcProperty> noUpdates) throws SQLException, SQLHandledException {
        for(CalcProperty noUpdate : noUpdates)
            addNoChange(noUpdate);
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
    public boolean contains(CalcProperty property) {
        return changes.containsKey(property);
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

    public <P extends PropertyInterface> void clear() throws SQLException, SQLHandledException {
        eventChanges(changes.keySet());

        changes.clear();
    }

    public <P extends PropertyInterface> void addNoChange(CalcProperty<P> property) throws SQLException, SQLHandledException {
        add(property, property.getNoChange());
    }

    public <P extends PropertyInterface> void add(CalcProperty<P> property, PropertyChange<P> change) throws SQLException, SQLHandledException {
        add(property, change, true);
    }

    public <P extends PropertyInterface> void add(CalcProperty<P> property, PropertyChange<P> change, boolean dataChanged) throws SQLException, SQLHandledException {
        PropertyChange<PropertyInterface> previous = changes.put(property, (PropertyChange<PropertyInterface>) change);

        eventChange(property, dataChanged, previous==null || !BaseUtils.hashEquals(previous, change));
    }

    public long getMaxCount(CalcProperty property) {
        return 0;
    }

    @Override
    public String out() {
        return "\nchange : " + changes;
    }
}
