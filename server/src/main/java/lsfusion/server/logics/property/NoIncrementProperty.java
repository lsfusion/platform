package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.i18n.LocalizedString;

public abstract class NoIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public NoIncrementProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // не может быть stored / modified
    }
}
