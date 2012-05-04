package platform.server.session;

import platform.base.*;
import platform.server.caches.ManualLazy;
import platform.server.logics.property.OverrideModifier;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.HashMap;
import java.util.Map;

public abstract class MutableModifier extends MutableObject implements Modifier {

    private WeakIdentityHashSet<OverrideModifier> views = new WeakIdentityHashSet<OverrideModifier>();
    public void registerView(OverrideModifier modifier) { // protected
        views.add(modifier);
        modifier.addChanges(getPropertyChanges().getProperties());
    }

    private QuickSet<Property> changed = new QuickSet<Property>();

    protected void addChange(Property property) {
        changed.add(property);

        for(OverrideModifier view : views)
            view.addChange(property);
    }

    protected void addChanges(Iterable<? extends Property> properties) {
        for(Property property : properties)
            addChange(property);
    }

    public abstract PropertyChanges calculatePropertyChanges();

    // по сути protected
    protected abstract <P extends PropertyInterface> ModifyChange<P> getModifyChange(Property<P> property);

    protected PropertyChanges propertyChanges = PropertyChanges.EMPTY;
    @ManualLazy
    public PropertyChanges getPropertyChanges() {
        if(changed.size>0) {
            Map<Property, ModifyChange> replace = new HashMap<Property, ModifyChange>();
            for(int i=0;i<changed.size;i++) {
                Property property = changed.get(i);
                replace.put(property, getModifyChange(property));
            }

            propertyChanges = propertyChanges.replace(replace);
            changed = new QuickSet<Property>();
        }
        return propertyChanges;
    }
}
