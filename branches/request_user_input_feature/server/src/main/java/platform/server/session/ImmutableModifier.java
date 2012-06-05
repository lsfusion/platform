package platform.server.session;

import platform.base.ImmutableObject;

public class ImmutableModifier extends ImmutableObject implements Modifier {

    private final PropertyChanges changes;
    public ImmutableModifier(PropertyChanges changes) {
        this.changes = changes;
    }

    public PropertyChanges getPropertyChanges() {
        return changes;
    }
}
