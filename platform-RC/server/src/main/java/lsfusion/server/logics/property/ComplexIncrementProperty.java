package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.i18n.LocalizedString;

// кроме OrderGroupProperty и FormulaUnionProperty
public abstract class ComplexIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public ComplexIncrementProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        return false;
    }
}
