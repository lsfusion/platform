package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImOrderSet;

// кроме OrderGroupProperty и FormulaUnionProperty
public abstract class ComplexIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public ComplexIncrementProperty(String sID, String caption, ImOrderSet<T> interfaces) {
        super(sID, caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        return false;
    }
}
