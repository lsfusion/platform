package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class NoIncrementProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    public NoIncrementProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // не может быть stored / modified
    }
}
