package platform.server.session;

import platform.base.*;
import platform.server.caches.ManualLazy;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.OverrideModifier;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.util.HashMap;
import java.util.Map;

public abstract class MutableModifier extends MutableObject implements Modifier {

    private WeakIdentityHashSet<OverrideModifier> views = new WeakIdentityHashSet<OverrideModifier>();
    public void registerView(OverrideModifier modifier) { // protected
        views.add(modifier);
        modifier.addChanges(getPropertyChanges().getProperties());
    }

    private QuickSet<CalcProperty> changed = new QuickSet<CalcProperty>();

    protected void addChange(CalcProperty property) {
        changed.add(property);

        for(OverrideModifier view : views)
            view.addChange(property);
    }

    protected void addChanges(Iterable<? extends CalcProperty> properties) {
        for(CalcProperty property : properties)
            addChange(property);
    }

    public abstract PropertyChanges calculatePropertyChanges();

    // по сути protected
    protected abstract <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property);

    protected PropertyChanges propertyChanges = PropertyChanges.EMPTY;
    @ManualLazy
    public PropertyChanges getPropertyChanges() {
        if(changed.size>0) {
            Map<CalcProperty, ModifyChange> replace = new HashMap<CalcProperty, ModifyChange>();
            for(int i=0;i<changed.size;i++) {
                CalcProperty property = changed.get(i);
                replace.put(property, getModifyChange(property));
            }

            propertyChanges = propertyChanges.replace(replace);
            changed = new QuickSet<CalcProperty>();
        }
        return propertyChanges;
    }
}
