package lsfusion.server.logics.property.derived;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class ChangeProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    public ChangeProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // can not be stored / modified;
    }
}
