package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

abstract public class FormulaProperty<T extends PropertyInterface> extends NoIncrementProperty<T> {

    protected FormulaProperty(String caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    @Override
    public boolean check(boolean constraint) {
        return true;
    }
}
