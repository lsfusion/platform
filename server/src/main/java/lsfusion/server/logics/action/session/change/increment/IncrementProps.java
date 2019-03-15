package lsfusion.server.logics.action.session.change.increment;

import lsfusion.base.col.heavy.weak.WeakIdentityHashSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.OverrideSessionModifier;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public abstract class IncrementProps {

    // пока не concurrent, но в будущем возможно понадобится
    private WeakIdentityHashSet<OverrideSessionModifier> modifiers = new WeakIdentityHashSet<>();
    public void registerView(OverrideSessionModifier modifier) throws SQLException, SQLHandledException {
        modifiers.add(modifier);
        modifier.eventDataChanges(getProperties());
    }
    public void unregisterView(OverrideSessionModifier modifier) {
        modifiers.remove(modifier);
    }

    private WeakIdentityHashSet<OverrideIncrementProps> increments = new WeakIdentityHashSet<>();
    public void registerView(OverrideIncrementProps modifier) {
        increments.add(modifier);
    }
    public void unregisterView(OverrideIncrementProps modifier) {
        increments.remove(modifier);
    }

    public void eventChange(Property property, boolean sourceChanged) throws SQLException, SQLHandledException {
        eventChange(property, true, sourceChanged);
    }

    public void eventChange(Property property, boolean dataChanged, boolean sourceChanged) throws SQLException, SQLHandledException {
        for(OverrideIncrementProps increment : increments)
            increment.eventChange(property, dataChanged, sourceChanged);

         for(OverrideSessionModifier modifier : modifiers)
            modifier.eventIncrementChange(property, dataChanged, sourceChanged);
    }
    public void eventChanges(Iterable<? extends Property> properties) throws SQLException, SQLHandledException {
        for(Property property : properties)
            eventChange(property, true); // вызывается при clear, а значит все "источники" сбрасываются
    }
    
    public abstract <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property);
    public abstract ImSet<Property> getProperties();

    public abstract long getMaxCount(Property property);
    
    public abstract String out();
}
