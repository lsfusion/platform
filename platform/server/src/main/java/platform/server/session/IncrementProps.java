package platform.server.session;

import platform.base.QuickSet;
import platform.base.WeakIdentityHashSet;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.OverrideSessionModifier;
import platform.server.logics.property.PropertyInterface;

public abstract class IncrementProps {

    private WeakIdentityHashSet<OverrideSessionModifier> modifiers = new WeakIdentityHashSet<OverrideSessionModifier>();
    public void registerView(OverrideSessionModifier modifier) {
        modifiers.add(modifier);
        modifier.eventDataChanges(getProperties());
    }
    public void unregisterView(OverrideSessionModifier modifier) {
        modifiers.remove(modifier);
    }

    private WeakIdentityHashSet<OverrideIncrementProps> increments = new WeakIdentityHashSet<OverrideIncrementProps>();
    public void registerView(OverrideIncrementProps modifier) {
        increments.add(modifier);
    }
    public void unregisterView(OverrideIncrementProps modifier) {
        increments.remove(modifier);
    }

    public void eventChange(CalcProperty property) {
        for(OverrideIncrementProps increment : increments)
            increment.eventChange(property);

         for(OverrideSessionModifier modifier : modifiers)
            modifier.eventIncrementChange(property);
    }
    public void eventChanges(Iterable<? extends CalcProperty> properties) {
        for(CalcProperty property : properties)
            eventChange(property);
    }
    
    public abstract <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property);
    public abstract QuickSet<CalcProperty> getProperties();
}
