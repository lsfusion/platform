package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.i18n.LocalizedString;

public abstract class SimpleIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    protected SimpleIncrementProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        return true;
    }
}
