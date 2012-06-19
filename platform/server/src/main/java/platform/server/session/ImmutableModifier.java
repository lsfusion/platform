package platform.server.session;

import platform.base.ImmutableObject;
import platform.base.QuickMap;
import platform.server.logics.property.CalcProperty;

public class ImmutableModifier extends ImmutableObject implements Modifier {

    private final PropertyChanges changes;
    public ImmutableModifier(PropertyChanges changes) {
        this.changes = changes;
    }

    public ImmutableModifier(QuickMap<? extends CalcProperty, ? extends PropertyChange> changes) {
        this(new PropertyChanges(changes));
    }

    public PropertyChanges getPropertyChanges() {
        return changes;
    }
}
