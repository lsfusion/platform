package platform.server.session;

import platform.base.QuickMap;
import platform.base.SimpleMap;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.Collection;

public abstract class BaseMutableModifier extends MutableModifier {

    protected abstract boolean isFinal();
    protected abstract <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property);
    protected <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property) {
        PropertyChange<P> propertyChange = getPropertyChange(property);
        if(propertyChange!=null)
            return new ModifyChange<P>(propertyChange, isFinal());
        return null;
    }

    protected abstract Collection<CalcProperty> calculateProperties();

    public PropertyChanges calculatePropertyChanges() {
        QuickMap<CalcProperty, PropertyChange> mapChanges = new SimpleMap<CalcProperty, PropertyChange>();
        for(CalcProperty<?> property : calculateProperties())
            mapChanges.add(property, getPropertyChange(property));
        return new PropertyChanges(mapChanges, isFinal());
    }
}
